package ru.practicum.analyzer.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.analyzer.model.UserActionEntity;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Mapper(componentModel = "spring")
public interface UserActionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "weight", ignore = true)
    UserActionEntity toEntity(UserActionAvro avro);
}