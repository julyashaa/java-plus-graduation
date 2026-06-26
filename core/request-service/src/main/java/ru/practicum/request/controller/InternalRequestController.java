package ru.practicum.request.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.enums.RequestStatus;
import ru.practicum.request.service.RequestService;

import java.util.HashMap;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/requests")
public class InternalRequestController {

    private final RequestService requestService;

    @GetMapping("/events/{eventId}/participants")
    public List<ParticipationRequestDto> getEventParticipants(
            @RequestParam Long userId,
            @PathVariable Long eventId
    ) {
        return requestService.getEventParticipants(userId, eventId);
    }

    @PatchMapping("/events/{eventId}/status")
    public EventRequestStatusUpdateResult changeRequestStatus(
            @RequestParam Long userId,
            @PathVariable Long eventId,
            @RequestBody EventRequestStatusUpdateRequest request
    ) {
        return requestService.changeRequestStatus(userId, eventId, request);
    }

    @GetMapping("/events/{eventId}/confirmed-count")
    public Integer getEventParticipantsWithConfirm(
            @RequestParam Long userId,
            @PathVariable Long eventId
    ) {
        return requestService.getEventParticipantsWithConfirm(userId, eventId);
    }

    @GetMapping("/events/confirmed-counts")
    public HashMap<Long, Integer> getAllConfirmedParticipants(
            @RequestParam List<Long> eventIds
    ) {
        return requestService.getAllEventParticipiants(eventIds, RequestStatus.CONFIRMED);
    }

    @GetMapping("/events/{eventId}/users/{userId}/confirmed")
    public Boolean isUserConfirmedParticipant(
            @PathVariable Long eventId,
            @PathVariable Long userId
    ) {
        return requestService.isUserConfirmedParticipant(userId, eventId);
    }
}