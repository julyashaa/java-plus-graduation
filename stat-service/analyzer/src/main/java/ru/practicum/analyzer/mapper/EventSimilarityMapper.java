package ru.practicum.analyzer.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.analyzer.model.EventSimilarityEntity;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@Mapper(componentModel = "spring")
public interface EventSimilarityMapper {

    @Mapping(target = "id", ignore = true)
    EventSimilarityEntity toEntity(
            EventSimilarityAvro avro
    );
}