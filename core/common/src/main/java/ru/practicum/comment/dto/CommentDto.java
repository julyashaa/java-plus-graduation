package ru.practicum.comment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.user.UserShortDto;

import java.time.LocalDateTime;

import static ru.practicum.constants.DatePatternConstant.DATE_TIME_PATTERN;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {

    private Long id;

    private Long eventId;

    private String text;

    private UserShortDto author;

    @JsonFormat(pattern = DATE_TIME_PATTERN)
    private LocalDateTime createdOn;

    @JsonFormat(pattern = DATE_TIME_PATTERN)
    private LocalDateTime editedOn;
}