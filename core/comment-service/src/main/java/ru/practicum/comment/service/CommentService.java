package ru.practicum.comment.service;

import ru.practicum.comment.dto.*;

import java.util.List;

public interface CommentService {

    CommentDto createComment(Long userId, NewCommentDto newCommentDto);

    ShortCommentDto getComment(Long commentId);

    CommentDto getCommentByUser(Long userId, Long commentId);

    List<CommentDto> getUserComments(Long userId, GetCommentsDtoParams params);

    List<ShortCommentDto> getEventComments(Long eventId, GetCommentsDtoParams params);

    List<CommentDto> getCommentsByParams(GetCommentsAdminDtoParams params);

    CommentDto updateCommentByAdmin(Long commentId, UpdateCommentDto updateCommentDto);

    CommentDto updateCommentByUser(Long userId, Long commentId, UpdateCommentDto updateCommentDto);

    void deleteCommentByAdmin(Long commentId);

    void deleteCommentByUser(Long userId, Long commentId);
}