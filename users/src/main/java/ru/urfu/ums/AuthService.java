package ru.urfu.ums;

import ru.urfu.xbackend.api.ApiException;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
class AuthService {
    private static final String TRAINING_EMAIL_CODE = "123456";
    private final UserRepository repository;
    private final UserService users;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final String dummyHash;

    AuthService(UserRepository repository, UserService users, PasswordEncoder passwords, JwtEncoder encoder) {
        this.repository = repository;
        this.users = users;
        this.passwords = passwords;
        this.encoder = encoder;
        this.dummyHash = passwords.encode(UUID.randomUUID().toString());
    }

    LoginResponse login(LoginRequest request) {
        var credentials = repository.findCredentials(UserService.normalizeEmail(request.email()));
        String password = UserService.requireBcryptLength(request.password());
        boolean matches = passwords.matches(password,
                credentials.map(UserRepository.Credentials::passwordHash).orElse(dummyHash));
        if (!matches || credentials.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Неверный email или пароль");
        }
        var account = credentials.get();
        if (account.confirmationId() != null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Подтвердите email",
                    "EMAIL_NOT_CONFIRMED", new EmailConfirmation(account.confirmationId()));
        }
        Profile profile = users.findProfile(account.id());
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(SecurityConfig.ISSUER).issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS)).subject(Long.toString(profile.id()))
                .claim("roles", profile.roles()).claim("ver", account.authVersion()).build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return new LoginResponse(encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(), profile);
    }

    void confirmEmail(ConfirmEmailRequest request) {
        if (!TRAINING_EMAIL_CODE.equals(request.code()) || !repository.confirmEmail(request.confirmationId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Неверный код или ID подтверждения",
                    "INVALID_EMAIL_CONFIRMATION", null);
        }
    }

    void changePassword(Actor actor, ChangePasswordRequest request) {
        String current = UserService.requireBcryptLength(request.currentPassword());
        String next = UserService.requireBcryptLength(request.newPassword());
        String hash = repository.findPassword(actor.id())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Войдите снова"));
        if (!passwords.matches(current, hash)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Неверный текущий пароль",
                    "INVALID_CURRENT_PASSWORD", null);
        }
        if (!repository.updatePassword(actor.id(), hash, passwords.encode(next))) {
            throw new ApiException(HttpStatus.CONFLICT, "Данные аккаунта изменились. Войдите снова");
        }
    }
}
