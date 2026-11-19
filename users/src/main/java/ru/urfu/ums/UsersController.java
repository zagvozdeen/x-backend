package ru.urfu.ums;

import ru.urfu.xbackend.api.ApiResponse;
import ru.urfu.xbackend.api.Items;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
class UsersController {
    private final UserService users;
    private final AuthService auth;

    UsersController(UserService users, AuthService auth) {
        this.users = users;
        this.auth = auth;
    }

    @PostMapping("/users")
    ResponseEntity<ApiResponse<RegistrationResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(201, "Пользователь создан", users.register(request)));
    }

    @PostMapping("/auth/login")
    ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return new ApiResponse<>(200, "Вход выполнен", auth.login(request));
    }

    @GetMapping("/auth/me")
    ApiResponse<Actor> currentUser(@AuthenticationPrincipal Actor actor) {
        return new ApiResponse<>(200, "Пользователь определён", actor);
    }

    @PostMapping("/auth/confirm-email")
    ApiResponse<Map<String, Boolean>> confirmEmail(@Valid @RequestBody ConfirmEmailRequest request) {
        auth.confirmEmail(request);
        return new ApiResponse<>(200, "Email подтверждён", Map.of("email_confirmed", true));
    }

    @PutMapping("/users/me/password")
    ApiResponse<Map<String, Object>> changePassword(@AuthenticationPrincipal Actor actor,
                                                   @Valid @RequestBody ChangePasswordRequest request) {
        auth.changePassword(actor, request);
        return new ApiResponse<>(200, "Пароль изменён", Map.of());
    }

    @GetMapping("/users")
    ApiResponse<Items<UserSummary>> list(@RequestParam(required = false) String role,
                                         @RequestParam(required = false) @Size(max = 100) List<Long> ids,
                                         @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
                                         @RequestParam(defaultValue = "0") @Min(0) int offset) {
        return new ApiResponse<>(200, "Пользователи получены", new Items<>(users.list(role, ids, limit, offset)));
    }

    @GetMapping("/users/{id}")
    ApiResponse<Profile> profile(@AuthenticationPrincipal Actor actor, @PathVariable @Positive long id) {
        return new ApiResponse<>(200, "Профиль получен", users.profile(actor, id));
    }

    @PatchMapping("/users/{id}")
    ApiResponse<Profile> patch(@AuthenticationPrincipal Actor actor, @PathVariable @Positive long id,
                               @Valid @RequestBody ProfilePatch request) {
        return new ApiResponse<>(200, "Пользователь изменён", users.update(actor, id, request));
    }

    @DeleteMapping("/users/{id}")
    ApiResponse<Map<String, Object>> delete(@AuthenticationPrincipal Actor actor, @PathVariable @Positive long id) {
        users.delete(actor, id);
        return new ApiResponse<>(200, "Пользователь удалён", Map.of());
    }

    @GetMapping("/roles")
    ApiResponse<Items<Role>> roles() {
        return new ApiResponse<>(200, "Роли получены", new Items<>(users.roles()));
    }

    @GetMapping("/users/{id}/roles")
    ApiResponse<Items<String>> userRoles(@AuthenticationPrincipal Actor actor, @PathVariable @Positive long id) {
        return new ApiResponse<>(200, "Роли пользователя получены", new Items<>(users.profile(actor, id).roles()));
    }

    @PutMapping("/users/{id}/roles/{roleId}")
    ApiResponse<Profile> assign(@PathVariable @Positive long id, @PathVariable @Positive int roleId) {
        return new ApiResponse<>(200, "Роль назначена", users.changeRole(id, roleId, true));
    }

    @DeleteMapping("/users/{id}/roles/{roleId}")
    ApiResponse<Profile> remove(@PathVariable @Positive long id, @PathVariable @Positive int roleId) {
        return new ApiResponse<>(200, "Роль снята", users.changeRole(id, roleId, false));
    }
}
