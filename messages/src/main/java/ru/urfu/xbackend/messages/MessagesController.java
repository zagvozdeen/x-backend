package ru.urfu.xbackend.messages;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.urfu.xbackend.api.ApiResponse;
import ru.urfu.xbackend.api.Items;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/messages")
class MessagesController {
    private final MessageService messages;

    MessagesController(MessageService messages) {
        this.messages = messages;
    }

    @PostMapping
    ResponseEntity<ApiResponse<Message>> create(Authentication actor, @Valid @RequestBody MessageRequest request) {
        Message message = messages.create(userId(actor), request.text());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(201, "Сообщение создано", message));
    }

    @GetMapping
    ApiResponse<Items<Message>> list(@RequestParam(name = "author_id", required = false) @Positive Long authorId,
                                     @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
                                     @RequestParam(defaultValue = "0") @Min(0) int offset) {
        return new ApiResponse<>(200, "Сообщения получены", new Items<>(messages.list(authorId, limit, offset)));
    }

    @GetMapping("/{id}")
    ApiResponse<Message> get(@PathVariable @Positive long id) {
        return new ApiResponse<>(200, "Сообщение получено", messages.get(id));
    }

    @PatchMapping("/{id}")
    ApiResponse<Message> update(@PathVariable @Positive long id, Authentication actor,
                                @Valid @RequestBody MessageRequest request) {
        return new ApiResponse<>(200, "Сообщение изменено",
                messages.update(id, userId(actor), isAdmin(actor), request.text()));
    }

    @DeleteMapping("/{id}")
    ApiResponse<Map<String, Object>> delete(@PathVariable @Positive long id, Authentication actor) {
        messages.delete(id, userId(actor), isAdmin(actor));
        return new ApiResponse<>(200, "Сообщение удалено", Map.of());
    }

    private static long userId(Authentication actor) {
        return Long.parseLong(actor.getName());
    }

    private static boolean isAdmin(Authentication actor) {
        return actor.getAuthorities().stream().anyMatch(role -> "ROLE_admin".equals(role.getAuthority()));
    }
}
