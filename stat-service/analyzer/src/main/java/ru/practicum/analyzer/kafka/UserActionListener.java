package ru.practicum.analyzer.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.analyzer.service.AnalyzerServiceImpl;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionListener {

    private final AnalyzerServiceImpl analyzerService;

    @KafkaListener(
            topics = "${analyzer.kafka.topic.user-actions}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "userActionKafkaListenerContainerFactory"
    )
    public void listen(UserActionAvro action) {
        log.info("Analyzer received user action: userId={}, eventId={}, actionType={}",
                action.getUserId(), action.getEventId(), action.getActionType());

        analyzerService.process(action);
    }
}