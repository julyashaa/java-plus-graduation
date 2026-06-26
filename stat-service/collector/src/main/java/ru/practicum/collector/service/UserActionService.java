package ru.practicum.collector.service;

import ru.practicum.grpc.stats.action.UserActionProto;

public interface UserActionService {

    void collect(UserActionProto action);
}