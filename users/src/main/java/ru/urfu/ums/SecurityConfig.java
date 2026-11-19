package ru.urfu.ums;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import ru.urfu.xbackend.api.ApiSecurity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
class SecurityConfig {
    static final String ISSUER = "zagvozdeen-ums";

    @Bean
    SecretKey jwtKey(@Value("${ums.jwt-secret}") String secret) {
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        if (key.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must be at least 32 bytes");
        }
        return new SecretKeySpec(key, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtKey) {
        var decoder = NimbusJwtDecoder.withSecretKey(jwtKey).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        return decoder;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    ApplicationRunner adminInitializer(UserService users, @Value("${ums.admin-email}") String email,
                                       @Value("${ums.admin-password}") String password) {
        return args -> {
            if (email.isBlank() && password.isBlank()) {
                return;
            }
            if (email.isBlank() || password.isBlank() || password.length() < 8) {
                throw new IllegalArgumentException("Set ADMIN_EMAIL and an ADMIN_PASSWORD of at least 8 characters");
            }
            users.initializeAdmin(email, password);
        };
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, UserRepository users, ObjectMapper json,
                                 CorsConfigurationSource cors) throws Exception {
        return ApiSecurity.configure(http, cors, json)
                .authorizeHttpRequests(rules -> rules
                        .requestMatchers(HttpMethod.POST, "/api/v1/users", "/api/v1/auth/login",
                                "/api/v1/auth/confirm-email").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/*/roles/*").hasRole("admin")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/*/roles/*").hasRole("admin")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.authenticationEntryPoint(ApiSecurity.bearerEntryPoint(json))
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(token -> {
                            long id;
                            try {
                                id = Long.parseLong(token.getSubject());
                            } catch (NumberFormatException ex) {
                                throw new InvalidBearerTokenException("Invalid user ID");
                            }
                            Object claim = token.getClaim("ver");
                            if (!(claim instanceof Number version)) {
                                throw new InvalidBearerTokenException("The token is outdated");
                            }
                            Actor actor = users.findActor(id, version.intValue())
                                    .orElseThrow(() -> new InvalidBearerTokenException("The token is no longer valid"));
                            var authorities = actor.roles().stream()
                                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
                            return UsernamePasswordAuthenticationToken.authenticated(actor, null, authorities);
                        })))
                .build();
    }
}
