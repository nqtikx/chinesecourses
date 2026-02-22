# ChineseCourses Admin

Админ-панель для управления курсами китайского языка. Spring Boot (backend) + React/TypeScript (frontend), PostgreSQL, Docker.

---

## Быстрый старт (Docker)

### Требования

- Docker Desktop (Windows/macOS) или Docker + Docker Compose (Linux)
- Порт **8080** должен быть свободен

### Запуск

```bash
git clone <url-репозитория>
cd chinesecourses-admin
docker compose up --build -d
```

Первый запуск занимает 2-3 минуты (сборка образов). После старта приложение доступно по адресу:

> **http://localhost:8080**

### Остановка

```bash
docker compose down        # остановить (данные сохраняются)
docker compose down -v     # остановить и удалить данные БД
```

### Пересборка после изменений в коде

```bash
docker compose down
docker compose build --no-cache
docker compose up -d
```

---

## Учётные записи

При первом запуске автоматически создаются два пользователя:

| Логин     | Пароль  | Роль           | Описание                        |
|-----------|---------|----------------|---------------------------------|
| `admin`   | `admin` | `ROLE_ADMIN`   | Полный доступ ко всем функциям  |
| `teacher` | `admin` | `ROLE_TEACHER` | Преподаватель (ограниченный доступ) |

### Различия ролей

**Администратор (ROLE_ADMIN):**
- Создание, редактирование, архивирование всех сущностей
- Управление персонами (абитуриенты/слушатели)
- Управление преподавателями
- Управление курсами, семестрами, группами, записями, расписанием
- Просмотр записей по семестру

**Преподаватель (ROLE_TEACHER):**
- Просмотр курсов, семестров, групп
- Просмотр записей только своих групп
- Просмотр персон только из своих групп
- Отметка посещаемости (создание/редактирование attendance)
- Нет доступа к управлению персонами, преподавателями, расписанием

---

## Работа через Web-интерфейс

### Вход

1. Откройте **http://localhost:8080**
2. Введите логин и пароль (например, `admin` / `admin`)
3. Нажмите «Войти»

### Навигация (боковое меню)

| Раздел | URL | Описание |
|--------|-----|----------|
| Главная | `/` | Дашборд |
| Курсы | `/courses` | Список курсов |
| Семестры | `/semesters` | Семестры выбранного курса |
| Группы | `/groups` | Учебные группы в семестре |
| Абитуриенты и слушатели | `/persons` | Поиск/создание/редактирование персон (только админ) |
| Преподаватели | `/teachers` | Управление преподавателями (только админ) |
| Записи | `/enrollments` | Записи студентов в группы |
| Занятия | `/lessons` | Список занятий группы |
| Посещаемость | `/attendance` | Отметка посещаемости |
| Расписание | `/schedule` | Правила расписания для групп (только админ) |

### Типичный сценарий работы

1. **Создать курс** → Курсы → «Добавить курс»
2. **Создать семестр** → Семестры → выбрать курс → «Добавить семестр»
3. **Добавить персону** → Абитуриенты → «Добавить персону» (ФИО, телефон, email)
4. **Назначить преподавателя** → Преподаватели → «Добавить преподавателя» → выбрать персону
5. **Создать группу** → Группы → выбрать семестр → «Добавить группу» → назначить преподавателя
6. **Записать студента** → Записи → выбрать группу → «Добавить запись» → указать студента, уровень, статус
7. **Создать занятие** → Занятия → выбрать группу → «Добавить занятие»
8. **Отметить посещаемость** → Посещаемость → выбрать занятие → отметить статус каждого студента

---

## Работа через Postman (REST API)

Все API-эндпоинты находятся по адресу `http://localhost:8080/api/...`

### Аутентификация

Все запросы (кроме login и health) требуют JWT-токен.

**1. Получить токен:**

```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin"
}
```

Ответ:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**2. Использовать токен:** добавьте заголовок к каждому запросу:

```
Authorization: Bearer <ваш_токен>
```

В Postman: вкладка Authorization → Type: Bearer Token → вставить токен.

### Справочные эндпоинты

| Метод | URL | Описание |
|-------|-----|----------|
| GET | `/api/health` | Проверка работоспособности (без авторизации) |
| GET | `/api/admin/me` | Информация о текущем пользователе |

### Курсы

| Метод | URL | Роль | Описание |
|-------|-----|------|----------|
| GET | `/api/courses` | ADMIN, TEACHER | Список курсов (топ 50) |
| GET | `/api/courses/{id}` | ADMIN, TEACHER | Курс по ID |
| POST | `/api/courses` | ADMIN | Создать курс |
| PUT | `/api/courses/{id}` | ADMIN | Обновить курс |
| PATCH | `/api/courses/{id}/archive` | ADMIN | Архивировать/восстановить |

**Пример создания курса:**
```
POST http://localhost:8080/api/courses
Authorization: Bearer <токен>
Content-Type: application/json

{
  "name": "Разговорный китайский HSK2",
  "description": "Интенсивный курс разговорного китайского языка"
}
```

### Семестры

| Метод | URL | Роль | Описание |
|-------|-----|------|----------|
| GET | `/api/semesters?courseId={id}` | ADMIN, TEACHER | Семестры курса |
| GET | `/api/semesters/{id}` | ADMIN, TEACHER | Семестр по ID |
| POST | `/api/semesters` | ADMIN | Создать семестр |
| PUT | `/api/semesters/{id}` | ADMIN | Обновить семестр |
| PATCH | `/api/semesters/{id}/archive` | ADMIN | Архивировать/восстановить |

**Пример:**
```
POST http://localhost:8080/api/semesters
Authorization: Bearer <токен>
Content-Type: application/json

{
  "courseId": 1,
  "name": "Осень 2026",
  "startDate": "2026-09-01",
  "endDate": "2026-12-31"
}
```

### Персоны (Абитуриенты и слушатели)

| Метод | URL | Роль | Описание |
|-------|-----|------|----------|
| GET | `/api/persons?lastNamePrefix={prefix}` | ADMIN | Поиск по фамилии |
| GET | `/api/persons/{id}` | ADMIN, TEACHER | Персона по ID |
| POST | `/api/persons` | ADMIN | Создать персону |
| PUT | `/api/persons/{id}` | ADMIN | Обновить персону |
| PATCH | `/api/persons/{id}/archive` | ADMIN | Архивировать/восстановить |

**Пример:**
```
POST http://localhost:8080/api/persons
Authorization: Bearer <токен>
Content-Type: application/json

{
  "lastName": "Новиков",
  "firstName": "Дмитрий",
  "middleName": "Олегович",
  "birthDate": "2002-03-15",
  "phone": "+375291234567",
  "email": "novikov@example.com"
}
```

### Преподаватели

| Метод | URL | Роль | Описание |
|-------|-----|------|----------|
| GET | `/api/teachers/{id}` | ADMIN, TEACHER | Преподаватель по ID |
| POST | `/api/teachers` | ADMIN | Создать преподавателя из персоны |
| PATCH | `/api/teachers/{id}/archive` | ADMIN | Архивировать/восстановить |

**Пример (сначала создайте персону, потом назначьте преподавателем):**
```
POST http://localhost:8080/api/teachers
Authorization: Bearer <токен>
Content-Type: application/json

{
  "personId": 7
}
```

### Учебные группы

| Метод | URL | Роль | Описание |
|-------|-----|------|----------|
| GET | `/api/study-groups?semesterId={id}` | ADMIN, TEACHER | Группы семестра |
| GET | `/api/study-groups/{id}` | ADMIN, TEACHER | Группа по ID |
| POST | `/api/study-groups` | ADMIN | Создать группу |
| PUT | `/api/study-groups/{id}` | ADMIN | Обновить группу |
| PATCH | `/api/study-groups/{id}/archive` | ADMIN | Архивировать/восстановить |

**Пример:**
```
POST http://localhost:8080/api/study-groups
Authorization: Bearer <токен>
Content-Type: application/json

{
  "semesterId": 1,
  "teacherId": 1,
  "name": "Группа B-1"
}
```

### Записи (Enrollment)

| Метод | URL | Роль | Описание |
|-------|-----|------|----------|
| GET | `/api/enrollments?groupId={id}` | ADMIN, TEACHER | Записи по группе |
| GET | `/api/enrollments?semesterId={id}&status=ACTIVE&level=HSK1` | ADMIN | Записи по семестру (фильтры) |
| GET | `/api/enrollments/{id}` | ADMIN, TEACHER | Запись по ID |
| POST | `/api/enrollments` | ADMIN | Создать запись |
| PUT | `/api/enrollments/{id}` | ADMIN | Обновить запись |
| PATCH | `/api/enrollments/{id}/archive` | ADMIN | Архивировать/восстановить |

Возможные статусы: `APPLICANT`, `ACTIVE`, `COMPLETED`, `EXPELLED`, `TRANSFERRED`  
Уровни: `HSK1`, `HSK2`, `HSK3`, `HSK4`, `HSK5`, `HSK6`

**Пример:**
```
POST http://localhost:8080/api/enrollments
Authorization: Bearer <токен>
Content-Type: application/json

{
  "studentId": 4,
  "payerId": 4,
  "semesterId": 1,
  "groupId": 1,
  "status": "ACTIVE",
  "level": "HSK1"
}
```

### Занятия (Lesson Sessions)

| Метод | URL | Роль | Описание |
|-------|-----|------|----------|
| GET | `/api/lesson-sessions?groupId={id}` | ADMIN, TEACHER | Занятия группы |
| GET | `/api/lesson-sessions/{id}` | ADMIN, TEACHER | Занятие по ID |
| POST | `/api/lesson-sessions` | ADMIN | Создать занятие |
| PUT | `/api/lesson-sessions/{id}` | ADMIN | Обновить занятие |
| PATCH | `/api/lesson-sessions/{id}/archive` | ADMIN | Архивировать/восстановить |

**Пример:**
```
POST http://localhost:8080/api/lesson-sessions
Authorization: Bearer <токен>
Content-Type: application/json

{
  "groupId": 1,
  "teacherId": 1,
  "startsAt": "2026-03-01T10:00:00Z",
  "endsAt": "2026-03-01T11:30:00Z",
  "topic": "Тоны китайского языка"
}
```

### Посещаемость (Attendance)

| Метод | URL | Роль | Описание |
|-------|-----|------|----------|
| GET | `/api/attendance?lessonSessionId={id}` | ADMIN, TEACHER | Посещаемость на занятии |
| GET | `/api/attendance?enrollmentId={id}` | ADMIN, TEACHER | Посещаемость студента |
| GET | `/api/attendance/{id}` | ADMIN, TEACHER | Запись по ID |
| POST | `/api/attendance` | ADMIN, TEACHER | Создать запись |
| PUT | `/api/attendance/{id}` | ADMIN, TEACHER | Обновить запись |
| PATCH | `/api/attendance/{id}/archive` | ADMIN, TEACHER | Архивировать/восстановить |

Статусы: `PRESENT`, `ABSENT`, `LATE`, `EXCUSED`

**Пример:**
```
POST http://localhost:8080/api/attendance
Authorization: Bearer <токен>
Content-Type: application/json

{
  "lessonSessionId": 1,
  "enrollmentId": 1,
  "status": "PRESENT"
}
```

### Расписание (Group Schedule Rules)

| Метод | URL | Роль | Описание |
|-------|-----|------|----------|
| GET | `/api/group-schedule-rules?groupId={id}` | ADMIN | Правила расписания группы |
| GET | `/api/group-schedule-rules/{id}` | ADMIN | Правило по ID |
| POST | `/api/group-schedule-rules` | ADMIN | Создать правило |
| PUT | `/api/group-schedule-rules/{id}` | ADMIN | Обновить правило |
| PATCH | `/api/group-schedule-rules/{id}/archive` | ADMIN | Архивировать/восстановить |

**Пример:**
```
POST http://localhost:8080/api/group-schedule-rules
Authorization: Bearer <токен>
Content-Type: application/json

{
  "groupId": 1,
  "dayOfWeek": "MONDAY",
  "startTime": "10:00",
  "endTime": "11:30",
  "room": "Аудитория 305"
}
```

### Архивирование (общий формат)

Для любого PATCH `/{id}/archive`:
```json
{ "archived": true }   // архивировать
{ "archived": false }  // восстановить
```

---

## Тестовые данные

При первом запуске автоматически загружаются следующие данные:

### Персоны
| ID | Фамилия | Имя | Роль |
|----|---------|-----|------|
| 1 | Ли | Мэйлинь | Преподаватель (teacher_id=1) |
| 2 | Иванов | Пётр | Студент |
| 3 | Козлова | Анна | Студент |
| 4 | Сидоров | Алексей | Студент |
| 5 | Петрова | Мария | Студент |
| 6 | Чжан | Вэй | Преподаватель (teacher_id=6) |

### Курсы
| ID | Название |
|----|----------|
| 1 | Базовый курс HSK1 |
| 2 | Продвинутый курс HSK3 |

### Семестры
| Курс | Семестр | Период |
|------|---------|--------|
| Базовый курс HSK1 | Весна 2026 | 01.02.2026 — 30.06.2026 |
| Продвинутый курс HSK3 | Весна 2026 | 01.02.2026 — 30.06.2026 |

### Группы
| Группа | Преподаватель | Семестр |
|--------|---------------|---------|
| Группа A-1 | Ли Мэйлинь | Базовый курс HSK1, Весна 2026 |
| Группа A-2 | Чжан Вэй | Базовый курс HSK1, Весна 2026 |

### Записи студентов
| Студент | Группа | Статус |
|---------|--------|--------|
| Иванов Пётр | Группа A-1 | ACTIVE |
| Козлова Анна | Группа A-1 | ACTIVE |
| Петрова Мария | Группа A-1 | ACTIVE |
| Сидоров Алексей | Группа A-2 | APPLICANT |

---

## Вход от учителя

1. Откройте **http://localhost:8080**
2. Логин: `teacher`, пароль: `admin`
3. Учитель «Ли Мэйлинь» привязан к **Группе A-1**
4. Учитель видит только свои группы и студентов этих групп
5. Учитель может отмечать посещаемость, но не может создавать курсы/персоны/группы

Через Postman:
```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "teacher",
  "password": "admin"
}
```

---

## Архитектура

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Nginx      │────▶│  Spring Boot │────▶│  PostgreSQL   │
│  (frontend)  │     │  (backend)   │     │   :5432       │
│  :80 → :8080 │     │  :8080       │     │               │
└──────────────┘     └──────────────┘     └──────────────┘
  React SPA            REST API             Данные + Liquibase
  Tailwind CSS         JWT Auth
```

- **Frontend:** React 18 + TypeScript + Vite + Tailwind CSS, собирается в статику и раздаётся через Nginx
- **Backend:** Spring Boot 3.5 + JPA/Hibernate + Liquibase, REST API
- **Nginx** проксирует `/api/*` запросы на backend, всё остальное — SPA fallback
- **Liquibase** автоматически создаёт/мигрирует таблицы при старте

---

## Локальная разработка (без Docker)

### Backend
```bash
# Запустить PostgreSQL (через Docker или локально) на порту 5433
# Создать БД "chinesecourses"
cd chinesecourses-admin
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Frontend
```bash
cd frontend
npm install
npm run dev
```

Frontend dev-сервер запустится на **http://localhost:5173** с проксированием API на `:8080`.

---

## Структура проекта

```
chinesecourses-admin/
├── src/main/java/com/bntu/chinesecourses/
│   ├── config/          # SecurityConfig, JWT, Properties
│   ├── controller/      # REST контроллеры
│   ├── exception/       # Обработка ошибок
│   ├── model/
│   │   ├── dto/         # Request/Response DTO
│   │   └── entity/      # JPA сущности
│   ├── repository/      # Spring Data JPA репозитории
│   ├── security/        # JWT фильтр и сервис
│   └── service/         # Бизнес-логика
├── src/main/resources/
│   ├── db/changelog/    # Liquibase миграции
│   ├── application.yml
│   └── application-docker.yml
├── frontend/
│   ├── src/
│   │   ├── api/         # Axios клиент
│   │   ├── components/  # UI компоненты
│   │   ├── context/     # AuthContext
│   │   ├── pages/       # Страницы
│   │   └── types/       # TypeScript типы
│   ├── nginx.conf       # Конфиг Nginx для Docker
│   └── Dockerfile       # Multi-stage сборка frontend
├── Dockerfile           # Multi-stage сборка backend
├── docker-compose.yml   # Оркестрация всех сервисов
└── .env                 # Переменные окружения
```
