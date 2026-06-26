package ru.practicum.event.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.client.collector.CollectorClient;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.service.EventService;
import ru.practicum.grpc.stats.action.ActionTypeProto;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class PublicEventController {

    private static final String USER_ID_HEADER = "X-EWM-USER-ID";

    private final EventService eventService;
    private final CollectorClient collectorClient;

    @GetMapping("/{id}")
    public EventFullDto getEventById(
            @PathVariable Long id,
            @RequestHeader(USER_ID_HEADER) Long userId
    ) {
        collectorClient.collectUserAction(
                userId,
                id,
                ActionTypeProto.ACTION_VIEW
        );

        return eventService.getPublishedEventById(id);
    }

    @GetMapping
    public List<EventShortDto> getEvents(@RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) String rangeStart,
            @RequestParam(required = false) String rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {
        return eventService.getPublicEvents(
                text,
                categories,
                paid,
                rangeStart,
                rangeEnd,
                onlyAvailable,
                sort,
                from,
                size
        );
    }

    @GetMapping("/recommendations")
    public List<EventShortDto> getRecommendations(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestParam(defaultValue = "10") int maxResults
    ) {
        return eventService.getRecommendations(userId, maxResults);
    }

    @PutMapping("/{eventId}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void likeEvent(
            @PathVariable Long eventId,
            @RequestHeader(USER_ID_HEADER) Long userId
    ) {
        eventService.likeEvent(userId, eventId);
    }
}