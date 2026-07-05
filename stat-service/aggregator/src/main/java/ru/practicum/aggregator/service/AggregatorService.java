package ru.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.aggregator.model.EventPair;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AggregatorService {

    private final KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;

    @Value("${aggregator.kafka.topic.events-similarity}")
    private String eventsSimilarityTopic;

    private final Map<Long, Map<Long, Double>> eventUserWeights = new HashMap<>();
    private final Map<Long, Double> eventWeightSums = new HashMap<>();
    private final Map<EventPair, Double> minWeightSums = new HashMap<>();

    public void process(UserActionAvro action) {
        long eventId = action.getEventId();
        long userId = action.getUserId();

        double oldWeight = getUserEventWeight(eventId, userId);
        double newWeight = ActionWeightResolver.resolve(action.getActionType());

        if (newWeight <= oldWeight) {
            return;
        }

        updateSimilarities(action, oldWeight, newWeight);
        updateUserWeight(eventId, userId, oldWeight, newWeight);
    }

    private void updateSimilarities(
            UserActionAvro action,
            double oldWeight,
            double newWeight
    ) {
        long eventA = action.getEventId();
        long userId = action.getUserId();

        for (Long eventB : eventUserWeights.keySet()) {
            if (eventA == eventB) {
                continue;
            }

            double weightB = getUserEventWeight(eventB, userId);

            if (weightB == 0.0) {
                continue;
            }

            EventPair pair = EventPair.of(eventA, eventB);

            double oldMin = Math.min(oldWeight, weightB);
            double newMin = Math.min(newWeight, weightB);
            double updatedMinSum = minWeightSums.getOrDefault(pair, 0.0) + newMin - oldMin;

            minWeightSums.put(pair, updatedMinSum);

            double updatedEventASum =
                    eventWeightSums.getOrDefault(eventA, 0.0) + newWeight - oldWeight;
            double eventBSum = eventWeightSums.getOrDefault(eventB, 0.0);

            double score = updatedMinSum / Math.sqrt(updatedEventASum * eventBSum);

            EventSimilarityAvro similarity = EventSimilarityAvro.newBuilder()
                    .setEventA(pair.first())
                    .setEventB(pair.second())
                    .setScore(score)
                    .setTimestamp(action.getTimestamp() == null ? Instant.now() : action.getTimestamp())
                    .build();

            kafkaTemplate.send(
                    eventsSimilarityTopic,
                    pair.first() + ":" + pair.second(),
                    similarity
            );
        }
    }

    private void updateUserWeight(
            long eventId,
            long userId,
            double oldWeight,
            double newWeight
    ) {
        eventUserWeights
                .computeIfAbsent(eventId, event -> new HashMap<>())
                .put(userId, newWeight);

        eventWeightSums.merge(eventId, newWeight - oldWeight, Double::sum);
    }

    private double getUserEventWeight(long eventId, long userId) {
        return eventUserWeights
                .getOrDefault(eventId, Map.of())
                .getOrDefault(userId, 0.0);
    }
}