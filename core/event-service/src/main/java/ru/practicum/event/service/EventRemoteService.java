package ru.practicum.event.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.client.RequestClient;
import ru.practicum.client.UserClient;
import ru.practicum.user.UserDto;
import ru.practicum.user.UserShortDto;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventRemoteService {
    private final UserClient userClient;
    private final RequestClient requestClient;

    @Retry(name = "userService")
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByIdFallback")
    public UserDto getUserById(Long userId) {
        return userClient.getUserById(userId);
    }

    @Retry(name = "userService")
    @CircuitBreaker(name = "userService", fallbackMethod = "getAllShortUsersFallback")
    public List<UserShortDto> getAllShortUsers() {
        return userClient.getAllShortUsers();
    }

    @Retry(name = "requestService")
    @CircuitBreaker(name = "requestService", fallbackMethod = "getConfirmedCountFallback")
    public Integer getConfirmedCount(Long userId, Long eventId) {
        return requestClient.getEventParticipantsWithConfirm(userId, eventId);
    }

    @Retry(name = "requestService")
    @CircuitBreaker(name = "requestService", fallbackMethod = "getAllConfirmedParticipantsFallback")
    public HashMap<Long, Integer> getAllConfirmedParticipants(List<Long> eventIds) {
        return requestClient.getAllConfirmedParticipants(eventIds);
    }

    private UserDto getUserByIdFallback(Long userId, Throwable throwable) {
        log.warn("user-service недоступен. Возвращаем заглушку для userId={}", userId, throwable);
        return new UserDto(userId, "Unknown user", "unknown@email.com");
    }

    private List<UserShortDto> getAllShortUsersFallback(Throwable throwable) {
        log.warn("user-service недоступен. Возвращаем пустой список пользователей", throwable);
        return Collections.emptyList();
    }

    private Integer getConfirmedCountFallback(Long userId, Long eventId, Throwable throwable) {
        log.warn("request-service недоступен. confirmedRequests=0 для eventId={}", eventId, throwable);
        return 0;
    }

    private HashMap<Long, Integer> getAllConfirmedParticipantsFallback(List<Long> eventIds, Throwable throwable) {
        log.warn("request-service недоступен. confirmedRequests=0 для событий {}", eventIds, throwable);
        return new HashMap<>();
    }
}