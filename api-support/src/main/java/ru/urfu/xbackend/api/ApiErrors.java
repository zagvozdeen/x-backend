package ru.urfu.xbackend.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.IOException;

@RestControllerAdvice
public class ApiErrors extends ResponseEntityExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ApiErrors.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiResponse<Object>> domain(ApiException ex) {
        return ResponseEntity.status(ex.status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body(ex.status, ex.getMessage(), ex.error, ex.data));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity<ApiResponse<Void>> duplicate() {
        return response(HttpStatus.CONFLICT, "A record with these details already exists");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> unexpected(Exception ex) {
        logger.error("Failed to process request", ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = null;
        if (ex instanceof MethodArgumentNotValidException validation) {
            message = validation.getBindingResult().getFieldErrors().stream()
                    .map(e -> e.getField() + ": " + e.getDefaultMessage())
                    .findFirst().orElse(null);
        }
        var jsonHeaders = new HttpHeaders();
        jsonHeaders.putAll(headers);
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        return super.handleExceptionInternal(
                ex, body(status, message, null, null), jsonHeaders, status, request);
    }

    static ResponseEntity<ApiResponse<Void>> response(HttpStatusCode status, String message) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
                .body(body(status, message, null, null));
    }

    static void write(ServerHttpResponse response, ObjectMapper json, HttpStatusCode status,
                      String message, String error) throws IOException {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        json.writeValue(response.getBody(), body(status, message, error, null));
        response.flush();
    }

    private static <T> ApiResponse<T> body(HttpStatusCode status, String message, String error, T data) {
        HttpStatus knownStatus = HttpStatus.resolve(status.value());
        String defaultCode = knownStatus == null ? "HTTP_" + status.value() : knownStatus.name();
        return new ApiResponse<>(status.value(), message == null ? defaultMessage(status) : message,
                data, error == null ? defaultCode : error);
    }

    private static String defaultMessage(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "Invalid request parameters";
            case 401 -> "A valid bearer token is required";
            case 403 -> "Insufficient permissions";
            case 404 -> "Route not found";
            case 405 -> "HTTP method not allowed";
            case 406 -> "Requested response format is not supported";
            case 415 -> "Unsupported Content-Type";
            default -> status.is5xxServerError() ? "Internal server error" : "Unable to process request";
        };
    }
}

