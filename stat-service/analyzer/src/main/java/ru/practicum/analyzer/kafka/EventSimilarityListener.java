package ru.practicum.analyzer.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.analyzer.service.AnalyzerServiceImpl;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSimilarityListener {

    private final AnalyzerServiceImpl analyzerService;

    @KafkaListener(
            topics = "${analyzer.kafka.topic.events-similarity}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "eventSimilarityKafkaListenerContainerFactory"
    )
    public void listen(EventSimilarityAvro similarity) {
        log.info("Analyzer received similarity: eventA={}, eventB={}, score={}",
                similarity.getEventA(), similarity.getEventB(), similarity.getScore());

        analyzerService.process(similarity);
    }
}