# Тестирование API через Postman

## 1. Запуск приложения

Запустите приложение (например, `mvn spring-boot:run`). По умолчанию сервер на **http://localhost:8080** (проверьте `server.port` в `application.yml`).

Базовый URL для запросов: **http://localhost:8080**

---

## 2. Логин (получить токен)

**Запрос:** `POST http://localhost:8080/api/auth/login`

**Headers:**  
`Content-Type: application/json`

**Body (raw, JSON):**
```json
{
  "username": "admin",
  "password": "ВАШ_ПАРОЛЬ_АДМИНА"
}
```

**Ответ (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

Скопируйте значение `token` — оно понадобится для всех остальных запросов.

> Пароль админа задаётся в миграции БД (`update-admin-password.yml`). Если не знаете пароль — сгенерируйте новый BCrypt-хеш и обновите запись в таблице `admin_user`, либо временно верните в сиде известный пароль и пересоздайте БД.

---

## 3. Запросы с авторизацией

Для любого защищённого эндпоинта:

1. Вкладка **Authorization** → Type: **Bearer Token** → в поле Token вставьте полученный `token`.

Либо вручную в **Headers** добавьте:
- Key: `Authorization`  
- Value: `Bearer <ваш_токен>`

(слово `Bearer` и пробел перед токеном обязательны)

---

## 4. Проверка текущего пользователя (роль, teacherId)

**Запрос:** `GET http://localhost:8080/api/admin/me`  
**Authorization:** Bearer Token (как выше)

**Ответ (200) для админа:**
```json
{
  "id": 1,
  "username": "admin",
  "role": "ROLE_ADMIN",
  "teacherId": null,
  "createdAt": "2024-01-01T00:00:00Z"
}
```

Для учителя в ответе будет `"role": "ROLE_TEACHER"` и заполненный `teacherId`.

---

## 5. Примеры запросов под админом

После логина под `admin` и подстановки токена можно вызывать любые эндпоинты, например:

| Метод | URL | Описание |
|-------|-----|----------|
| GET | `http://localhost:8080/api/courses` | Список курсов |
| GET | `http://localhost:8080/api/semesters?courseId=1` | Семестры по курсу |
| GET | `http://localhost:8080/study-groups?semesterId=1` | Группы по семестру |
| GET | `http://localhost:8080/lesson-sessions?groupId=1` | Занятия по группе |
| GET | `http://localhost:8080/enrollments?groupId=1` | Записи по группе |
| GET | `http://localhost:8080/attendance?lessonSessionId=1` | Посещаемость по занятию |
| POST | `http://localhost:8080/attendance` | Создать запись посещаемости (body см. ниже) |

**Пример тела для создания посещаемости (POST /attendance):**
```json
{
  "lessonSessionId": 1,
  "enrollmentId": 1,
  "status": "PRESENT",
  "comment": null
}
```
Статусы: `PRESENT`, `ABSENT`, `LATE`, `EXCUSED`.

---

## 6. Тестирование под учителем (ROLE_TEACHER)

1. В БД должна быть запись в таблице `teacher` (например, id=1) и пользователь с ролью учителя:
   ```sql
   INSERT INTO admin_user (username, password_hash, role, teacher_id, created_at)
   VALUES ('teacher1', '$2a$10$...', 'ROLE_TEACHER', 1, now());
   ```
   Пароль нужно захешировать (BCrypt). Временно можно подставить тот же хеш, что и у admin, и использовать тот же пароль.

2. В Postman: **POST /api/auth/login** с `username: teacher1` и паролем учителя.

3. В ответе — новый токен. Подставьте его в **Authorization → Bearer Token**.

4. **GET /api/admin/me** — в ответе должно быть `"role": "ROLE_TEACHER"` и `"teacherId": 1`.

5. Проверка ограничений:
   - **GET /study-groups?semesterId=1** — только группы, где `teacher_id = 1`.
   - **GET /study-groups/999** — если группа не ваша: **403 Forbidden**.
   - **GET /attendance?lessonSessionId=1** — только если занятие принадлежит вашей группе; иначе **403**.
   - **POST /attendance** для занятия чужой группы — **403**.
   - **POST /study-groups** (создание группы) — **403** (только админ).

---

## 7. Типичные коды ответов

| Код | Значение |
|-----|----------|
| 200 | Успех |
| 201 | Создано (например, POST) |
| 400 | Неверный запрос (валидация, неверные параметры) |
| 401 | Нет или неверный токен |
| 403 | Нет прав (роль или чужие данные для учителя) |
| 404 | Ресурс не найден |
| 409 | Конфликт (дубликат и т.п.) |

---

## 8. Коллекция Postman (идея)

Можно создать коллекцию с переменными:

- **baseUrl**: `http://localhost:8080`
- **token**: задаётся вручную после логина или скриптом в тесте Login.

В тесте запроса Login добавьте в раздел "Tests":
```javascript
var json = pm.response.json();
if (json.token) {
  pm.collectionVariables.set("token", json.token);
}
```
Тогда остальные запросы коллекции могут использовать `Authorization: Bearer {{token}}`.
