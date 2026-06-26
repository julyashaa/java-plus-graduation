package ru.practicum.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.service.CategoryService;
import ru.practicum.client.analyzer.AnalyzerClient;
import ru.practicum.client.collector.CollectorClient;
import ru.practicum.event.dto.*;
import ru.practicum.event.enums.EventState;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.grpc.stats.action.ActionTypeProto;
import ru.practicum.grpc.stats.recommendation.RecommendedEventProto;
import ru.practicum.user.UserDto;
import ru.practicum.user.UserShortDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ru.practicum.constants.DatePatternConstant.DATE_TIME_PATTERN;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EventService {
    private final EventMapper eventMapper;
    private final EventRepository eventRepository;
    private final CategoryService categoryService;
    private final AnalyzerClient analyzerClient;
    private final CollectorClient collectorClient;
    private final EventRemoteService eventRemoteService;

    @Transactional
    public EventFullDto create(NewEventDto dto, long userId) {
        log.info("Получен запрос на создание события от пользователя с ID={} dto={}", userId, dto);
        if (dto.getEventDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Не корректное дата события");
        }
        Event event = eventMapper.toEntity(dto);
        event.setCreatedOn(LocalDateTime.now());
        event.setInitiator(userId);
        event.setState(EventState.PENDING);
        event.setRating(0.0);
        if (event.getPaid() == null) {
            event.setPaid(false);
        }
        if (event.getParticipantLimit() == null) {
            event.setParticipantLimit(0);
        }
        if (event.getRequestModeration() == null) {
            event.setRequestModeration(true);
        }
        eventRepository.save(event);
        EventFullDto eventFullDto = fillingFieldsInEventFullDto(event);
        log.info("Успешно создано событие с ID={}", eventFullDto);
        return eventFullDto;
    }

    public List<EventShortDto> getEventsForUser(Long initiatorId, Integer from, Integer size) {
        log.info("Запрос списка событий для инициатора с ID={} (from={}, size={})", initiatorId, from, size);
        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findByInitiator(initiatorId, pageable);
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }
        log.info("Найдено {} событий для инициатора с ID={}", events.size(), initiatorId);

        return fillingFieldsInEventShortDtos(events);
    }

    public EventDto getEventById(Long eventId) {
        log.info("getEventById - Запрос получения события по его ID = {}", eventId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с ID=" + eventId + " не найдено"));
        log.info("Найдено событие: {}", event);
        return fillingFieldsInEventDto(event);
    }

    @Transactional
    public EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest request) {
        log.info("Начинается обновление события {}", request);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с ID=" + eventId + " не найдено"));

        if (!event.getInitiator().equals(userId)) {
            throw new ConflictException("Вы не являетесь инициатором этого события");
        }

        if (!(event.getState().equals(EventState.CANCELED) || event.getState().equals(EventState.PENDING))) {
            throw new ConflictException("Изменять можно только ожидающие или отменённые события");
        }

        LocalDateTime newEventDate = null;
        if (request.getEventDate() != null) {
            newEventDate = LocalDateTime.parse(request.getEventDate(), DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
            if (newEventDate.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new BadRequestException("Дата и время события должны быть не раньше чем через 2 часа");
            }
            event.setEventDate(newEventDate);
        }

        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getLocation() != null) {
            event.setLocation(request.getLocation());
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }
        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }

        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;
                case SEND_TO_REVIEW:
                    event.setState(EventState.PENDING);
                    break;
            }
        }

        if (request.getCategory() != null) {
            event.setCategory(request.getCategory());
        }

        eventRepository.save(event);
        EventFullDto eventFullDto = fillingFieldsInEventFullDto(event);
        log.info("Событие {} успешно сохранено и обновлено", eventFullDto);
        return eventFullDto;
    }

    public List<EventFullDto> getEvents(
            List<Long> users,
            List<String> states,
            List<Long> categories,
            String rangeStart,
            String rangeEnd,
            int from,
            int size
    ) {
        log.info("Получение событий: users={}, categories={}, start={}, end={}, from={}, size={}, states={}",
                users, categories, rangeStart, rangeEnd, from, size, states);

        LocalDateTime start = null;
        LocalDateTime end = null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

        try {
            if (rangeStart != null) {
                start = LocalDateTime.parse(rangeStart, formatter);
            }
            if (rangeEnd != null) {
                end = LocalDateTime.parse(rangeEnd, formatter);
            }
        } catch (Exception e) {
            throw new BadRequestException("Некорректный формат даты");
        }

        if (start != null && end != null && start.isAfter(end)) {
            throw new BadRequestException("Конец интервала не может быть раньше начала");
        }

        List<Long> usersParam = null;
        if (users != null && !(users.size() == 1 && users.getFirst() == 0L)) {
            usersParam = users;
        }

        List<Long> categoriesParam = null;
        if (categories != null && !(categories.size() == 1 && categories.getFirst() == 0L)) {
            categoriesParam = categories;
        }

        List<EventState> statesParam =
                states != null && !states.isEmpty()
                        ? convertStringsToEventStates(states)
                        : null;

        List<Event> events = eventRepository.findEventsWithFilters(
                statesParam,
                usersParam,
                categoriesParam,
                Pageable.unpaged()
        );

        LocalDateTime finalStart = start;
        LocalDateTime finalEnd = end;

        if (finalStart != null) {
            events = events.stream()
                    .filter(event -> !event.getEventDate().isBefore(finalStart))
                    .toList();
        }

        if (finalEnd != null) {
            events = events.stream()
                    .filter(event -> !event.getEventDate().isAfter(finalEnd))
                    .toList();
        }

        if (from >= events.size()) {
            return Collections.emptyList();
        }

        int toIndex = Math.min(from + size, events.size());
        events = events.subList(from, toIndex);

        log.info("Обработка DTO завершена. Количество DTO: {}", events.size());

        return fillingFieldsInEventFullDtos(events);
    }

    public List<EventShortDto> getPublicEvents(
            String text,
            List<Long> categories,
            Boolean paid,
            String rangeStart,
            String rangeEnd,
            Boolean onlyAvailable,
            String sort,
            int from,
            int size
    ) {
        LocalDateTime start = null;
        LocalDateTime end = null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

        try {
            if (rangeStart != null) {
                start = LocalDateTime.parse(rangeStart, formatter);
            }
            if (rangeEnd != null) {
                end = LocalDateTime.parse(rangeEnd, formatter);
            }
        } catch (Exception e) {
            throw new BadRequestException("Некорректный формат даты");
        }

        if (start != null && end != null && start.isAfter(end)) {
            throw new BadRequestException("Дата начала не может быть позже даты окончания");
        }

        if (start == null) {
            start = LocalDateTime.now();
        }

        LocalDateTime finalStart = start;
        LocalDateTime finalEnd = end;

        List<Event> events = eventRepository.findByState(EventState.PUBLISHED, Pageable.unpaged());

        events = events.stream()
                .filter(event -> categories == null || categories.isEmpty()
                        || categories.contains(event.getCategory()))
                .filter(event -> paid == null || paid.equals(event.getPaid()))
                .filter(event -> !event.getEventDate().isBefore(finalStart))
                .filter(event -> finalEnd == null || !event.getEventDate().isAfter(finalEnd))
                .filter(event -> text == null || text.isBlank()
                        || event.getAnnotation().toLowerCase().contains(text.toLowerCase())
                        || event.getDescription().toLowerCase().contains(text.toLowerCase()))
                .toList();

        if (Boolean.TRUE.equals(onlyAvailable)) {
            events = events.stream()
                    .filter(event -> event.getParticipantLimit() == 0
                            || eventRemoteService.getConfirmedCount(
                            event.getInitiator(),
                            event.getId()
                    ) < event.getParticipantLimit())
                    .toList();
        }

        if ("EVENT_DATE".equalsIgnoreCase(sort)) {
            events = events.stream()
                    .sorted(Comparator.comparing(Event::getEventDate))
                    .toList();
        }

        if (from >= events.size()) {
            return Collections.emptyList();
        }

        int toIndex = Math.min(from + size, events.size());
        events = events.subList(from, toIndex);

        return fillingFieldsInEventShortDtos(events);
    }

    @Transactional
    public EventFullDto updateEvent(Long eventId, UpdateEventAdminRequest request) {
        log.info("Начинается обновление события admin {}", request);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        String action = request.getStateAction();
        LocalDateTime now = LocalDateTime.now();

        if (action != null) {
            switch (action) {
                case "PUBLISH_EVENT":
                    validatePublish(event, request, now);
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(now);
                    break;
                case "REJECT_EVENT":
                    validateCancel(event);
                    event.setState(EventState.CANCELED);
                    break;
                default:
                    break;
            }
        }

        if (request.getTitle() != null) {
            if (request.getTitle().length() < 3 || request.getTitle().length() > 120) {
                throw new BadRequestException("Заголовок должен быть от 3 до 120 символов");
            }
            event.setTitle(request.getTitle());
        }

        if (request.getAnnotation() != null) {
            if (request.getAnnotation().length() < 20 || request.getAnnotation().length() > 2000) {
                throw new BadRequestException("Длина аннотации некорректна");
            }
            event.setAnnotation(request.getAnnotation());
        }

        if (request.getDescription() != null) {
            if (request.getDescription().length() < 20 || request.getDescription().length() > 7000) {
                throw new BadRequestException("Длина описания некорректна");
            }
            event.setDescription(request.getDescription());
        }

        if (request.getEventDate() != null) {
            LocalDateTime newEventDate;
            try {
                newEventDate = LocalDateTime.parse(request.getEventDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (DateTimeParseException e) {
                throw new BadRequestException("Некорректный формат даты");
            }
            validateEventDate(newEventDate, now);
            event.setEventDate(newEventDate);
        }

        if (request.getCategory() != null) {
            event.setCategory(request.getCategory());
        }

        if (request.getLocation() != null) {
            event.setLocation(request.getLocation());
        }

        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }

        if (request.getParticipantLimit() != null) {
            if (request.getParticipantLimit() < 0) {
                throw new BadRequestException("Число участников не может быть отрицательным");
            }
            event.setParticipantLimit(request.getParticipantLimit());
        }

        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }
        eventRepository.save(event);
        EventFullDto eventFullDto = fillingFieldsInEventFullDto(event);
        log.info("Событие успешно сохранено и обновлено {}", eventFullDto);

        return eventFullDto;
    }

    @Transactional
    public EventFullDto getPublishedEventById(Long id) {
        log.info("getPublishedEventById - Запрос получения события по его ID = {}", id);

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Событие с ID=" + id + " не найдено"));

        if (!EventState.PUBLISHED.equals(event.getState())) {
            throw new NotFoundException("Событие с ID=" + id + " не найдено");
        }

        log.info("Найдено событие: {}", event);
        return fillingFieldsInEventFullDto(event);
    }

    public List<EventShortDto> getRecommendations(Long userId, int maxResults) {
        List<Long> eventIds = analyzerClient.getRecommendations(userId, maxResults)
                .stream()
                .map(RecommendedEventProto::getEventId)
                .toList();

        if (eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Event> events = eventRepository.findAllById(eventIds)
                .stream()
                .filter(event -> EventState.PUBLISHED.equals(event.getState()))
                .toList();

        return fillingFieldsInEventShortDtos(events);
    }

    @Transactional
    public void likeEvent(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с ID=" + eventId + " не найдено"));

        if (!EventState.PUBLISHED.equals(event.getState())) {
            throw new BadRequestException("Лайкать можно только опубликованные события");
        }

        if (!eventRemoteService.isUserConfirmedParticipant(userId, eventId)) {
            throw new BadRequestException("Пользователь может лайкать только посещённые мероприятия");
        }

        collectorClient.collectUserAction(
                userId,
                eventId,
                ActionTypeProto.ACTION_LIKE
        );
    }

    private void validatePublish(Event event, UpdateEventAdminRequest request, LocalDateTime now) {
        if (EventState.PUBLISHED.equals(event.getState())) {
            throw new ConflictException("Невозможно опубликовать — событие уже опубликовано");
        }

        if (!EventState.PENDING.equals(event.getState())) {
            throw new ConflictException("Можно публиковать только события в состоянии ожидания");
        }

        if (request.getEventDate() != null) {
            LocalDateTime eventDate;
            try {
                eventDate = LocalDateTime.parse(request.getEventDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Некорректный формат даты");
            }
            if (eventDate.isBefore(now.plusHours(1))) {
                throw new ConflictException("Дата мероприятия должна быть не раньше, чем через час от текущего времени");
            }
        } else if (event.getEventDate() != null) {
            if (event.getEventDate().isBefore(now.plusHours(1))) {
                throw new ConflictException("Дата мероприятия должна быть не раньше, чем через час от текущего времени");
            }
        }
    }

    private void validateCancel(Event event) {
        if (EventState.PUBLISHED.equals(event.getState())) {
            throw new ConflictException("Невозможно отменить опубликованное событие");
        }
        if (EventState.CANCELED.equals(event.getState())) {
            throw new ConflictException("Событие уже отменено");
        }
    }

    private void validateEventDate(LocalDateTime date, LocalDateTime now) {
        if (date.isBefore(now.plusHours(1))) {
            throw new BadRequestException("Дата события должна быть не раньше, чем через час от текущего времени");
        }
    }

    private List<EventState> convertStringsToEventStates(List<String> statesStrings) {
        if (statesStrings == null) {
            return null;
        }

        return statesStrings.stream()
                .map(String::toUpperCase)
                .map(s -> {
                    try {
                        return EventState.valueOf(s);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(e -> e != null)
                .collect(Collectors.toList());
    }

    private EventDto fillingFieldsInEventDto(Event event) {
        UserDto users = eventRemoteService.getUserById(event.getInitiator());
        CategoryDto categoryDto = categoryService.getById(event.getCategory());

        EventDto eventDto = eventMapper.toDto(event);
        eventDto.setInitiator(new UserShortDto(users.getId(), users.getName()));
        eventDto.setCategory(categoryDto);
        return eventDto;
    }

    private EventFullDto fillingFieldsInEventFullDto(Event event) {
        UserDto users = eventRemoteService.getUserById(event.getInitiator());
        CategoryDto categoryDto = categoryService.getById(event.getCategory());

        EventFullDto eventDto = eventMapper.toFullDto(event);

        eventDto.setInitiator(new UserShortDto(users.getId(), users.getName())
        );

        eventDto.setCategory(categoryDto);

        Integer confirmRequests = eventRemoteService.getConfirmedCount(event.getInitiator(),
                event.getId());

        eventDto.setConfirmedRequests(
                confirmRequests != null ? confirmRequests : 0
        );

        Double rating = analyzerClient
                .getInteractionsCount(List.of(event.getId()))
                .stream()
                .findFirst()
                .map(RecommendedEventProto::getScore)
                .orElse(0.0);

        eventDto.setRating(rating);

        return eventDto;
    }

    private List<EventFullDto> fillingFieldsInEventFullDtos(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }

        List<UserShortDto> users = eventRemoteService.getAllShortUsers();
        List<CategoryDto> categoryDtos = categoryService.getAll();

        Map<Long, CategoryDto> categoryMap = categoryDtos.stream()
                .collect(Collectors.toMap(
                        CategoryDto::getId,
                        Function.identity()
                ));

        Map<Long, UserShortDto> userMap = users.stream()
                .collect(Collectors.toMap(
                        UserShortDto::getId,
                        Function.identity()
                ));

        List<EventFullDto> eventFullDtos = eventMapper.toFullDtos(events);

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();

        HashMap<Long, Integer> participantLimitMapConfirm =
                eventRemoteService.getAllConfirmedParticipants(eventIds);

        Map<Long, Double> ratings = analyzerClient
                .getInteractionsCount(eventIds)
                .stream()
                .collect(Collectors.toMap(
                        RecommendedEventProto::getEventId,
                        RecommendedEventProto::getScore
                ));

        for (int i = 0; i < events.size(); i++) {
            Event event = events.get(i);
            EventFullDto dto = eventFullDtos.get(i);

            if (event.getCategory() != null) {
                dto.setCategory(categoryMap.get(event.getCategory()));
            }

            if (event.getInitiator() != null) {
                dto.setInitiator(userMap.get(event.getInitiator()));
            }

            Integer count = participantLimitMapConfirm.get(event.getId());
            dto.setConfirmedRequests(count != null ? count : 0);

            dto.setRating(ratings.getOrDefault(event.getId(), 0.0));
        }

        return eventFullDtos;
    }

    private List<EventShortDto> fillingFieldsInEventShortDtos(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }

        List<UserShortDto> users = eventRemoteService.getAllShortUsers();

        List<CategoryDto> categoryDtos = categoryService.getAll();

        Map<Long, CategoryDto> categoryMap = categoryDtos.stream()
                        .collect(Collectors.toMap(
                                CategoryDto::getId,
                                Function.identity()
                        ));

        Map<Long, UserShortDto> userMap = users.stream()
                        .collect(Collectors.toMap(
                                UserShortDto::getId,
                                Function.identity()
                        ));

        List<EventShortDto> eventShortDtos = eventMapper.toShortDtos(events);

        List<Long> eventIds = events.stream()
                        .map(Event::getId)
                        .toList();

        HashMap<Long, Integer> participantLimitMapConfirm =
                eventRemoteService.getAllConfirmedParticipants(eventIds);

        Map<Long, Double> ratings =
                analyzerClient.getInteractionsCount(eventIds)
                        .stream()
                        .collect(Collectors.toMap(
                                RecommendedEventProto::getEventId,
                                RecommendedEventProto::getScore
                        ));

        for (int i = 0; i < events.size(); i++) {
            Event event = events.get(i);
            EventShortDto dto = eventShortDtos.get(i);

            if (event.getCategory() != null) {
                dto.setCategory(categoryMap.get(event.getCategory()));
            }

            if (event.getInitiator() != null) {
                dto.setInitiator(userMap.get(event.getInitiator()));
            }

            Integer count = participantLimitMapConfirm.get(event.getId());

            dto.setConfirmedRequests(count != null ? count : 0);

            dto.setRating(ratings.getOrDefault(event.getId(), 0.0));
        }

        return eventShortDtos;
    }
}
