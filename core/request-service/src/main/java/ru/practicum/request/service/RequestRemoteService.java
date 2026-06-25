package ru.practicum.request.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.client.EventClient;
import ru.practicum.client.UserClient;
import ru.practicum.event.dto.EventRequestInfoDto;
import ru.practicum.exception.ConditionsNotMetException;
import ru.practicum.user.UserDto;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestRemoteService {

    private final EventClient eventClient;
    private final UserClient userClient;

    @Retry(name = "eventService")
    @CircuitBreaker(
            name = "eventService",
            fallbackMethod = "getEventRequestInfoFallback"
    )
    public EventRequestInfoDto getEventRequestInfo(Long eventId) {
        return eventClient.getEventRequestInfo(eventId);
    }

    @Retry(name = "userService")
    @CircuitBreaker(
            name = "userService",
            fallbackMethod = "getUserByIdFallback"
    )
    public UserDto getUserById(Long userId) {
        return userClient.getUserById(userId);
    }

    private EventRequestInfoDto getEventRequestInfoFallback(
            Long eventId,
            Throwable throwable
    ) {
        log.warn(
                "event-service недоступен для eventId={}",
                eventId,
                throwable
        );

        throw new ConditionsNotMetException(
                "Сервис мероприятий временно недоступен"
        );
    }

    private UserDto getUserByIdFallback(
            Long userId,
            Throwable throwable
    ) {
        log.warn(
                "user-service недоступен для userId={}",
                userId,
                throwable
        );

        throw new ConditionsNotMetException(
                "Сервис пользователей временно недоступен"
        );
    }
}