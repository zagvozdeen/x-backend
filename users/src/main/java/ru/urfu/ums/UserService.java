package ru.urfu.ums;

import ru.urfu.xbackend.api.ApiException;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
class UserService {
    private final UserRepository users;
    private final PasswordEncoder passwords;

    UserService(UserRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        UUID confirmationId = UUID.randomUUID();
        long id = users.insert(request.firstName().strip(), request.lastName().strip(),
                normalizeEmail(request.email()), request.phoneNumber(),
                passwords.encode(requireBcryptLength(request.password())), confirmationId).orElseThrow();
        grantInitialRole(id, "user");
        return new RegistrationResponse(findProfile(id), confirmationId);
    }

    @Transactional
    public void initializeAdmin(String email, String password) {
        users.insert("Администратор", "UMS", normalizeEmail(email), null,
                        passwords.encode(requireBcryptLength(password)), null)
                .ifPresent(id -> {
                    grantInitialRole(id, "user");
                    grantInitialRole(id, "admin");
                });
    }

    public List<UserSummary> list(String role, List<Long> ids, int limit, int offset) {
        if (ids != null && (ids.isEmpty() || ids.stream().anyMatch(id -> id == null || id <= 0))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ids must contain positive IDs");
        }
        if (role != null && users.findRoles().stream().noneMatch(r -> r.name().equals(role))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown role");
        }
        return users.findAll(role, ids, limit, offset);
    }

    public Profile profile(Actor actor, long id) {
        requireSelfOrAdmin(actor, id);
        return findProfile(id);
    }

    @Transactional
    public Profile update(Actor actor, long id, ProfilePatch patch) {
        requireSelfOrAdmin(actor, id);
        if (patch.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Specify at least one field to update");
        }
        if (!users.update(id, patch, clean(patch.firstName()), clean(patch.lastName()),
                patch.email() == null ? null : normalizeEmail(patch.email()))) {
            throw userNotFound();
        }
        return findProfile(id);
    }

    @Transactional
    public void delete(Actor actor, long id) {
        requireSelfOrAdmin(actor, id);
        if (!users.delete(id)) {
            throw userNotFound();
        }
    }

    public List<Role> roles() {
        return users.findRoles();
    }

    @Transactional
    public Profile changeRole(long id, int roleId, boolean assign) {
        if (!users.lockUser(id)) {
            throw userNotFound();
        }
        if (users.findRoles().stream().noneMatch(role -> role.id() == roleId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Role not found");
        }
        if (assign) {
            users.assignRole(id, roleId);
        } else {
            users.removeRole(id, roleId);
        }
        return findProfile(id);
    }

    Profile findProfile(long id) {
        return users.findById(id).orElseThrow(UserService::userNotFound);
    }

    private void grantInitialRole(long id, String name) {
        int roleId = users.findRoles().stream().filter(r -> r.name().equals(name))
                .findFirst().orElseThrow().id();
        users.assignRole(id, roleId);
    }

    private static void requireSelfOrAdmin(Actor actor, long id) {
        if (actor.id() != id && !actor.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Insufficient permissions");
        }
    }

    private static ApiException userNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "User not found");
    }

    static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private static String clean(String value) {
        return value == null ? null : value.strip();
    }

    static String requireBcryptLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must be at most 72 bytes in UTF-8");
        }
        return password;
    }
}
