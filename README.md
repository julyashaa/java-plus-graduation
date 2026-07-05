# Explore With Me — Microservices Edition

## О проекте

Explore With Me — сервис для поиска и публикации событий. Пользователи могут создавать мероприятия, участвовать в событиях других пользователей, оставлять заявки на участие, просматривать опубликованные события и получать персональные рекомендации.

Проект реализован в виде микросервисной архитектуры с использованием Spring Cloud, Apache Kafka и gRPC.

---

# Архитектура проекта

Проект разделён на инфраструктурные, бизнес-сервисы и сервисы рекомендаций.

---

# Инфраструктурные сервисы

## discovery-server

Сервис обнаружения Eureka.

Отвечает за регистрацию и поиск микросервисов внутри системы.

---

## config-server

Централизованный сервер конфигурации.

Все сервисы получают свои настройки через Spring Cloud Config Server.

---

## gateway-server

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
* получение рекомендаций;
* лайки мероприятий;
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

# Система рекомендаций

В проекте реализована рекомендательная система, основанная на анализе действий пользователей.

---

## collector

Получает пользовательские действия по gRPC.

Поддерживаются действия:

* VIEW
* REGISTER
* LIKE

Публикует события в Kafka.

---

## aggregator

Обрабатывает пользовательские действия.

Вычисляет похожесть мероприятий и публикует результаты в Kafka.

---

## analyzer

Хранит информацию о пользовательских действиях и похожести мероприятий.

Предоставляет gRPC API для:

* получения рекомендаций;
* получения похожих мероприятий;
* расчёта рейтинга событий.

---

# Kafka

Используются следующие топики:

```text
stats.user-actions.v1
stats.events-similarity.v1
```

---

# Взаимодействие сервисов

## event-service

Использует:

* user-service
* request-service
* stats-server
* collector
* analyzer

---

## request-service

Использует:

* event-service
* user-service
* collector

---

## comment-service

Использует:

* event-service
* user-service

---

# Внутренний API

Взаимодействие между бизнес-сервисами реализовано с помощью Spring OpenFeign.

## User API

```text
/internal/users/**
```

Используется сервисами:

* event-service
* request-service
* comment-service

---

## Event API

```text
/ internal/events/**
```

Используется сервисами:

* request-service
* comment-service

---

## Request API

```text
/internal/requests/**
```

Используется сервисом:

* event-service

---

# gRPC API

gRPC используется внутри рекомендательной системы.

## Collector API

Получение пользовательских действий:

* VIEW
* REGISTER
* LIKE

---

## Analyzer API

Предоставляет:

* рекомендации пользователя;
* похожие мероприятия;
* рейтинг мероприятий.

---

# Рейтинг мероприятий

Рейтинг формируется на основе пользовательских действий:

* просмотров;
* регистраций;
* лайков.

---

# Новые возможности API

## Получение рекомендаций

```http
GET /events/recommendations
```

Возвращает список рекомендованных мероприятий.

---

## Лайк мероприятия

```http
PUT /events/{eventId}/like
```

Поставить лайк может только пользователь, подтвердивший участие в мероприятии.

---

## Заголовок пользователя

Для рекомендаций используется:

```text
X-EWM-USER-ID
```

---

# Конфигурация

Все конфигурации располагаются в Config Server.

Путь:

```text
infra/config-server/src/main/resources/config/
```

Для каждого сервиса используется собственная директория:

* event-service
* user-service
* request-service
* comment-service
* gateway-server
* stats-server
* collector
* aggregator
* analyzer

---

# Отказоустойчивость

Для обеспечения надёжности используются:

* Spring OpenFeign
* Resilience4j
* Retry
* CircuitBreaker

Реализованы fallback-механизмы.

Примеры:

* при недоступности request-service количество подтверждённых заявок считается равным 0;
* при недоступности user-service возвращаются заглушки пользователей;
* при недоступности сервисов рекомендаций основной функционал продолжает работу;
* ошибки межсервисного взаимодействия не приводят к ошибкам 5xx.
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
* Apache Kafka
* gRPC
* Apache Avro
* PostgreSQL
* Docker
* Maven

---

# Внешний API

Спецификации REST API:

* [Main Service API](./spec/ewm-main-service-spec.json)
* [Statistics Service API](./spec/ewm-stats-service-spec.json)

---

# Дополнительная функциональность — комментарии

Реализована трёхуровневая модель доступа.

## Публичный API

```text
GET /comments/{commentId}
GET /events/{eventId}/comments
```

---

## Приватный API

```text
POST   /users/{userId}/comments
GET    /users/{userId}/comments/{commentId}
GET    /users/{userId}/comments
PATCH  /users/{userId}/comments/{commentId}
DELETE /users/{userId}/comments/{commentId}
```

---

## Административный API

```text
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
5. collector
6. aggregator
7. analyzer
8. user-service
9. request-service
10. comment-service
11. event-service

После запуска сервисы:

* регистрируются в Eureka;
* получают конфигурацию из Config Server;
* подключаются к Kafka;
* становятся доступны через Gateway.

---

# Репозитории

Групповой проект:

https://github.com/julyashaa/java-explore-with-me-plus

Дипломный проект:

https://github.com/julyashaa/java-plus-graduation
