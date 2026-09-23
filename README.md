# Система автоматизации фитнес-центра «Фитнес-Про»

Полноценное клиент-серверное веб-приложение для учета клиентов, абонементов, расписания тренировок, онлайн-записи, посещений, уведомлений и управленческой отчетности.

## Стек

- Backend: Java 17, Spring Boot, Spring Security, JWT, Spring Data JPA/Hibernate, PostgreSQL, Maven.
- Frontend: React, TypeScript, React Router, Axios, Vite.
- База данных: PostgreSQL, реляционная модель по предметной области фитнес-центра.

## PostgreSQL

Для запуска нужно выбрать профиль Spring Boot. При локальной разработке используется профиль `local`: он подключается к `localhost:5432`, БД `fitness_pro`, пользователю `postgres` и паролю `admin`. Значения можно переопределить переменными `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` и `JWT_SECRET`.

## Полный запуск в Docker

Docker Compose поднимает PostgreSQL, Spring Boot backend и React frontend одной командой. Перед первым запуском создайте файл `docker/.env` из шаблона и задайте собственный JWT-секрет и пароль БД. Файл с реальными значениями игнорируется Git.

```powershell
cd docker
Copy-Item .env.example .env
docker compose up --build
```

После запуска откройте:

- frontend: `http://localhost:5173`;
- Swagger UI: `http://localhost:5173/swagger-ui/index.html`.

Профиль `demo` создаёт тестовых пользователей. Для запуска без демо-данных установите в `docker/.env` значение `SPRING_PROFILES_ACTIVE=prod-like`. Backend и PostgreSQL доступны только внутри сети Compose; Nginx передаёт backend-запросы по путям `/api` и `/swagger-ui`. Поэтому запуск не конфликтует с локальными backend и PostgreSQL.

В Compose включён `baseline-on-migrate` на версии `1`: чистая Docker-БД применит `V1__init_schema.sql`, а старая Docker-БД с уже существующей схемой версии 1 получит запись baseline без повторного создания таблиц. Это относится только к Docker dev-окружению; для локальной PostgreSQL baseline выполняется отдельно, как описано ниже.

Команда `docker compose down` остановит контейнеры и сохранит Docker-базу. `docker compose down -v` дополнительно удалит Docker volume с данными; локальная PostgreSQL на `localhost:5432` при этом не затрагивается.

## Запуск backend

```bash
cd backend
$env:SPRING_PROFILES_ACTIVE = "local"
mvn spring-boot:run
```

Backend стартует на `http://localhost:8080`. На чистой БД Flyway применит `V1__init_schema.sql` и создаст таблицы. Hibernate работает в режиме `validate`: он только проверяет соответствие схемы Java entity и не изменяет её.

Если локальная БД уже была создана прежним режимом `ddl-auto: update`, перед первым запуском с Flyway нужно один раз выполнить Flyway baseline на версии `1`; существующие таблицы и данные при этом не изменяются.

Тестовые данные создаёт `DataSeeder` только в профилях `local` и `demo`. Профили `test` и `prod-like` требуют отдельную БД и переменные окружения; они не содержат fallback-паролей, JWT-секрета или демо-пользователей.

## Документация API (Swagger)

После запуска backend интерактивная документация доступна по адресу:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI-спецификация доступна по адресу `http://localhost:8080/v3/api-docs`.
Чтобы проверить защищенные запросы в Swagger UI, выполните `POST /api/auth/login`, скопируйте поле `token` из ответа, нажмите **Authorize** и вставьте JWT.

## Запуск frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend будет доступен на `http://localhost:5173`.

## Тестовые пользователи

- `admin@example.com` / `admin123` — администратор.
- `manager@example.com` / `manager123` — руководитель.
- `trainer@example.com` / `trainer123` — тренер.
- `client@example.com` / `client123` — клиент.

## Роли

- Администратор управляет клиентами, абонементами, расписанием, посещениями и справочниками.
- Клиент просматривает расписание, записывается на тренировки, отменяет записи и читает уведомления.
- Тренер видит свое расписание, участников и может фиксировать посещения и управлять занятиями.
- Руководитель просматривает отчеты и аналитику без изменения операционных данных.

## Основные API

- `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`
- `GET|POST|PUT|DELETE /api/clients`
- `GET|POST|PUT|DELETE /api/membership-types`
- `GET /api/memberships`, `POST /api/memberships/sell`, `POST /api/memberships/buy`, `POST /api/memberships/{id}/renew`
- `GET|POST|PUT|DELETE /api/schedule`, `POST /api/schedule/{id}/cancel`, `POST /api/schedule/{id}/complete`
- `GET /api/bookings`, `GET /api/bookings/my`, `GET /api/bookings/schedule/{scheduleId}`, `POST /api/bookings`, `POST /api/bookings/{id}/cancel`
- `GET /api/visits`, `GET /api/visits/trainer/my`, `POST /api/visits/check-in`, `POST /api/visits/{id}/cancel`
- `GET /api/reports/revenue`, `GET /api/reports/attendance`, `GET /api/reports/trainers-load`, `GET /api/reports/popular-training-types`
- `GET /api/notifications`, `POST /api/notifications/{id}/read`

## Бизнес-правила

Система проверяет активность и срок абонемента, остаток посещений, свободные места на занятии, повторную запись после отмены, конфликты тренера и зала, сроки отмены записи и автоматически отменяет активные записи при отмене занятия.

## Подготовка к публикации

В репозиторий должны попадать исходники, конфиги, `pom.xml`, `package.json`, `package-lock.json`, Docker Compose и документация. Локальные артефакты сборки (`target`, `dist`, `node_modules`) и настройки IDE исключены через `.gitignore`.
