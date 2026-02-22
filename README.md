# ChineseCourses Admin — WEB-приложение администратора курсов китайского языка

WEB-приложение для ведения базы данных абитуриентов и слушателей курсов китайского языка.  
Система разделена на две роли: **Администратор** и **Преподаватель**, каждая из которых имеет свой набор возможностей.

---

## Технологии

| Слой | Технология |
|------|-----------|
| Backend | Java 17+, Spring Boot 3, Spring Security, JWT |
| База данных | PostgreSQL + Liquibase (миграции) |
| Frontend | React 18, TypeScript, Vite, Tailwind CSS |
| Иконки | Lucide React |
| HTTP-клиент | Axios |

---

## Запуск приложения

### Предварительные требования

- **Java 17+** (JDK)
- **Maven** (или используйте встроенный `./mvnw`)
- **PostgreSQL** (запущенный сервер с созданной БД)
- **Node.js 18+** и **npm**

### 1. Настройка базы данных

Создайте базу данных PostgreSQL и задайте переменные окружения или используйте профиль `local`:

```bash
# Пример для application-local.yml (создайте файл, если отсутствует)
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/chinesecourses
    username: postgres
    password: postgres
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.yml
```

### 2. Переменные окружения

| Переменная | Описание | По умолчанию |
|-----------|---------|-------------|
| `JWT_SECRET` | Секретный ключ для JWT-токенов (обязательно) | — |
| `JWT_ISSUER` | Издатель токена | `chinesecourses` |
| `JWT_ACCESS_TTL` | Время жизни токена (ISO 8601 Duration) | `PT30M` (30 минут) |

### 3. Запуск Backend

```bash
# Из корневой директории проекта
set JWT_SECRET=my-super-secret-key-at-least-32-chars

# Windows (PowerShell)
$env:JWT_SECRET = "my-super-secret-key-at-least-32-chars"

# Сборка и запуск
./mvnw spring-boot:run
```

Backend запустится на **http://localhost:8080**

### 4. Запуск Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend запустится на **http://localhost:5173**

> Vite автоматически проксирует API-запросы на backend (`localhost:8080`).

### 5. Вход в систему

Откройте **http://localhost:5173** в браузере. Используйте учётные данные администратора, созданные миграцией.

---

## Матрица прав доступа

Система поддерживает три роли: `ROLE_ADMIN`, `ROLE_TEACHER`, `ROLE_USER`.

### Администратор (`ROLE_ADMIN`)

Администратор имеет **полный доступ** ко всем функциям системы:

| Раздел | Возможности |
|--------|------------|
| **Курсы** | Создание, просмотр, редактирование, архивация |
| **Семестры** | Создание, просмотр, редактирование, архивация |
| **Учебные группы** | Создание, просмотр, редактирование, архивация |
| **Абитуриенты и слушатели** | Полный CRUD: создание, поиск, редактирование, архивация |
| **Преподаватели** | Создание (привязка к персоне), просмотр, архивация |
| **Зачисления** | Создание, просмотр, редактирование, архивация |
| **Занятия** | Создание, просмотр, редактирование, архивация |
| **Посещаемость** | Создание, просмотр, редактирование, архивация |
| **Расписание** | Создание правил, просмотр, редактирование, архивация |

### Преподаватель (`ROLE_TEACHER`)

Преподаватель имеет **ограниченный доступ** — в основном просмотр и управление посещаемостью:

| Раздел | Возможности |
|--------|------------|
| **Курсы** | Только просмотр |
| **Семестры** | Только просмотр |
| **Учебные группы** | Только просмотр |
| **Абитуриенты и слушатели** | Нет доступа (раздел скрыт) |
| **Преподаватели** | Нет доступа (раздел скрыт) |
| **Зачисления** | Только просмотр (по группе) |
| **Занятия** | Только просмотр (по группе) |
| **Посещаемость** | Создание и редактирование |
| **Расписание** | Нет доступа (раздел скрыт) |

---

## Структура проекта

```
chinesecourses-admin/
├── src/main/java/com/bntu/chinesecourses/
│   ├── config/          # SecurityConfig, JwtProperties, PropertiesConfig
│   ├── controller/      # REST-контроллеры (11 контроллеров)
│   ├── exception/       # Обработка ошибок (ApiExceptionHandler)
│   ├── model/
│   │   ├── dto/         # DTO для запросов и ответов
│   │   └── entity/      # JPA-сущности
│   ├── repository/      # Spring Data JPA репозитории
│   ├── security/        # JwtAuthFilter, JwtTokenService
│   └── service/         # Бизнес-логика
├── src/main/resources/
│   ├── application.yml
│   └── db/changelog/    # Liquibase-миграции
├── frontend/
│   ├── src/
│   │   ├── api/         # Axios-клиент и API-модули
│   │   ├── components/  # Layout, Modal, Badge, Toast, ConfirmDialog
│   │   ├── context/     # AuthContext (JWT-авторизация)
│   │   ├── pages/       # 10 страниц (Dashboard, Courses, Semesters, ...)
│   │   └── types/       # TypeScript-типы
│   ├── package.json
│   └── vite.config.ts   # Прокси API-запросов
└── README.md
```

---

## API-эндпоинты

### Публичные (без авторизации)

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/api/health` | Проверка здоровья сервиса |
| POST | `/api/auth/login` | Авторизация (получение JWT-токена) |

### Защищённые (требуют JWT в заголовке `Authorization: Bearer <token>`)

#### Курсы (`/api/courses`)

| Метод | Путь | Роли | Описание |
|-------|------|------|----------|
| GET | `/api/courses` | ADMIN, TEACHER | Список курсов |
| GET | `/api/courses/{id}` | ADMIN, TEACHER | Курс по ID |
| POST | `/api/courses` | ADMIN | Создать курс |
| PUT | `/api/courses/{id}` | ADMIN | Обновить курс |
| PATCH | `/api/courses/{id}/archive` | ADMIN | Архивировать/восстановить |

#### Семестры (`/api/semesters`)

| Метод | Путь | Роли | Описание |
|-------|------|------|----------|
| GET | `/api/semesters?courseId={id}` | ADMIN, TEACHER | Семестры по курсу |
| GET | `/api/semesters/{id}` | ADMIN, TEACHER | Семестр по ID |
| POST | `/api/semesters` | ADMIN | Создать семестр |
| PUT | `/api/semesters/{id}` | ADMIN | Обновить семестр |
| PATCH | `/api/semesters/{id}/archive` | ADMIN | Архивировать/восстановить |

#### Учебные группы (`/study-groups`)

| Метод | Путь | Роли | Описание |
|-------|------|------|----------|
| GET | `/study-groups?semesterId={id}` | ADMIN, TEACHER | Группы по семестру |
| GET | `/study-groups/{id}` | ADMIN, TEACHER | Группа по ID |
| POST | `/study-groups` | ADMIN | Создать группу |
| PUT | `/study-groups/{id}` | ADMIN | Обновить группу |
| PATCH | `/study-groups/{id}/archive` | ADMIN | Архивировать/восстановить |

#### Абитуриенты и слушатели (`/persons`)

| Метод | Путь | Роли | Описание |
|-------|------|------|----------|
| GET | `/persons?lastNamePrefix={prefix}` | ADMIN | Поиск по фамилии |
| GET | `/persons/{id}` | ADMIN, TEACHER | Персона по ID |
| POST | `/persons` | ADMIN | Создать персону |
| PUT | `/persons/{id}` | ADMIN | Обновить персону |
| PATCH | `/persons/{id}/archive` | ADMIN | Архивировать/восстановить |

#### Преподаватели (`/teachers`)

| Метод | Путь | Роли | Описание |
|-------|------|------|----------|
| GET | `/teachers/{id}` | ADMIN, TEACHER | Преподаватель по ID |
| POST | `/teachers` | ADMIN | Создать преподавателя |
| PATCH | `/teachers/{id}/archive` | ADMIN | Архивировать/восстановить |

#### Зачисления (`/enrollments`)

| Метод | Путь | Роли | Описание |
|-------|------|------|----------|
| GET | `/enrollments?groupId={id}` | ADMIN, TEACHER | По группе |
| GET | `/enrollments?semesterId={id}` | ADMIN | По семестру (с фильтрами) |
| GET | `/enrollments/{id}` | ADMIN, TEACHER | Зачисление по ID |
| POST | `/enrollments` | ADMIN | Создать зачисление |
| PUT | `/enrollments/{id}` | ADMIN | Обновить зачисление |
| PATCH | `/enrollments/{id}/archive` | ADMIN | Архивировать/восстановить |

#### Занятия (`/lesson-sessions`)

| Метод | Путь | Роли | Описание |
|-------|------|------|----------|
| GET | `/lesson-sessions?groupId={id}` | ADMIN, TEACHER | По группе |
| GET | `/lesson-sessions/{id}` | ADMIN, TEACHER | Занятие по ID |
| POST | `/lesson-sessions` | ADMIN | Создать занятие |
| PUT | `/lesson-sessions/{id}` | ADMIN | Обновить занятие |
| PATCH | `/lesson-sessions/{id}/archive` | ADMIN | Архивировать/восстановить |

#### Посещаемость (`/attendance`)

| Метод | Путь | Роли | Описание |
|-------|------|------|----------|
| GET | `/attendance?lessonSessionId={id}` | ADMIN, TEACHER | По занятию |
| GET | `/attendance?enrollmentId={id}` | ADMIN, TEACHER | По зачислению |
| GET | `/attendance/{id}` | ADMIN, TEACHER | Запись по ID |
| POST | `/attendance` | ADMIN, TEACHER | Отметить посещение |
| PUT | `/attendance/{id}` | ADMIN, TEACHER | Обновить запись |
| PATCH | `/attendance/{id}/archive` | ADMIN, TEACHER | Архивировать/восстановить |

#### Расписание (`/group-schedule-rules`)

| Метод | Путь | Роли | Описание |
|-------|------|------|----------|
| GET | `/group-schedule-rules?groupId={id}` | ADMIN | Правила по группе |
| GET | `/group-schedule-rules/{id}` | ADMIN | Правило по ID |
| POST | `/group-schedule-rules` | ADMIN | Создать правило |
| PUT | `/group-schedule-rules/{id}` | ADMIN | Обновить правило |
| PATCH | `/group-schedule-rules/{id}/archive` | ADMIN | Архивировать/восстановить |

---

## Сборка для продакшена

### Backend

```bash
./mvnw clean package -DskipTests
java -jar target/chinesecourses-admin-*.jar
```

### Frontend

```bash
cd frontend
npm run build
```

Собранные файлы будут в `frontend/dist/`.

---

## Авторы

БНТУ — Белорусский национальный технический университет
