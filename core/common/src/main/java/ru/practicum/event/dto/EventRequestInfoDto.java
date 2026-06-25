package ru.practicum.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.event.enums.EventState;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestInfoDto {
    private Long id;
    private Long initiator;
    private EventState state;
    private Integer participantLimit;
    private Boolean requestModeration;
}