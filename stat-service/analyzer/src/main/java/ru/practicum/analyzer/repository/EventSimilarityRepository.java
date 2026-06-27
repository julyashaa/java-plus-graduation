package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.analyzer.model.EventSimilarityEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarityEntity, Long> {

    Optional<EventSimilarityEntity> findByEventAAndEventB(Long eventA, Long eventB);

    List<EventSimilarityEntity> findByEventAOrEventB(Long eventA, Long eventB);

    List<EventSimilarityEntity> findByEventAInOrEventBIn(Set<Long> eventA, Set<Long> eventB);
}