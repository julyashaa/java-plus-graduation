package ru.practicum.aggregator.service;

import ru.practicum.ewm.stats.avro.ActionTypeAvro;

public final class ActionWeightResolver {

    private ActionWeightResolver() {
    }

    public static double resolve(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }
}