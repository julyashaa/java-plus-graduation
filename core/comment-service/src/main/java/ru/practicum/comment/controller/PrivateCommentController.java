package ru.practicum.comment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.GetCommentsDtoParams;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.UpdateCommentDto;
import ru.practicum.comment.service.CommentService;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/comments")
public class PrivateCommentController {

    private final CommentService commentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto createComment(@PathVariable Long userId,
                                    @RequestBody @Valid NewCommentDto newCommentDto) {
        log.info("Запрос на создание комментария: POST /users/{}/comments", userId);
        return commentService.createComment(userId, newCommentDto);
    }

    @GetMapping("/{commentId}")
    public CommentDto getComment(@PathVariable Long userId,
                                 @PathVariable Long commentId) {
        log.info("Запрос на получение комментария: GET /users/{}/comments/{}", userId, commentId);
        return commentService.getCommentByUser(userId, commentId);
    }

    @GetMapping
    public List<CommentDto> getUserComments(@PathVariable Long userId,
                                            @Valid GetCommentsDtoParams params) {
        log.info("Запрос на получение комментариев пользователя: GET /users/{}/comments?from={}&size={}",
                userId, params.getFrom(), params.getSize());
        return commentService.getUserComments(userId, params);
    }

    @PatchMapping("/{commentId}")
    public CommentDto updateComment(@PathVariable Long userId,
                                    @PathVariable Long commentId,
                                    @RequestBody @Valid UpdateCommentDto updateCommentDto) {
        log.info("Запрос на обновление комментария : PATCH /users/{}/comments/{}", userId, commentId);
        return commentService.updateCommentByUser(userId, commentId, updateCommentDto);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long userId,
                              @PathVariable Long commentId) {
        log.info("Запрос на удаление комментария: DELETE /users/{}/comments/{}", userId, commentId);
        commentService.deleteCommentByUser(userId, commentId);
    }
}