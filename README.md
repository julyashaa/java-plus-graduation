# Explore With Me — Microservices Edition

## О проекте

Explore With Me — сервис для поиска и публикации событий. Пользователи могут создавать мероприятия, участвовать в событиях других пользователей, оставлять заявки на участие и просматривать опубликованные события.

Проект реализован в виде микросервисной архитектуры с использованием Spring Cloud.

---

# Архитектура проекта

Проект разделён на инфраструктурные и бизнес-сервисы.

## Инфраструктурные сервисы

### discovery-server

Сервис обнаружения Eureka.

Отвечает за регистрацию и поиск микросервисов внутри системы.

---

### config-server

Централизованный сервер конфигурации.

Все сервисы получают свои настройки через Spring Cloud Config Server.

---

### gateway-server

Единая точка входа во внешнее API.

Выполняет маршрутизацию запросов к бизнес-сервисам.

---

# Бизнес-сервисы

## event-service

Основной сервис системы.

Отвечает за:

* создание событий;
* публикацию мероприятий;
* поиск событий;
* просмотр событий;
* работу с категориями;
* работу с подборками;
* административное управление событиями.

---

## user-service

Отвечает за:

* регистрацию пользователей;
* получение информации о пользователях;
* административное управление пользователями.

---

## request-service

Отвечает за:

* создание заявок на участие;
* подтверждение заявок;
* отмену заявок;
* управление лимитами участников.

---

## comment-service

Дополнительный сервис комментариев.

Позволяет:

* создавать комментарии;
* редактировать комментарии;
* удалять комментарии;
* модерировать комментарии.

---

## stats-server

Сервис статистики.

Сохраняет информацию о просмотрах событий и предоставляет агрегированную статистику.


---

# Взаимодействие сервисов

## event-service

Использует:

* user-service
* request-service
* stats-server

---

## request-service

Использует:

* event-service
* user-service

---

## comment-service

Использует:

* event-service
* user-service

---

# Внутренний API

Взаимодействие между сервисами реализовано с помощью Spring OpenFeign.

## User API

```
/internal/users/**
```

Используется сервисами:

* event-service
* request-service
* comment-service

---

## Event API

```
/internal/events/**
```

Используется сервисами:

* request-service
* comment-service

---

## Request API

```
/internal/requests/**
```

Используется сервисом:

* event-service

---

# Конфигурация

Все конфигурации располагаются в Config Server.

Путь:

```
infra/config-server/src/main/resources/config/
```

Для каждого сервиса используется собственная директория:

* event-service
* user-service
* request-service
* comment-service
* gateway-server
* stats-server

Все сервисы получают настройки автоматически через Spring Cloud Config.

---

# Отказоустойчивость

Для обеспечения надёжности системы используются:

* Spring OpenFeign
* Resilience4j
* Retry
* CircuitBreaker

Реализованы fallback-механизмы.

Примеры:

* при недоступности request-service количество подтверждённых заявок считается равным 0;
* при недоступности stats-server сервис событий продолжает работу;
* ошибки межсервисного взаимодействия не приводят к ошибкам 5xx.

Проведено тестирование отказоустойчивости путём последовательного отключения микросервисов.

---

# Технологии

* Java 21
* Spring Boot 3
* Spring Cloud
* Spring Data JPA
* OpenFeign
* Eureka Server
* Config Server
* Spring Cloud Gateway
* Resilience4j
* PostgreSQL
* Maven
* Docker

---

# Внешний API

Спецификации REST API:

- [Main Service API](./spec/ewm-main-service-spec.json)
- [Statistics Service API](./spec/ewm-stats-service-spec.json)

Групповой проект:

https://github.com/julyashaa/java-explore-with-me-plus

---

# Дополнительная функциональность — комментарии к событиям

Сервис комментариев предоставляет REST API для работы с комментариями к событиям.

Реализована трёхуровневая модель доступа:

* Публичный доступ — просмотр комментариев.
* Приватный доступ — работа со своими комментариями.
* Административный доступ — модерация комментариев.

---

## Публичный API

```
GET /comments/{commentId}
GET /events/{eventId}/comments
```

---

## Приватный API

```
POST   /users/{userId}/comments
GET    /users/{userId}/comments/{commentId}
GET    /users/{userId}/comments
PATCH  /users/{userId}/comments/{commentId}
DELETE /users/{userId}/comments/{commentId}
```

---

## Административный API

```
GET    /admin/comments
PATCH  /admin/comments/{commentId}
DELETE /admin/comments/{commentId}
```

Администратор может:

* просматривать комментарии по фильтрам;
* редактировать любые комментарии;
* удалять любые комментарии.

---

# Запуск проекта

Порядок запуска:

1. discovery-server
2. config-server
3. gateway-server
4. stats-server
5. user-service
6. request-service
7. comment-service
8. event-service

После запуска сервисы автоматически:

* регистрируются в Eureka;
* получают конфигурацию из Config Server;
* становятся доступны через Gateway.

---

# Репозиторий

Дипломный проект:

https://github.com/julyashaa/java-plus-graduation
