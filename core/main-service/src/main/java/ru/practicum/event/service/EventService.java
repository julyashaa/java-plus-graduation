package ru.practicum.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.service.CategoryService;
import ru.practicum.client.ClientForStat;
import ru.practicum.client.RestStatClient;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.event.dto.*;
import ru.practicum.event.enums.EventState;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.service.RequestService;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserShortDto;
import ru.practicum.user.service.UserService;

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
    private final UserService userService;
    private final CategoryService categoryService;
    private final RequestService requestService;
    private final ClientForStat client;
    private final RestStatClient restStatClient;

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
        event.setViews(0L);
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
        // Конвертация Event в EventShortDto
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

        // Проверка, что пользователь — инициатор
        if (!event.getInitiator().equals(userId)) {
            throw new ConflictException("Вы не являетесь инициатором этого события");
        }

        // Проверка статуса события
        if (!(event.getState().equals(EventState.CANCELED) || event.getState().equals(EventState.PENDING))) {
            throw new ConflictException("Изменять можно только ожидающие или отменённые события");
        }

        // Проверка даты события
        LocalDateTime newEventDate = null;
        if (request.getEventDate() != null) {
            newEventDate = LocalDateTime.parse(request.getEventDate(), DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
            if (newEventDate.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new BadRequestException("Дата и время события должны быть не раньше чем через 2 часа");
            }
            event.setEventDate(newEventDate);
        }

        // Обновление данных
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

        // Обработка изменения статуса
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

        // Обновление категории при необходимости
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
            throw new IllegalArgumentException("Некорректный формат даты");
        }

        if (start != null && start.isAfter(end)) {
            throw new BadRequestException("Конец интервала не может быть раньше начала");
        }

        // Вычисляем номер страницы
        int page = from / size;
        Pageable pageable = PageRequest.of(page, size);

        // Передавайте null, если списки пустые или null
        List<Long> usersParam = null;
        if (users != null && !(users.size() == 1 && users.getFirst() == 0L)) {
            usersParam = users;
        }

        // Обработка categories
        List<Long> categoriesParam = null;
        if (categories != null && !(categories.size() == 1 && categories.getFirst() == 0L)) {
            categoriesParam = categories;
        }

        List<EventState> statesParam = (states != null && !states.isEmpty()) ? convertStringsToEventStates(states) : null;

        List<Event> events = eventRepository.findEventsWithFilters(
                statesParam,
                usersParam,
                categoriesParam,
                pageable
        );

        if (start != null) {
            LocalDateTime finalStart = start;
            events = events.stream()
                    .filter(e -> e.getEventDate().isAfter(finalStart) || e.getEventDate().isEqual(finalStart))
                    .collect(Collectors.toList());
        }
        if (end != null) {
            LocalDateTime finalEnd = end;
            events = events.stream()
                    .filter(e -> e.getEventDate().isBefore(finalEnd) || e.getEventDate().isEqual(finalEnd))
                    .collect(Collectors.toList());
        }

        log.info("Обработка DTO завершена. Количество DTO: {}", events.size());
        return fillingFieldsInEventFullDtos(events);
    }

    @Transactional
    public EventFullDto updateEvent(Long eventId, UpdateEventAdminRequest request) {
        log.info("Начинается обновление события admin {}", request);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        String action = request.getStateAction();
        LocalDateTime now = LocalDateTime.now();

        // Внутренние проверки перед выполнением действия
        if (action != null) {
            switch (action) {
                case "PUBLISH_EVENT":
                    validatePublish(event, request, now);
                    // Обновляем статус и дату публикации
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

        // Обновление основных полей, если они заданы
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
        Optional<Event> optionalEvent = eventRepository.findById(id);
        if (optionalEvent.isEmpty() || !optionalEvent.get().getState().equals(EventState.PUBLISHED)) {
            throw new NotFoundException("Событие с ID=" + id + " не найдено");
        }
        Event event = optionalEvent.get();
        List<String> uris = new ArrayList<>(Collections.emptyList());
        uris.add("/events/" + event.getId());
        try {
            List<ViewStatsDto> viewStatsDtoMains = restStatClient.getAllStats(uris, false);
            if (!viewStatsDtoMains.isEmpty()) {
                event.setViews(viewStatsDtoMains.getFirst().getHits());
            }
        } catch (Exception e) {
            log.error("Ошбка запуска сервиса статистики", e);
        }
        log.info("Найдено событие: {}", event);
        return fillingFieldsInEventFullDto(event);
    }

    private void validatePublish(Event event, UpdateEventAdminRequest request, LocalDateTime now) {
        if (EventState.PUBLISHED.equals(event.getState())) {
            throw new ConflictException("Невозможно опубликовать — событие уже опубликовано");
        }

        if (!EventState.PENDING.equals(event.getState())) {
            throw new ConflictException("Можно публиковать только события в состоянии ожидания");
        }

        // Проверка, что дата события не раньше, чем через 1 час от текущего времени
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
            // если дата не передана в запрос, берем старую, но проверять всё равно
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
            return null; // Или возвращайте пустой список, в зависимости от логики
        }

        return statesStrings.stream()
                .map(String::toUpperCase) // на всякий случай делаем верхним регистром для совпадения с enum
                .map(s -> {
                    try {
                        return EventState.valueOf(s);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(e -> e != null) // убираем невалидные значения
                .collect(Collectors.toList());
    }

    private EventDto fillingFieldsInEventDto(Event event) {
        UserDto users = userService.getUser(event.getInitiator());
        CategoryDto categoryDto = categoryService.getById(event.getCategory());

        EventDto eventDto = eventMapper.toDto(event);
        eventDto.setInitiator(new UserShortDto(users.getId(), users.getName()));
        eventDto.setCategory(categoryDto);
        return eventDto;
    }

    private EventFullDto fillingFieldsInEventFullDto(Event event) {
        UserDto users = userService.getUser(event.getInitiator());
        CategoryDto categoryDto = categoryService.getById(event.getCategory());

        EventFullDto eventDto = eventMapper.toFullDto(event);
        eventDto.setInitiator(new UserShortDto(users.getId(), users.getName()));
        eventDto.setCategory(categoryDto);
        Integer confirmRequests = requestService.getEventParticipantsWithConfirm(event.getInitiator(), event.getId());
        eventDto.setConfirmedRequests(confirmRequests != null ? confirmRequests : 0);
        return eventDto;
    }

    private List<EventFullDto> fillingFieldsInEventFullDtos(List<Event> events) {
        List<UserShortDto> users = userService.getAllUserShort();
        List<CategoryDto> categoryDtos = categoryService.getAll();

        Map<Long, CategoryDto> categoryMap = categoryDtos.stream()
                .collect(Collectors.toMap(CategoryDto::getId, Function.identity()));

        Map<Long, UserShortDto> userMap = users.stream()
                .collect(Collectors.toMap(UserShortDto::getId, Function.identity()));

        List<EventFullDto> eventFullDtos = eventMapper.toFullDtos(events);

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();

        HashMap<Long, Integer> participantLimitMapConfirm = requestService.getAllEventParticipiants(eventIds,
                RequestStatus.CONFIRMED);

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
        }

        return eventFullDtos;
    }

    private List<EventShortDto> fillingFieldsInEventShortDtos(List<Event> events) {
        List<UserShortDto> users = userService.getAllUserShort();
        List<CategoryDto> categoryDtos = categoryService.getAll();

        Map<Long, CategoryDto> categoryMap = categoryDtos.stream()
                .collect(Collectors.toMap(CategoryDto::getId, Function.identity()));

        Map<Long, UserShortDto> userMap = users.stream()
                .collect(Collectors.toMap(UserShortDto::getId, Function.identity()));

        List<EventShortDto> eventShortDtos = eventMapper.toShortDtos(events);

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();

        HashMap<Long, Integer> participantLimitMapConfirm = requestService.getAllEventParticipiants(eventIds,
                RequestStatus.CONFIRMED);

        for (int i = 0; i < events.size(); i++) {
            Event event = events.get(i);
            EventShortDto dto = eventShortDtos.get(i);

            if (event.getCategory() != null) {
                dto.setCategory(categoryMap.get(event.getCategory()));
            }
            if (event.getInitiator() != null) {
                dto.setInitiator(userMap.get(event.getInitiator()));
            }
            dto.setConfirmedRequests(participantLimitMapConfirm.get(event.getId()));
        }

        return eventShortDtos;
    }
}
