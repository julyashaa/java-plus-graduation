package ru.practicum.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.collector.mapper.UserActionMapper;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.grpc.stats.action.UserActionProto;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActionServiceImpl implements UserActionService {

    private final UserActionMapper mapper;
    private final KafkaTemplate<String, UserActionAvro> kafkaTemplate;

    @Value("${collector.kafka.topic.user-actions}")
    private String userActionsTopic;

    @Override
    public void collect(UserActionProto action) {
        UserActionAvro avro = mapper.toAvro(action);

        kafkaTemplate.send(
                userActionsTopic,
                String.valueOf(avro.getUserId()),
                avro
        );

        log.info("User action sent to Kafka: userId={}, eventId={}, actionType={}",
                avro.getUserId(), avro.getEventId(), avro.getActionType());
    }
}