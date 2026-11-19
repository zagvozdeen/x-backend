package ru.urfu.ums;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
class UserRepository {
    private static final String ROLE_NAMES = """
            ARRAY(SELECT r.name FROM user_role ur JOIN roles r ON r.id = ur.role_id
                  WHERE ur.user_id = u.id ORDER BY r.id) AS roles
            """;
    private static final String PROFILE = """
            SELECT u.id, u.first_name, u.last_name, u.email, u.phone_number,
                   u.created_at, u.updated_at, u.email_confirmed_at IS NOT NULL AS email_confirmed,
            """ + ROLE_NAMES + " FROM users u WHERE u.id = :id";

    private final NamedParameterJdbcTemplate jdbc;

    UserRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    Optional<Profile> findById(long id) {
        return jdbc.query(PROFILE, Map.of("id", id), (rs, row) -> new Profile(
                rs.getLong("id"), rs.getString("first_name"), rs.getString("last_name"),
                rs.getString("email"), rs.getString("phone_number"), roles(rs),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                rs.getObject("updated_at", OffsetDateTime.class).toInstant(),
                rs.getBoolean("email_confirmed"))).stream().findFirst();
    }

    Optional<Credentials> findCredentials(String email) {
        return jdbc.query("""
                SELECT id, password, auth_version, email_confirmation_id FROM users WHERE email = :email
                """, Map.of("email", email), (rs, row) -> new Credentials(rs.getLong("id"),
                rs.getString("password"), rs.getInt("auth_version"),
                rs.getObject("email_confirmation_id", UUID.class))).stream().findFirst();
    }

    Optional<Actor> findActor(long id, int version) {
        return jdbc.query("SELECT u.id, " + ROLE_NAMES + """
                FROM users u
                WHERE u.id = :id AND u.auth_version = :version AND u.email_confirmed_at IS NOT NULL
                """, Map.of("id", id, "version", version),
                (rs, row) -> new Actor(id, roles(rs))).stream().findFirst();
    }

    List<UserSummary> findAll(String role, List<Long> ids, int limit, int offset) {
        String sql = "SELECT u.id, u.first_name, u.last_name, " + ROLE_NAMES + " FROM users u WHERE TRUE ";
        var params = new MapSqlParameterSource().addValue("limit", limit).addValue("offset", offset);
        if (role != null) {
            sql += """
                    AND EXISTS (SELECT 1 FROM user_role ur JOIN roles r ON r.id = ur.role_id
                                WHERE ur.user_id = u.id AND r.name = :role)
                    """;
            params.addValue("role", role);
        }
        if (ids != null) {
            sql += " AND u.id IN (:ids)";
            params.addValue("ids", ids);
        }
        return jdbc.query(sql + " ORDER BY u.id LIMIT :limit OFFSET :offset", params,
                (rs, row) -> new UserSummary(rs.getLong("id"), rs.getString("first_name"),
                        rs.getString("last_name"), roles(rs)));
    }

    Optional<Long> insert(String firstName, String lastName, String email, String phone, String hash,
                          UUID confirmationId) {
        boolean confirmed = confirmationId == null;
        var params = new MapSqlParameterSource()
                .addValue("firstName", firstName).addValue("lastName", lastName).addValue("email", email)
                .addValue("phone", phone, Types.VARCHAR).addValue("hash", hash)
                .addValue("confirmed", confirmed).addValue("confirmationId", confirmationId, Types.OTHER);
        String sql = """
                INSERT INTO users (first_name, last_name, email, phone_number, password,
                                   email_confirmed_at, email_confirmation_id)
                VALUES (:firstName, :lastName, :email, :phone, :hash,
                        CASE WHEN :confirmed THEN CURRENT_TIMESTAMP END, :confirmationId)
                """;
        if (confirmed) {
            sql += " ON CONFLICT (email) DO NOTHING";
        }
        return jdbc.query(sql + " RETURNING id", params, (rs, row) -> rs.getLong("id")).stream().findFirst();
    }

    boolean update(long id, ProfilePatch patch, String firstName, String lastName, String email) {
        var params = new MapSqlParameterSource("id", id)
                .addValue("firstName", firstName, Types.VARCHAR).addValue("lastName", lastName, Types.VARCHAR)
                .addValue("email", email, Types.VARCHAR).addValue("phone", patch.phoneNumber(), Types.VARCHAR)
                .addValue("phonePresent", patch.phonePresent())
                .addValue("confirmationId", email == null ? null : UUID.randomUUID(), Types.OTHER);
        return jdbc.update("""
                UPDATE users SET first_name = COALESCE(:firstName, first_name),
                    last_name = COALESCE(:lastName, last_name), email = COALESCE(:email, email),
                    email_confirmed_at = CASE WHEN :email <> email THEN NULL ELSE email_confirmed_at END,
                    email_confirmation_id = CASE WHEN :email <> email
                        THEN :confirmationId ELSE email_confirmation_id END,
                    auth_version = auth_version + CASE WHEN :email <> email THEN 1 ELSE 0 END,
                    phone_number = CASE WHEN :phonePresent THEN :phone ELSE phone_number END,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = :id
                """, params) > 0;
    }

    boolean confirmEmail(UUID confirmationId) {
        return jdbc.update("""
                UPDATE users SET email_confirmed_at = CURRENT_TIMESTAMP,
                    email_confirmation_id = NULL, updated_at = CURRENT_TIMESTAMP
                WHERE email_confirmation_id = :id
                """, Map.of("id", confirmationId)) > 0;
    }

    Optional<String> findPassword(long id) {
        return jdbc.query("SELECT password FROM users WHERE id = :id AND email_confirmed_at IS NOT NULL",
                Map.of("id", id), (rs, row) -> rs.getString("password")).stream().findFirst();
    }

    boolean updatePassword(long id, String previousHash, String newHash) {
        return jdbc.update("""
                UPDATE users SET password = :newHash, auth_version = auth_version + 1,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = :id AND password = :previousHash AND email_confirmed_at IS NOT NULL
                """, Map.of("id", id, "previousHash", previousHash, "newHash", newHash)) > 0;
    }

    boolean delete(long id) {
        return jdbc.update("DELETE FROM users WHERE id = :id", Map.of("id", id)) > 0;
    }

    boolean lockUser(long id) {
        return !jdbc.queryForList("SELECT id FROM users WHERE id = :id FOR UPDATE",
                Map.of("id", id), Long.class).isEmpty();
    }

    List<Role> findRoles() {
        return jdbc.query("SELECT id, name FROM roles ORDER BY id", Map.of(),
                (rs, row) -> new Role(rs.getInt("id"), rs.getString("name")));
    }

    void assignRole(long id, int roleId) {
        jdbc.update("INSERT INTO user_role (user_id, role_id) VALUES (:id, :role) ON CONFLICT DO NOTHING",
                Map.of("id", id, "role", roleId));
    }

    void removeRole(long id, int roleId) {
        jdbc.update("DELETE FROM user_role WHERE user_id = :id AND role_id = :role",
                Map.of("id", id, "role", roleId));
    }

    private static List<String> roles(ResultSet rs) throws SQLException {
        var array = rs.getArray("roles");
        try {
            return List.of((String[]) array.getArray());
        } finally {
            array.free();
        }
    }

    record Credentials(long id, String passwordHash, int authVersion, UUID confirmationId) {}
}

record Actor(long id, List<String> roles) {
    boolean isAdmin() {
        return roles.contains("admin");
    }
}
