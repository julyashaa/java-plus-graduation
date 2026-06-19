package ru.practicum.event.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.event.dto.EventDto;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.Location;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EventMapper {
    @Mapping(target = "id", ignore = true)
    Event toEntity(NewEventDto dto);

    @Mapping(source = "requestModeration", target = "requestModeration")
    @Mapping(source = "createdOn", target = "createdOn")
    @Mapping(source = "state", target = "state")
    @Mapping(source = "views", target = "views")
    @Mapping(target = "initiator", ignore = true)
    @Mapping(target = "category", ignore = true)
    EventFullDto toFullDto(Event event);

    @Mapping(source = "state", target = "state")
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "initiator", ignore = true)
    EventDto toDto(Event event);

    @Mapping(target = "category", ignore = true)
    @Mapping(target = "initiator", ignore = true)
    EventShortDto toShortDto(Event event);

    List<EventShortDto> toShortDtos(List<Event> events);

    List<EventFullDto> toFullDtos(List<Event> events);

    // Мапперы для Location внутри интерфейса
    default Location toLocation(ru.practicum.event.model.Location locationDto) {
        if (locationDto == null) {
            return null;
        }
        return new Location(locationDto.getLat(), locationDto.getLon());
    }
}