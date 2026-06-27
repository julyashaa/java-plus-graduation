package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.analyzer.model.UserActionEntity;

import java.util.List;
import java.util.Optional;

public interface UserActionRepository extends JpaRepository<UserActionEntity, Long> {

    Optional<UserActionEntity> findByUserIdAndEventId(Long userId, Long eventId);

    List<UserActionEntity> findByUserIdOrderByTimestampDesc(Long userId);

    List<UserActionEntity> findByEventId(Long eventId);

    @Query("SELECT ua FROM UserActionEntity ua WHERE ua.eventId IN :eventIds")
    List<UserActionEntity> findByEventIds(@Param("eventIds") List<Long> eventIds);
}