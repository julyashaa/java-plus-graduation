package ru.practicum.event.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.event.enums.EventState;
import ru.practicum.event.model.Event;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByInitiator(Long initiator, Pageable pageable);

    // Метод с фильтрацией по спискам
    @Query("SELECT e FROM Event e WHERE " +
            "(:states IS NULL OR e.state IN :states) AND " +
            "(:users IS NULL OR e.initiator IN :users) AND " +
            "(:categories IS NULL OR e.category IN :categories)")
    List<Event> findEventsWithFilters(
            @Param("states") List<EventState> states,
            @Param("users") List<Long> users,
            @Param("categories") List<Long> categories,
            Pageable pageable
    );
}
