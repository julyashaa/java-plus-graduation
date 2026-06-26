package ru.practicum.aggregator.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.aggregator.service.AggregatorService;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionListener {

    private final AggregatorService aggregatorService;

    @KafkaListener(
            topics = "${aggregator.kafka.topic.user-actions}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void listen(UserActionAvro action) {
        log.info("Received user action: userId={}, eventId={}, actionType={}",
                action.getUserId(), action.getEventId(), action.getActionType());

        aggregatorService.process(action);
    }
}