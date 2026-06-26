package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.analyzer.mapper.EventSimilarityMapper;
import ru.practicum.analyzer.mapper.UserActionMapper;
import ru.practicum.analyzer.model.EventSimilarityEntity;
import ru.practicum.analyzer.model.UserActionEntity;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.grpc.stats.recommendation.InteractionsCountRequestProto;
import ru.practicum.grpc.stats.recommendation.RecommendedEventProto;
import ru.practicum.grpc.stats.recommendation.SimilarEventsRequestProto;
import ru.practicum.grpc.stats.recommendation.UserPredictionsRequestProto;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyzerServiceImpl implements AnalyzerService {

    private final UserActionRepository userActionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;
    private final UserActionMapper userActionMapper;
    private final EventSimilarityMapper eventSimilarityMapper;

    @Override
    public void process(UserActionAvro action) {
        double weight = ActionWeightResolver.resolve(action.getActionType());

        UserActionEntity entity = userActionRepository
                .findByUserIdAndEventId(action.getUserId(), action.getEventId())
                .orElseGet(() -> {
                    UserActionEntity newEntity = userActionMapper.toEntity(action);
                    newEntity.setWeight(weight);
                    return newEntity;
                });

        if (weight > entity.getWeight()) {
            entity.setWeight(weight);
        }

        entity.setTimestamp(action.getTimestamp());
        userActionRepository.save(entity);
    }

    @Override
    public void process(EventSimilarityAvro similarity) {
        long eventA = Math.min(similarity.getEventA(), similarity.getEventB());
        long eventB = Math.max(similarity.getEventA(), similarity.getEventB());

        EventSimilarityEntity entity = eventSimilarityRepository
                .findByEventAAndEventB(eventA, eventB)
                .orElseGet(() -> {
                    EventSimilarityEntity newEntity =
                            eventSimilarityMapper.toEntity(similarity);
                    newEntity.setEventA(eventA);
                    newEntity.setEventB(eventB);
                    return newEntity;
                });

        entity.setScore(similarity.getScore());
        entity.setTimestamp(similarity.getTimestamp());

        eventSimilarityRepository.save(entity);
    }

    @Override
    public List<RecommendedEventProto> getRecommendationsForUser(
            UserPredictionsRequestProto request
    ) {
        long userId = request.getUserId();
        int maxResults = request.getMaxResults();

        List<UserActionEntity> recentInteractions = userActionRepository
                .findByUserIdOrderByTimestampDesc(userId)
                .stream()
                .limit(maxResults)
                .toList();

        if (recentInteractions.isEmpty()) {
            return List.of();
        }

        Set<Long> interactedEventIds = recentInteractions
                .stream()
                .map(UserActionEntity::getEventId)
                .collect(Collectors.toSet());

        List<EventSimilarityEntity> candidateSimilarities = recentInteractions
                .stream()
                .flatMap(interaction -> eventSimilarityRepository
                        .findByEventAOrEventB(
                                interaction.getEventId(),
                                interaction.getEventId()
                        )
                        .stream())
                .filter(similarity -> {
                    long candidateEventId = getOtherEventId(
                            similarity,
                            interactedEventIds
                    );
                    return candidateEventId != 0
                            && !interactedEventIds.contains(candidateEventId);
                })
                .sorted(Comparator.comparingDouble(
                        EventSimilarityEntity::getScore
                ).reversed())
                .limit(maxResults)
                .toList();

        return candidateSimilarities
                .stream()
                .map(similarity -> {
                    long candidateEventId = getOtherEventId(
                            similarity,
                            interactedEventIds
                    );

                    return RecommendedEventProto.newBuilder()
                            .setEventId(candidateEventId)
                            .setScore(predictScore(
                                    candidateEventId,
                                    recentInteractions
                            ))
                            .build();
                })
                .filter(event -> event.getScore() > 0)
                .sorted(Comparator.comparingDouble(
                        RecommendedEventProto::getScore
                ).reversed())
                .limit(maxResults)
                .toList();
    }

    @Override
    public List<RecommendedEventProto> getSimilarEvents(
            SimilarEventsRequestProto request
    ) {
        long eventId = request.getEventId();
        long userId = request.getUserId();
        int maxResults = request.getMaxResults();

        Set<Long> interactedEventIds = userActionRepository
                .findByUserIdOrderByTimestampDesc(userId)
                .stream()
                .map(UserActionEntity::getEventId)
                .collect(Collectors.toSet());

        return eventSimilarityRepository
                .findByEventAOrEventB(eventId, eventId)
                .stream()
                .map(similarity -> {
                    long similarEventId = similarity.getEventA().equals(eventId)
                            ? similarity.getEventB()
                            : similarity.getEventA();

                    return RecommendedEventProto.newBuilder()
                            .setEventId(similarEventId)
                            .setScore(similarity.getScore())
                            .build();
                })
                .filter(event -> !interactedEventIds.contains(event.getEventId()))
                .sorted(Comparator.comparingDouble(
                        RecommendedEventProto::getScore
                ).reversed())
                .limit(maxResults)
                .toList();
    }

    @Override
    public List<RecommendedEventProto> getInteractionsCount(
            InteractionsCountRequestProto request
    ) {
        return request.getEventIdList()
                .stream()
                .map(eventId -> RecommendedEventProto.newBuilder()
                        .setEventId(eventId)
                        .setScore(userActionRepository.findByEventId(eventId)
                                .stream()
                                .mapToDouble(UserActionEntity::getWeight)
                                .sum())
                        .build())
                .toList();
    }

    private double predictScore(
            long targetEventId,
            List<UserActionEntity> userInteractions
    ) {
        Map<Long, UserActionEntity> interactionsByEventId = userInteractions
                .stream()
                .collect(Collectors.toMap(
                        UserActionEntity::getEventId,
                        Function.identity(),
                        (first, second) -> first
                ));

        double weightedSum = 0.0;
        double similaritySum = 0.0;

        for (UserActionEntity interaction : interactionsByEventId.values()) {
            double similarity = eventSimilarityRepository
                    .findByEventAAndEventB(
                            Math.min(targetEventId, interaction.getEventId()),
                            Math.max(targetEventId, interaction.getEventId())
                    )
                    .map(EventSimilarityEntity::getScore)
                    .orElse(0.0);

            if (similarity <= 0) {
                continue;
            }

            weightedSum += similarity * interaction.getWeight();
            similaritySum += similarity;
        }

        return similaritySum == 0.0 ? 0.0 : weightedSum / similaritySum;
    }

    private long getOtherEventId(
            EventSimilarityEntity similarity,
            Set<Long> eventIds
    ) {
        if (eventIds.contains(similarity.getEventA())) {
            return similarity.getEventB();
        }

        if (eventIds.contains(similarity.getEventB())) {
            return similarity.getEventA();
        }

        return 0;
    }
}