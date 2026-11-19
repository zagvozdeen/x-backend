# Аналог Twitter/X

- **Название:** Аналог Twitter с двумя микросервисами для работы с пользователями и сообщениями
- **Студент:** Загвоздин Денис Сергеевич
- **Группа:** РИМ-250950

## Запуск

1. `docker compose up -d`
2. `./mvnw package`
3. `export JWT_SECRET="$(openssl rand -hex 32)"`
4. `export ADMIN_EMAIL=admin@example.com`
5. `export ADMIN_PASSWORD='password'`
6. `java -jar users/target/users-0.0.1-SNAPSHOT.jar`
7. `java -jar messages/target/messages-0.0.1-SNAPSHOT.jar`

Запустятся сервисы:

| Сервис | HTTP | PostgreSQL | Имя базы, пользователь и пароль |
| --- | --- | --- | --- |
| Users | `http://127.0.0.1:9000` | `127.0.0.1:5432` | `users` |
| Messages | `http://127.0.0.1:9001` | `127.0.0.1:5433` | `messages` |

## Сервис пользователей

| Метод и путь | Доступ | Действие |
| --- | --- | --- |
| `POST /api/v1/users` | Все | Регистрация с ролью `user`, HTTP 201 и `data.confirmation_id` |
| `POST /api/v1/auth/login` | Все | Вход по подтверждённому email и паролю, токен и профиль |
| `POST /api/v1/auth/confirm-email` | Все | Подтвердить email по ID регистрации и учебному коду |
| `GET /api/v1/auth/me` | После входа | ID текущего пользователя и актуальные роли. Этот маршрут вызывает Messages |
| `GET /api/v1/users` | Все | Имена и роли без email и телефона. Фильтры `role`, `ids` через запятую, `limit` (1–100, по умолчанию 20), `offset` (по умолчанию 0) |
| `GET /api/v1/users/{id}` | Сам пользователь или admin | Полный профиль |
| `PATCH /api/v1/users/{id}` | Сам пользователь или admin | Изменить имя, фамилию, email или телефон. Новый email надо подтвердить. `"phone_number": null` очищает номер |
| `PUT /api/v1/users/me/password` | После входа | Изменить свой пароль по текущему паролю |
| `DELETE /api/v1/users/{id}` | Сам пользователь или admin | Удалить профиль и назначения ролей из БД Users |
| `GET /api/v1/roles` | После входа | Справочник ролей с ID |
| `GET /api/v1/users/{id}/roles` | Сам пользователь или admin | Названия ролей пользователя |
| `PUT /api/v1/users/{id}/roles/{role_id}` | admin | Назначить роль |
| `DELETE /api/v1/users/{id}/roles/{role_id}` | admin | Снять роль |

Все ошибки возвращаются в одном формате. Текст в `message` на английском:

```json
{
  "code": 401,
  "error": "UNAUTHORIZED",
  "message": "A valid bearer token is required",
  "data": null
}
```

Пример регистрации:

```sh
curl -i http://127.0.0.1:9000/api/v1/users \
  -H 'Content-Type: application/json' \
  -d '{"first_name":"Иван","last_name":"Иванов","email":"ivan@example.test","password":"example-pass-32"}'
```

## Сервис сообщений

| Метод и путь | Доступ | Действие |
| --- | --- | --- |
| `POST /api/v1/messages` | Роль `producer` | Опубликовать сообщение, HTTP 201 |
| `GET /api/v1/messages` | Все | Список сообщений от новых к старым. Параметры `author_id`, `limit` (1–100, по умолчанию 20), `offset` (по умолчанию 0) |
| `GET /api/v1/messages/{id}` | Все | Получить сообщение |
| `PATCH /api/v1/messages/{id}` | Автор или `admin` | Изменить текст |
| `DELETE /api/v1/messages/{id}` | Автор или `admin` | Удалить сообщение |
