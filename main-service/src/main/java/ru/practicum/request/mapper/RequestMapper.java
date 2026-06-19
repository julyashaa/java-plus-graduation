package ru.practicum.request.mapper;

import org.mapstruct.Mapper;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.model.ParticipationRequest;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RequestMapper {
    ParticipationRequestDto toDto(ParticipationRequest request);

    List<ParticipationRequestDto> toDtos(List<ParticipationRequest> requests);
}
