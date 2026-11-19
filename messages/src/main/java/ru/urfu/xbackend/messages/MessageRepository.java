package ru.urfu.xbackend.messages;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
class MessageRepository {
    private static final String FIELDS = "id, author_id, text, created_at, updated_at";
    private static final RowMapper<Message> MAPPER = (row, index) -> new Message(
            row.getLong("id"), row.getLong("author_id"), row.getString("text"),
            row.getObject("created_at", OffsetDateTime.class).toInstant(),
            row.getObject("updated_at", OffsetDateTime.class).toInstant());

    private final NamedParameterJdbcTemplate jdbc;

    MessageRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    Message create(long authorId, String text) {
        return jdbc.queryForObject("""
                INSERT INTO messages (author_id, text) VALUES (:authorId, :text)
                RETURNING
                """ + FIELDS, Map.of("authorId", authorId, "text", text), MAPPER);
    }

    List<Message> list(Long authorId, int limit, int offset) {
        String filter = authorId == null ? "" : " WHERE author_id = :authorId";
        var values = new MapSqlParameterSource("limit", limit)
                .addValue("offset", offset).addValue("authorId", authorId);
        return jdbc.query("SELECT " + FIELDS + " FROM messages" + filter
                + " ORDER BY created_at DESC, id DESC LIMIT :limit OFFSET :offset", values, MAPPER);
    }

    Optional<Message> find(long id) {
        return jdbc.query("SELECT " + FIELDS + " FROM messages WHERE id = :id",
                Map.of("id", id), MAPPER).stream().findFirst();
    }

    Optional<Message> update(long id, String text) {
        return jdbc.query("""
                UPDATE messages SET text = :text, updated_at = CURRENT_TIMESTAMP
                WHERE id = :id RETURNING
                """ + FIELDS, Map.of("id", id, "text", text), MAPPER).stream().findFirst();
    }

    boolean delete(long id) {
        return jdbc.update("DELETE FROM messages WHERE id = :id", Map.of("id", id)) > 0;
    }
}
