package ru.practicum.client.analyzer;

import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.grpc.stats.dashboard.RecommendationsControllerGrpc;
import ru.practicum.grpc.stats.recommendation.InteractionsCountRequestProto;
import ru.practicum.grpc.stats.recommendation.RecommendedEventProto;
import ru.practicum.grpc.stats.recommendation.SimilarEventsRequestProto;
import ru.practicum.grpc.stats.recommendation.UserPredictionsRequestProto;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

@Component
@RequiredArgsConstructor
public class GrpcAnalyzerClient implements AnalyzerClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub client;

    @Override
    public List<RecommendedEventProto> getRecommendations(long userId, int maxResults) {
        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();

        return toList(client.getRecommendationsForUser(request));
    }

    @Override
    public List<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();

        return toList(client.getSimilarEvents(request));
    }

    @Override
    public List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addAllEventId(eventIds)
                .build();

        return toList(client.getInteractionsCount(request));
    }

    private List<RecommendedEventProto> toList(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(iterator, 0),
                        false
                )
                .toList();
    }
}