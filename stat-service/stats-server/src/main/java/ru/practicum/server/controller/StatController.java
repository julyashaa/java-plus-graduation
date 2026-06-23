package ru.practicum.server.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.server.service.StatService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class StatController {
    private final StatService statService;

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void hit(@Valid @RequestBody EndpointHitDto hitDto) {
        log.info("Запрос на сохранение статистики: app={}, uri={}", hitDto.getApp(), hitDto.getUri());
        statService.saveHit(hitDto);
    }

    @GetMapping("/stats")
    public List<ViewStatsDto> getStats(@RequestParam String start,
                                       @RequestParam String end,
                                       @RequestParam(required = false) List<String> uris,
                                       @RequestParam(defaultValue = "false") Boolean unique) {
        log.info("Запрос на получение статистики: start={}, end={}, uris={}, unique={}", start, end, uris, unique);
        return statService.getStats(start, end, uris, unique);
    }

    @GetMapping("/stats/all")
    public List<ViewStatsDto> getUniqStats(@RequestParam(required = false) List<String> uris, Boolean unique) {
        log.info("Запрос на получение статистики: uris={}", uris);
        return statService.getAllStats(uris, unique);
    }
}