# Система автоматизации фитнес-центра «Фитнес-Про»

Полноценное клиент-серверное веб-приложение для учета клиентов, абонементов, расписания тренировок, онлайн-записи, посещений, уведомлений и управленческой отчетности.

## Стек

- Backend: Java 17, Spring Boot, Spring Security, JWT, Spring Data JPA/Hibernate, PostgreSQL, Maven.
- Frontend: React, TypeScript, React Router, Axios, Vite.
- База данных: PostgreSQL, реляционная модель по предметной области фитнес-центра.

## Запуск PostgreSQL

```bash
cd docker
docker compose up -d
```

База будет доступна на `localhost:5433`, БД `fitness_pro`, пользователь `fitness`, пароль `fitness`.

## Запуск backend

```bash
cd backend
mvn spring-boot:run
```

Backend стартует на `http://localhost:8080`. При первом запуске Hibernate создаст таблицы, а `DataSeeder` добавит тестовые данные.

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
