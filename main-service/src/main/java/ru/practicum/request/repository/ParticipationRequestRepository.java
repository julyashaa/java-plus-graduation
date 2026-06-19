package ru.practicum.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.request.model.RequestStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    List<ParticipationRequest> findByRequester(Long requester);

    List<ParticipationRequest> findByEvent(Long event);

    Optional<ParticipationRequest> findByIdAndRequester(Long id, Long requester);

    boolean existsByRequesterAndEvent(Long requester, Long event);

    long countByEventAndStatus(Long event, RequestStatus status);

    List<ParticipationRequest> findByEventAndIdIn(Long event, Collection<Long> ids);

    List<ParticipationRequest> findByEventAndStatus(Long event, RequestStatus status);

    @Query("SELECT r.event, COUNT(r) FROM ParticipationRequest r WHERE r.event IN :eventIds AND r.status = :status " +
            "GROUP BY r.event")
    List<Object[]> countRequestsByEventIdsAndStatus(@Param("eventIds") List<Long> eventIds,
                                                    @Param("status") RequestStatus status);
}
