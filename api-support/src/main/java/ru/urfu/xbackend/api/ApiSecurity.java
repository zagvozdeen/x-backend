package ru.urfu.xbackend.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Configuration(proxyBeanMethods = false)
public class ApiSecurity {
    @Bean
    CorsConfigurationSource cors(@Value("${CORS_ALLOWED_ORIGINS:http://localhost:5173,http://127.0.0.1:5173}")
                                 String origins) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origins.split(","))
                .map(String::strip).filter(origin -> !origin.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/**", config);
        return source;
    }

    @Bean
    WebSecurityCustomizer requestRejection(ObjectMapper json) {
        return web -> web.requestRejectedHandler((request, response, ex) ->
                ApiErrors.write(new ServletServerHttpResponse(response), json, HttpStatus.BAD_REQUEST,
                        null, null));
    }

    public static HttpSecurity configure(HttpSecurity http, CorsConfigurationSource cors, ObjectMapper json)
            throws Exception {
        var filter = new CorsFilter(cors);
        filter.setCorsProcessor(new DefaultCorsProcessor() {
            @Override
            protected void rejectRequest(ServerHttpResponse response) throws IOException {
                ApiErrors.write(response, json, HttpStatus.FORBIDDEN,
                        "Request rejected by the CORS policy", "CORS_REJECTED");
            }
        });
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .addFilter(filter)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rules -> rules.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(bearerEntryPoint(json))
                        .accessDeniedHandler((request, response, ex) -> ApiErrors.write(
                                new ServletServerHttpResponse(response), json, HttpStatus.FORBIDDEN, null, null)));
    }

    public static AuthenticationEntryPoint bearerEntryPoint(ObjectMapper json) {
        var bearer = new BearerTokenAuthenticationEntryPoint();
        return (request, response, ex) -> {
            var output = new ServletServerHttpResponse(response);
            if (ex instanceof AuthenticationServiceException) {
                ApiErrors.write(output, json, HttpStatus.SERVICE_UNAVAILABLE, "The Users service is unavailable", null);
                return;
            }
            bearer.commence(request, response, ex);
            ApiErrors.write(output, json, HttpStatusCode.valueOf(response.getStatus()), null, null);
        };
    }
}
