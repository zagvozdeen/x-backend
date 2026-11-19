package ru.urfu.xbackend.api;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiErrorController implements ErrorController {
    @RequestMapping("${server.error.path:/error}")
    ResponseEntity<ApiResponse<Void>> error(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatusCode code = status instanceof Integer value && value >= 400 && value < 600
                ? HttpStatusCode.valueOf(value) : HttpStatus.NOT_FOUND;
        return ApiErrors.response(code, null);
    }
}
