package ru.urfu.xbackend.messages;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;
import ru.urfu.xbackend.api.ApiSecurity;

@Configuration(proxyBeanMethods = false)
class MessagesSecurityConfig {
    @Bean
    SecurityFilterChain security(HttpSecurity http, ObjectMapper json, CorsConfigurationSource cors,
                                 UsersTokenIntrospector users) throws Exception {
        var entryPoint = ApiSecurity.bearerEntryPoint(json);
        return ApiSecurity.configure(http, cors, json)
                .authorizeHttpRequests(rules -> rules
                        .requestMatchers(HttpMethod.GET, "/api/v1/messages", "/api/v1/messages/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/messages").hasRole("producer")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .authenticationEntryPoint(entryPoint)
                        .withObjectPostProcessor(new ObjectPostProcessor<BearerTokenAuthenticationFilter>() {
                            @Override
                            public <T extends BearerTokenAuthenticationFilter> T postProcess(T filter) {
                                filter.setAuthenticationFailureHandler(entryPoint::commence);
                                return filter;
                            }
                        })
                        .opaqueToken(token -> token.introspector(users)))
                .build();
    }
}
