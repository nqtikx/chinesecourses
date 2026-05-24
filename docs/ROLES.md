# Роли и доступ API

## Роли

- **ROLE_ADMIN** — полный доступ ко всем ресурсам и операциям.
- **ROLE_TEACHER** — доступ только к своим группам, занятиям и посещаемости; может отмечать посещаемость (явка/неявка).
- **ROLE_USER** — зарезервировано на будущее.

## Вход в систему

- **Логин:** `POST /api/auth/login` с телом `{ "username": "...", "password": "..." }`.
- Ответ: `{ "token": "JWT..." }`. В заголовке `Authorization: Bearer <token>` передавать токен для защищённых запросов.
- **Текущий пользователь:** `GET /api/admin/me` возвращает `id`, `username`, `role`, `teacherId` (для учителя), `createdAt`.

## Учётная запись учителя

Учитель — это пользователь в таблице `admin_user` с `role = 'ROLE_TEACHER'` и заполненным `teacher_id` (ссылка на запись в таблице `teacher`). Создать такого пользователя можно вручную в БД или через будущий админ-интерфейс.

Пример (после создания записи в `teacher` и хеша пароля):

```sql
INSERT INTO admin_user (username, password_hash, role, teacher_id, created_at)
VALUES ('teacher1', '$2a$10$...', 'ROLE_TEACHER', 1, now());
```

## Доступ по ролям

| Ресурс / действие | ADMIN | TEACHER |
|-------------------|-------|---------|
| Курсы, семестры   | CRUD + список | Только чтение (get, list) |
| Учителя (teachers)| CRUD | Только get по id |
| Группы (study-groups) | CRUD | Только свои группы (get, list по semesterId) |
| Занятия (lesson-sessions) | CRUD | Только свои группы (get, list по groupId) |
| Записи (enrollments) | CRUD, список по семестру | get по id (только свои), список по groupId (только свои группы) |
| Персоны (persons) | CRUD, поиск | get по id только для персон из своих групп |
| Посещаемость (attendance) | Всё | Создание/обновление/чтение только по своим занятиям |
| Расписание групп (group-schedule-rules) | CRUD | Нет доступа |

## Посещаемость (явка/неявка)

Статусы в API: `PRESENT` (явка), `ABSENT` (неявка), `LATE`, `EXCUSED`. Создание и обновление записей посещаемости: `POST /attendance`, `PUT /attendance/{id}`.
