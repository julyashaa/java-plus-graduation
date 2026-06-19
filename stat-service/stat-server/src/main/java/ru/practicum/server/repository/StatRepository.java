package ru.practicum.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.server.model.EndpointHit;
import ru.practicum.server.repository.query.StatQueries;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StatRepository extends JpaRepository<EndpointHit, Long> {

    @Query(StatQueries.STATS)
    List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris);

    @Query(StatQueries.UNIQUE_STATS)
    List<ViewStatsDto> getUniqueStats(LocalDateTime start, LocalDateTime end, List<String> uris);

    @Query(StatQueries.ALL_STATS)
    List<ViewStatsDto> getUniqueAllStats(List<String> uris, Boolean unique);
}