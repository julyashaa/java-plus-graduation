package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;

import java.util.HashMap;
import java.util.List;

@FeignClient(name = "request-service", path = "/internal/requests")
public interface RequestClient {

    @GetMapping("/events/{eventId}/participants")
    List<ParticipationRequestDto> getEventParticipants(
            @RequestParam("userId") Long userId,
            @PathVariable Long eventId
    );

    @PatchMapping("/events/{eventId}/status")
    EventRequestStatusUpdateResult changeRequestStatus(
            @RequestParam("userId") Long userId,
            @PathVariable Long eventId,
            @RequestBody EventRequestStatusUpdateRequest request
    );

    @GetMapping("/events/{eventId}/confirmed-count")
    Integer getEventParticipantsWithConfirm(
            @RequestParam("userId") Long userId,
            @PathVariable Long eventId
    );

    @GetMapping("/events/confirmed-counts")
    HashMap<Long, Integer> getAllConfirmedParticipants(
            @RequestParam("eventIds") List<Long> eventIds
    );
}