package ru.urfu.xbackend.messages;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.urfu.xbackend.api.ApiException;

import java.util.List;

@Service
class MessageService {
    private final MessageRepository messages;

    MessageService(MessageRepository messages) {
        this.messages = messages;
    }

    Message create(long authorId, String text) {
        return messages.create(authorId, text.strip());
    }

    List<Message> list(Long authorId, int limit, int offset) {
        return messages.list(authorId, limit, offset);
    }

    Message get(long id) {
        return messages.find(id).orElseThrow(MessageService::notFound);
    }

    Message update(long id, long actorId, boolean admin, String text) {
        checkAccess(id, actorId, admin);
        return messages.update(id, text.strip()).orElseThrow(MessageService::notFound);
    }

    void delete(long id, long actorId, boolean admin) {
        checkAccess(id, actorId, admin);
        if (!messages.delete(id)) {
            throw notFound();
        }
    }

    private void checkAccess(long id, long actorId, boolean admin) {
        Message message = get(id);
        if (message.authorId() != actorId && !admin) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Insufficient permissions");
        }
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "Message not found");
    }
}
