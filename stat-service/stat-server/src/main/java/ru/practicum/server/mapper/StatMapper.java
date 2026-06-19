package ru.practicum.server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.server.model.EndpointHit;

import static ru.practicum.constants.DatePatternConstant.DATE_TIME_PATTERN;

@Mapper(componentModel = "spring")
public interface StatMapper {

    @Mapping(target = "timestamp", source = "timestamp", dateFormat = DATE_TIME_PATTERN)
    EndpointHit toEntity(EndpointHitDto dto);
}