package ru.practicum.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.server.exception.BadRequestException;
import ru.practicum.server.mapper.StatMapper;
import ru.practicum.server.model.EndpointHit;
import ru.practicum.server.repository.StatRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static ru.practicum.constants.DatePatternConstant.DATE_TIME_PATTERN;

@Service
@RequiredArgsConstructor
public class StatService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    private final StatMapper statMapper;
    private final StatRepository statRepository;

    public void saveHit(EndpointHitDto hitDto) {
        EndpointHit hit = statMapper.toEntity(hitDto);
        statRepository.save(hit);
    }

    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, Boolean unique) {
        LocalDateTime startTime = LocalDateTime.parse(start, FORMATTER);
        LocalDateTime endTime = LocalDateTime.parse(end, FORMATTER);

        if (startTime.isAfter(endTime)) {
            throw new BadRequestException("Конец интервала не может быть раньше начала");
        }
        if (unique) {
            return statRepository.getUniqueStats(startTime, endTime, uris);
        } else {
            return statRepository.getStats(startTime, endTime, uris);
        }
    }

    public List<ViewStatsDto> getAllStats(List<String> uris, Boolean unique) {
        return statRepository.getUniqueAllStats(uris, unique);
    }
}