package ru.practicum.client;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import static ru.practicum.constants.DatePatternConstant.DATE_TIME_PATTERN;

@Component
@RequiredArgsConstructor
public class RestStatClient implements StatClient {

    private final RestClient.Builder restClientBuilder;
    private final DiscoveryClient discoveryClient;

    private static final String STATS_SERVER = "stats-server";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    @Override
    public void hit(EndpointHitDto endpointHitDto) {
        try {
            getRestClient().post()
                    .uri("/hit")
                    .body(endpointHitDto)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка сохранения статистики", e);
        }
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        try {
            ResponseEntity<List<ViewStatsDto>> response = getRestClient().get()
                    .uri(uriBuilder -> {
                        UriBuilder builder = uriBuilder.path("/stats")
                                .queryParam("start", start.format(FORMATTER))
                                .queryParam("end", end.format(FORMATTER));

                        if (uris != null && !uris.isEmpty()) {
                            builder.queryParam("uris", uris);
                        }
                        if (unique != null) {
                            builder.queryParam("unique", unique);
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<>() {});

            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка получения статистики", e);
        }
    }

    @Override
    public List<ViewStatsDto> getAllStats(List<String> uris, Boolean unique) {
        try {
            ResponseEntity<List<ViewStatsDto>> response = getRestClient().get()
                    .uri(uriBuilder -> {
                        UriBuilder builder = uriBuilder.path("/stats/all");

                        if (uris != null && !uris.isEmpty()) {
                            builder.queryParam("uris", uris);
                        }
                        if (unique != null) {
                            builder.queryParam("unique", unique);
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<>() {});

            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка получения всей статистики", e);
        }
    }

    private RestClient getRestClient() {
        List<ServiceInstance> instances = discoveryClient.getInstances(STATS_SERVER);

        if (instances.isEmpty()) {
            throw new RuntimeException("Сервис статистики не найден в Eureka");
        }

        return restClientBuilder
                .baseUrl(instances.getFirst().getUri().toString())
                .build();
    }
}