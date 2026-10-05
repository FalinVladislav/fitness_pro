# Фитнес-Про — система управления фитнес-центром

Полноценное веб-приложение для ежедневной работы фитнес-центра. В одном интерфейсе собраны клиенты, абонементы, расписание, запись на занятия, посещения, уведомления и отчёты. Проект объединяет REST API на Spring Boot, интерфейс на React и PostgreSQL.

**Быстрый старт:** Docker Compose собирает и запускает все три части проекта; приложение открывается на <http://localhost:5173>.

## Что умеет приложение

- Учёт клиентов, тренеров, залов и видов тренировок.
- Продажа, продление, отмена и заморозка абонементов; учёт оставшихся посещений.
- Расписание занятий, запись клиентов, отмена записей и отметка посещений.
- Заявки клиентов на абонементы с одобрением или отклонением администратором.
- Уведомления внутри приложения и отчёты по выручке, посещаемости и нагрузке тренеров.
- Доступ к операциям по ролям `ADMIN`, `MANAGER`, `TRAINER`, `CLIENT`.

## Архитектура и стек

| Часть | Технологии |
| --- | --- |
| Backend | Java 17, Spring Boot 3.3, Spring Security, Spring Data JPA, Hibernate, Maven |
| Аутентификация | JWT, BCrypt, проверка ролей через Spring Security |
| База данных | PostgreSQL 16 в Docker, Flyway для миграций схемы |
| Frontend | React, TypeScript, Vite, React Router, Axios |
| Запуск | Docker Compose, Nginx для статических файлов и проксирования API |
| Документация API | springdoc OpenAPI и Swagger UI |

```text
Браузер → localhost:5173 → Nginx → /api/* → Spring Boot → PostgreSQL
                               └→ React, CSS и JavaScript
```

В Docker браузер обращается к одному адресу. Nginx отдаёт собранный frontend и передаёт запросы `/api`, `/swagger-ui` и `/v3/api-docs` backend. Backend обращается к PostgreSQL по имени сервиса `postgres` во внутренней сети Compose. Порты backend и базы наружу не опубликованы.

Основные каталоги: `backend/` — Java-код, тесты и миграции; `frontend/` — React-приложение; `docker/` — Compose и пример переменных окружения; `.github/workflows/ci.yml` — проверки при push и pull request.

## Быстрый запуск: весь проект в Docker

Нужен запущенный Docker Desktop с поддержкой Compose. Из корня репозитория выполните в PowerShell:

```powershell
cd docker
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
docker compose up --build
```

`Copy-Item` нужен только при первом запуске. Перед запуском замените значения `POSTGRES_PASSWORD` и `JWT_SECRET` в `docker/.env` на свои. Для JWT используйте длинный случайный секрет длиной не менее 32 байт. Файл `docker/.env` исключён из Git; шаблон `.env.example` хранится в репозитории.

| Адрес | Назначение |
| --- | --- |
| <http://localhost:5173> | Приложение |
| <http://localhost:5173/swagger-ui/index.html> | Swagger UI |
| <http://localhost:5173/v3/api-docs> | OpenAPI JSON |

При первом запуске Compose собирает backend и frontend, поднимает PostgreSQL, ждёт готовности базы, затем запускает Spring Boot и Nginx. `--build` включает в образы изменения исходников. Обычный повторный запуск — `docker compose up`; для запуска в фоне добавьте `-d`. Порт frontend можно изменить через `FRONTEND_HOST_PORT` в `.env`.

Полезные команды из каталога `docker/`:

```powershell
docker compose ps                 # состояние сервисов
docker compose logs -f backend    # логи backend
docker compose down              # остановить, сохранив Docker-БД
```

`docker compose down -v` **удаляет данные Docker-БД** вместе с volume. Локальный PostgreSQL, установленный на компьютере, эта команда не затрагивает.

Пароль `POSTGRES_PASSWORD` применяется PostgreSQL при первом создании volume. Если база уже существует, изменение этого значения в `.env` не меняет пароль пользователя внутри БД: его нужно изменить отдельно в PostgreSQL.

### Профили `demo` и `prod-like`

По умолчанию Compose использует профиль `demo`. После создания схемы `DataSeeder` добавляет тестовых пользователей и данные для знакомства с приложением. Для запуска без автоматического добавления демо-данных установите в `docker/.env`:

```dotenv
SPRING_PROFILES_ACTIVE=prod-like
```

Затем пересоздайте backend: `docker compose up -d --build --force-recreate backend`. Переключение профиля сохраняет уже существующие данные в Docker volume. В профиле `prod-like` `DataSeeder` не запускается.

## Запуск без Docker Compose

Для разработки нужны Java 17, Maven, Node.js 22 с npm и работающий локальный PostgreSQL. Создайте пустую базу `fitness_pro` и настройте пользователя с доступом к ней. Профиль `local` берёт параметры подключения и JWT-секрет из игнорируемого Git файла `backend/src/main/resources/application-local.yml`.

Из корня репозитория:

```powershell
if (-not (Test-Path backend/src/main/resources/application-local.yml)) { Copy-Item backend/src/main/resources/application-local.example.yml backend/src/main/resources/application-local.yml }
```

Откройте созданный файл и укажите свои `url`, `username`, `password` и длинный `app.jwt.secret`. В двух отдельных терминалах выполните:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE = 'local'
mvn spring-boot:run
```

```powershell
cd frontend
npm ci
npm run dev
```

Frontend откроется на <http://localhost:5173>, backend — на <http://localhost:8080>, Swagger UI — на <http://localhost:8080/swagger-ui/index.html>. Vite передаёт запросы `/api` локальному backend на порту `8080`. Для смены локальных параметров подключения измените свой `application-local.yml`.

### Миграции Flyway

`backend/src/main/resources/db/migration/V1__init_schema.sql` создаёт **только схему**, без пользователей и тестовых записей. Flyway хранит историю применённых миграций и проверяет их контрольные суммы; уже применённую миграцию следует оставлять неизменной, а изменение схемы оформлять следующей версией. Hibernate настроен на `ddl-auto: validate`: он проверяет соответствие entity схеме, но не создаёт таблицы.

На новой пустой БД Flyway применит `V1` автоматически. В Docker Compose включён `baseline-on-migrate` версии `1` для старого Docker volume с ранее созданной схемой. Если ваша **локальная** БД уже существовала до Flyway, перед запуском нужно отдельно проверить её соответствие `V1` и выполнить baseline версии `1`; профиль `local` не делает это автоматически. Baseline только записывает исходную версию в историю Flyway и не запускает SQL из `V1`.

| Профиль | Источник параметров БД и JWT | Демо-данные |
| --- | --- | --- |
| `local` | `application-local.yml` | Да |
| `demo` | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` | Да |
| `prod-like` | те же обязательные переменные окружения | Нет |
| `test` | `TEST_DB_URL`, `TEST_DB_USERNAME`, `TEST_DB_PASSWORD`; необязательный `TEST_JWT_SECRET` | Нет |

## Демо-аккаунты

Доступны при первоначальном заполнении базы в профилях `demo` и `local`:

| Роль | Email | Пароль |
| --- | --- | --- |
| Администратор | `admin@example.com` | `admin123` |
| Руководитель | `manager@example.com` | `manager123` |
| Тренер | `trainer@example.com` | `trainer123` |
| Клиент | `client@example.com` | `client123` |

`DataSeeder` проверяет наличие администратора перед заполнением базы. Эти пароли предназначены только для демонстрации.

## API и доступ

| Раздел | Примеры запросов | Основной сценарий |
| --- | --- | --- |
| Аутентификация | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` | Регистрация, вход, текущий пользователь |
| Клиенты и справочники | `/api/clients`, `/api/trainers`, `/api/halls`, `/api/training-types`, `/api/membership-types` | Данные фитнес-центра |
| Абонементы и заявки | `/api/memberships`, `/api/membership-requests` | Покупка, продажа, заморозка, решение по заявке |
| Занятия и записи | `/api/schedule`, `/api/bookings` | Расписание и бронирования |
| Посещения | `/api/visits` | Отметка и история посещений |
| Уведомления и отчёты | `/api/notifications`, `/api/reports` | События и аналитика |

Полный список методов, параметров и DTO доступен в Swagger UI. Для защищённых запросов выполните `POST /api/auth/login`, скопируйте поле `token` и вставьте его в кнопку **Authorize** в Swagger. Backend ожидает заголовок `Authorization: Bearer <JWT>`.

Роли ограничивают действия: администратор управляет данными и операциями; руководитель просматривает отчёты; тренер работает со своими занятиями и отмечает посещения; клиент управляет своими записями и абонементами. Конкретные разрешения задаются `@PreAuthorize` у методов контроллеров. Пароли хранятся как BCrypt-хеши, серверная сессия не используется, а ошибки API возвращаются в JSON с полями `timestamp`, `status`, `error` и `message`.

### Примеры бизнес-правил

- На занятие можно записаться до его начала при действующем абонементе и наличии свободных мест. Повторная активная запись одного клиента запрещена; отменённую запись можно восстановить при соблюдении проверок.
- Клиент может отменить запись заранее: стандартный срок — не позднее чем за 120 минут до начала занятия. При отмене занятия активные записи переводятся в `CANCELLED`.
- Абонементы одного клиента с активным или замороженным статусом не должны пересекаться по сроку; заморозка ограничена настройками тарифа.
- При отметке посещения проверяется право на вход и, если требуется, уменьшается число оставшихся посещений. Для одной тренировки повторная отметка отклоняется.

## Тесты и CI

```powershell
cd backend
mvn test
```

```powershell
cd frontend
npm ci
npm run build
```

GitHub Actions выполняет backend-тесты и сборку frontend при каждом push и pull request. Dockerfile backend собирает JAR с `-DskipTests`; тесты выполняются отдельной командой и в CI.
