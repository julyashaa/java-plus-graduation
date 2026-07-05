package ru.practicum.aggregator.model;

public record EventPair(long first, long second) {

    public static EventPair of(long eventA, long eventB) {
        return eventA < eventB
                ? new EventPair(eventA, eventB)
                : new EventPair(eventB, eventA);
    }
}