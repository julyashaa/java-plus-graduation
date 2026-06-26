package ru.practicum.client.collector;

import ru.practicum.grpc.stats.action.ActionTypeProto;

public interface CollectorClient {

    void collectUserAction(
            long userId,
            long eventId,
            ActionTypeProto actionType
    );
}