package ru.practicum.comment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.comment.dto.GetCommentsDtoParams;
import ru.practicum.comment.dto.ShortCommentDto;
import ru.practicum.comment.service.CommentService;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class PublicCommentController {

    private final CommentService commentService;

    @GetMapping("/comments/{commentId}")
    public ShortCommentDto getComment(@PathVariable Long commentId) {
        log.info("Запрос на получение комментария: GET /comments/{}", commentId);
        return commentService.getComment(commentId);
    }

    @GetMapping("/events/{eventId}/comments")
    public List<ShortCommentDto> getEventComments(@PathVariable Long eventId,
                                                  @Valid GetCommentsDtoParams params) {
        log.info("Запрос на получение комментариев ивента: GET /events/{}/comments?from={}&size={}",
                eventId, params.getFrom(), params.getSize());
        return commentService.getEventComments(eventId, params);
    }
}