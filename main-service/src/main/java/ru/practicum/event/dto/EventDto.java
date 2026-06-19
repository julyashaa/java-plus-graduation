package ru.practicum.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.event.enums.EventState;
import ru.practicum.event.model.Location;
import ru.practicum.user.dto.UserShortDto;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDto {
    private Long id;

    private String title;

    private String annotation;

    private CategoryDto category;

    private Boolean paid;

    private LocalDateTime eventDate;

    private UserShortDto initiator;

    private String description;

    private LocalDateTime createdOn;

    private Integer participantLimit;

    private Boolean requestModeration;

    private Location location;

    private EventState state;
}
