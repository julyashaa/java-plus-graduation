package ru.practicum.analyzer.service;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.grpc.stats.recommendation.InteractionsCountRequestProto;
import ru.practicum.grpc.stats.recommendation.RecommendedEventProto;
import ru.practicum.grpc.stats.recommendation.SimilarEventsRequestProto;
import ru.practicum.grpc.stats.recommendation.UserPredictionsRequestProto;

import java.util.List;

public interface AnalyzerService {

    void process(UserActionAvro action);

    void process(EventSimilarityAvro similarity);

    List<RecommendedEventProto> getRecommendationsForUser(
            UserPredictionsRequestProto request
    );

    List<RecommendedEventProto> getSimilarEvents(
            SimilarEventsRequestProto request
    );

    List<RecommendedEventProto> getInteractionsCount(
            InteractionsCountRequestProto request
    );
}