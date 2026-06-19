package ru.practicum.compilation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.GetCompilationsDtoParams;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.compilation.mapper.CompilationMapper;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.repository.CompilationRepository;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {

    private final EventMapper eventMapper;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;
    private final CompilationRepository compilationRepository;

    @Override
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        String title = newCompilationDto.getTitle();
        log.info("Создание новой подборки: {}", title);

        throwIfTitleExist(title);

        Compilation compilation = compilationMapper.toEntity(newCompilationDto);

        Set<Long> eventsId = newCompilationDto.getEvents();
        if (eventsId != null && !eventsId.isEmpty()) {
            List<Event> events = eventRepository.findAllById(eventsId);
            compilation.setEvents(new HashSet<>(events));
        }

        Compilation savedCompilation = compilationRepository.save(compilation);
        log.info("Подборка создана с ID: {}", savedCompilation.getId());

        CompilationDto result = compilationMapper.toDto(savedCompilation);
        result.setEvents(mapEventsToEventsShortDto(savedCompilation.getEvents()));

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public CompilationDto getCompilationById(Long compId) {
        log.info("Получение подборки с ID: {}", compId);

        Compilation compilation = getCompilationOrElseThrow(compId);

        CompilationDto result = compilationMapper.toDto(compilation);
        result.setEvents(mapEventsToEventsShortDto(compilation.getEvents()));

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompilationDto> getCompilations(GetCompilationsDtoParams params) {
        log.info("Получение подборок с параметрами {}", params);

        Pageable pageable = PageRequest.of(params.getFrom() / params.getSize(), params.getSize());

        boolean pinned = params.isPinned();
        List<Compilation> compilations = pinned ?
                compilationRepository.findAllByPinned(pinned, pageable).getContent() :
                compilationRepository.findAll(pageable).getContent();

        return compilations.stream()
                .map(compilation -> {
                    CompilationDto result = compilationMapper.toDto(compilation);
                    result.setEvents(mapEventsToEventsShortDto(compilation.getEvents()));
                    return result;
                }).toList();
    }

    @Override
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateRequest) {
        log.info("Обновление подборки с ID: {}", compId);

        Compilation compilation = getCompilationOrElseThrow(compId);

        String title = updateRequest.getTitle();
        if (title != null && !title.isEmpty()) {
            compilation.setTitle(updateRequest.getTitle());
        }

        Boolean pinned = updateRequest.getPinned();
        if (pinned != null) {
            compilation.setPinned(pinned);
        }

        Set<Long> requestEvents = updateRequest.getEvents();
        if (requestEvents != null) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(requestEvents));
            compilation.setEvents(events);
        }

        Compilation updatedCompilation = compilationRepository.save(compilation);
        log.info("Подборка с ID {} обновлена", compId);

        CompilationDto result = compilationMapper.toDto(updatedCompilation);
        result.setEvents(mapEventsToEventsShortDto(updatedCompilation.getEvents()));

        return result;
    }

    @Override
    public void deleteCompilation(Long compId) {
        log.info("Удаление подборки с ID: {}", compId);

        getCompilationOrElseThrow(compId);

        compilationRepository.deleteById(compId);
        log.info("Подборка с ID {} удалена", compId);
    }

    private void throwIfTitleExist(String title) {
        if (compilationRepository.existsByTitle(title)) {
            throw new ConflictException("Подборка с названием '" + title + "' уже существует");
        }
    }

    private Set<EventShortDto> mapEventsToEventsShortDto(Set<Event> events) {
        if (events == null || events.isEmpty()) {
            return Set.of();
        }
        return events.stream()
                .map(eventMapper::toShortDto)
                .collect(Collectors.toSet());
    }

    private Compilation getCompilationOrElseThrow(Long id) {
        return compilationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Compilation with id " + id + " not found"));
    }
}