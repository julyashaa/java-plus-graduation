package ru.practicum.request.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.collector.CollectorClient;
import ru.practicum.event.dto.EventRequestInfoDto;
import ru.practicum.event.enums.EventState;
import ru.practicum.exception.ConditionsNotMetException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.grpc.stats.action.ActionTypeProto;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.request.enums.RequestStatus;
import ru.practicum.request.repository.ParticipationRequestRepository;


import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestService {
    private final ParticipationRequestRepository requestRepository;
    private final RequestMapper requestMapper;
    private final RequestRemoteService requestRemoteService;
    private final CollectorClient collectorClient;

    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        ensureUserExists(userId);
        List<ParticipationRequest> requests = requestRepository.findByRequester(userId);
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }
        return requestMapper.toDtos(requests);
    }

    @Transactional
    public ParticipationRequestDto addParticipationRequest(Long userId, Long eventId) {
        ensureUserExists(userId);
        EventRequestInfoDto event = getEventOrThrow(eventId);

        if (event.getInitiator().equals(userId)) {
            throw new ConflictException("Initiator cannot request participation in own event");
        }
        if (!EventState.PUBLISHED.equals(event.getState())) {
            throw new ConflictException("Event must be published");
        }
        if (requestRepository.existsByRequesterAndEvent(userId, eventId)) {
            throw new ConflictException("Participation request already exists");
        }

        int limit = getParticipantLimit(event);
        long confirmedCount = requestRepository.countByEventAndStatus(eventId, RequestStatus.CONFIRMED);
        if (limit > 0 && confirmedCount >= limit) {
            throw new ConflictException("The participant limit has been reached");
        }

        RequestStatus status;
        if (Boolean.FALSE.equals(event.getRequestModeration()) || limit == 0) {
            status = RequestStatus.CONFIRMED;
        } else {
            status = RequestStatus.PENDING;
        }

        LocalDateTime createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        ParticipationRequest request = ParticipationRequest.builder()
                .created(createdAt)
                .event(eventId)
                .requester(userId)
                .status(status)
                .build();
        ParticipationRequest saved = requestRepository.save(request);

        collectorClient.collectUserAction(
                userId,
                eventId,
                ActionTypeProto.ACTION_REGISTER
        );

        return requestMapper.toDto(saved);
    }

    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ensureUserExists(userId);
        ParticipationRequest request = requestRepository.findByIdAndRequester(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " was not found"));
        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequest saved = requestRepository.save(request);
        return requestMapper.toDto(saved);
    }

    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {
        ensureUserExists(userId);
        EventRequestInfoDto event = getEventForInitiator(userId, eventId);
        List<ParticipationRequest> requests = requestRepository.findByEvent(event.getId());
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }
        return requestMapper.toDtos(requests);
    }

    public Integer getEventParticipantsWithConfirm(Long userId, Long eventId) {
        ensureUserExists(userId);
        return Math.toIntExact(requestRepository.countByEventAndStatus(eventId, RequestStatus.CONFIRMED));
    }

    public HashMap<Long, Integer> getAllEventParticipiants(List<Long> eventIds, RequestStatus status) {
        List<Object[]> counts = requestRepository.countRequestsByEventIdsAndStatus(eventIds, status);

        HashMap<Long, Integer> resultMap = new HashMap<>();
        for (Object[] row : counts) {
            Long eventId = (Long) row[0];
            Long countLong = (Long) row[1];
            resultMap.put(eventId, countLong != null ? countLong.intValue() : 0);
        }
        return resultMap;
    }

    @Transactional
    public EventRequestStatusUpdateResult changeRequestStatus(
            Long userId,
            Long eventId,
            EventRequestStatusUpdateRequest updateRequest
    ) {
        ensureUserExists(userId);
        EventRequestInfoDto event = getEventForInitiator(userId, eventId);

        List<Long> requestIds = updateRequest.getRequestIds();
        if (requestIds == null || requestIds.isEmpty()) {
            return EventRequestStatusUpdateResult.builder()
                    .confirmedRequests(Collections.emptyList())
                    .rejectedRequests(Collections.emptyList())
                    .build();
        }

        List<ParticipationRequest> requests = requestRepository.findByEventAndIdIn(eventId, requestIds);
        ensureAllRequestsFound(requestIds, requests);

        for (ParticipationRequest request : requests) {
            if (!RequestStatus.PENDING.equals(request.getStatus())) {
                throw new ConditionsNotMetException("Request must have status PENDING");
            }
        }

        RequestStatus targetStatus = updateRequest.getStatus();
        if (targetStatus == null) {
            throw new ConditionsNotMetException("Request status must be specified");
        }

        List<ParticipationRequest> confirmed = new ArrayList<>();
        List<ParticipationRequest> rejected = new ArrayList<>();

        if (RequestStatus.CONFIRMED.equals(targetStatus)) {
            int limit = getParticipantLimit(event);
            long confirmedCount = requestRepository.countByEventAndStatus(eventId, RequestStatus.CONFIRMED);
            if (limit > 0 && confirmedCount >= limit) {
                throw new ConditionsNotMetException("The participant limit has been reached");
            }

            int available = limit == 0 ? Integer.MAX_VALUE : (int) (limit - confirmedCount);
            for (ParticipationRequest request : requests) {
                if (available > 0) {
                    request.setStatus(RequestStatus.CONFIRMED);
                    confirmed.add(request);
                    available--;
                } else {
                    request.setStatus(RequestStatus.REJECTED);
                    rejected.add(request);
                }
            }
            requestRepository.saveAll(requests);

            if (limit > 0 && available == 0) {
                List<ParticipationRequest> pendingRequests = requestRepository
                        .findByEventAndStatus(eventId, RequestStatus.PENDING);
                if (!pendingRequests.isEmpty()) {
                    for (ParticipationRequest pending : pendingRequests) {
                        pending.setStatus(RequestStatus.REJECTED);
                    }
                    requestRepository.saveAll(pendingRequests);
                    rejected.addAll(pendingRequests);
                }
            }
        } else if (RequestStatus.REJECTED.equals(targetStatus)) {
            for (ParticipationRequest request : requests) {
                request.setStatus(RequestStatus.REJECTED);
                rejected.add(request);
            }
            requestRepository.saveAll(requests);
        } else {
            throw new ConditionsNotMetException("Unsupported request status");
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(requestMapper.toDtos(confirmed))
                .rejectedRequests(requestMapper.toDtos(rejected))
                .build();
    }

    private void ensureUserExists(Long userId) {
        requestRemoteService.getUserById(userId);
    }

    private EventRequestInfoDto getEventOrThrow(Long eventId) {
        return requestRemoteService.getEventRequestInfo(eventId);
    }

    private EventRequestInfoDto getEventForInitiator(Long userId, Long eventId) {
        EventRequestInfoDto event = getEventOrThrow(eventId);
        if (!event.getInitiator().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }
        return event;
    }

    private int getParticipantLimit(EventRequestInfoDto event) {
        Integer limit = event.getParticipantLimit();
        return limit == null ? 0 : limit;
    }

    private void ensureAllRequestsFound(List<Long> requestIds, List<ParticipationRequest> requests) {
        if (requests.size() == requestIds.size()) {
            return;
        }
        Set<Long> found = new HashSet<>();
        for (ParticipationRequest request : requests) {
            found.add(request.getId());
        }
        for (Long id : requestIds) {
            if (!found.contains(id)) {
                throw new NotFoundException("Request with id=" + id + " was not found");
            }
        }
    }

    public Boolean isUserConfirmedParticipant(Long userId, Long eventId) {
        ensureUserExists(userId);
        getEventOrThrow(eventId);

        return requestRepository.existsByRequesterAndEventAndStatus(
                userId,
                eventId,
                RequestStatus.CONFIRMED
        );
    }
}
