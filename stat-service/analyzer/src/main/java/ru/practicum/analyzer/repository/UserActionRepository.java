package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.analyzer.model.UserActionEntity;

import java.util.List;
import java.util.Optional;

public interface UserActionRepository extends JpaRepository<UserActionEntity, Long> {

    Optional<UserActionEntity> findByUserIdAndEventId(Long userId, Long eventId);

    List<UserActionEntity> findByUserIdOrderByTimestampDesc(Long userId);

    List<UserActionEntity> findByEventId(Long eventId);
}