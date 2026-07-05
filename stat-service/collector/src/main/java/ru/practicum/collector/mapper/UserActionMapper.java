package ru.practicum.collector.mapper;

import com.google.protobuf.Timestamp;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.grpc.stats.action.ActionTypeProto;
import ru.practicum.grpc.stats.action.UserActionProto;

import java.time.Instant;

@Mapper(componentModel = "spring")
public interface UserActionMapper {

    @Mapping(
            target = "actionType",
            source = "actionType",
            qualifiedByName = "mapActionType"
    )
    @Mapping(
            target = "timestamp",
            source = "timestamp",
            qualifiedByName = "mapTimestamp"
    )
    UserActionAvro toAvro(UserActionProto proto);

    @Named("mapActionType")
    default ActionTypeAvro mapActionType(ActionTypeProto type) {
        return switch (type) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            case UNRECOGNIZED ->
                    throw new IllegalArgumentException("Unknown action type");
        };
    }

    @Named("mapTimestamp")
    default Instant mapTimestamp(Timestamp timestamp) {
        return Instant.ofEpochSecond(
                timestamp.getSeconds(),
                timestamp.getNanos()
        );
    }
}