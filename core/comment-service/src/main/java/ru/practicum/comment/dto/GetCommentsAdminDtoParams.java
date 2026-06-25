package ru.practicum.comment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.comment.validation.ValidDateRange;

import java.time.LocalDateTime;
import java.util.List;

import static ru.practicum.constants.DatePatternConstant.DATE_TIME_PATTERN;

@Data
@Builder
@ValidDateRange
@NoArgsConstructor
@AllArgsConstructor
public class GetCommentsAdminDtoParams {

    private List<Long> ids;

    private Long userId;

    private Long eventId;

    @PastOrPresent
    @JsonFormat(pattern = DATE_TIME_PATTERN)
    private LocalDateTime rangeStart;

    @JsonFormat(pattern = DATE_TIME_PATTERN)
    private LocalDateTime rangeEnd;

    @Builder.Default
    @Min(0)
    private Integer from = 0;

    @Builder.Default
    @Min(1)
    private Integer size = 10;
}