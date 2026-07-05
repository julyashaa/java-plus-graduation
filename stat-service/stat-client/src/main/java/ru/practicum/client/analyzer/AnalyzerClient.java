package ru.practicum.client.analyzer;

import ru.practicum.grpc.stats.recommendation.RecommendedEventProto;

import java.util.List;

public interface AnalyzerClient {

    List<RecommendedEventProto> getRecommendations(
            long userId,
            int maxResults
    );

    List<RecommendedEventProto> getSimilarEvents(
            long eventId,
            long userId,
            int maxResults
    );

    List<RecommendedEventProto> getInteractionsCount(
            List<Long> eventIds
    );
}