package ru.urfu.xbackend.messages;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
class UsersTokenIntrospector implements OpaqueTokenIntrospector {
    private final RestClient users;

    UsersTokenIntrospector(RestClient.Builder client, @Value("${messages.users-url}") String usersUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(2));
        this.users = client.baseUrl(usersUrl).requestFactory(factory).build();
    }

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        IdentityResponse response;
        try {
            response = users.get().uri("/api/v1/auth/me")
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve().body(IdentityResponse.class);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
                throw new BadOpaqueTokenException("Токен больше не действует");
            }
            throw new OAuth2IntrospectionException("Сервис Users не подтвердил запрос", ex);
        } catch (RestClientException ex) {
            throw new OAuth2IntrospectionException("Сервис Users недоступен", ex);
        }

        Identity identity = response == null ? null : response.data();
        if (identity == null || identity.id() <= 0 || identity.roles() == null) {
            throw new OAuth2IntrospectionException("Некорректный ответ сервиса Users");
        }
        var authorities = identity.roles().stream()
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
        return new DefaultOAuth2AuthenticatedPrincipal(Long.toString(identity.id()),
                Map.of("user_id", identity.id()), authorities);
    }

    private record IdentityResponse(Identity data) {}
    private record Identity(long id, List<String> roles) {}
}
