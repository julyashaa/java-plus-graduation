package ru.practicum.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.comment.dto.*;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.comment.repository.specification.CommentSpecification;
import ru.practicum.event.dto.EventRequestInfoDto;
import ru.practicum.user.UserDto;
import ru.practicum.user.UserShortDto;
import ru.practicum.exception.ForbiddenException;
import ru.practicum.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final CommentRepository commentRepository;
    private final CommentRemoteService commentRemoteService;

    @Override
    public CommentDto createComment(Long userId, NewCommentDto newCommentDto) {
        log.info("Создание нового комментария: {}", newCommentDto);

        UserDto user = getUserOrElseThrow(userId);
        EventRequestInfoDto event = getEventOrElseThrow(newCommentDto.getEventId());

        Comment comment = commentMapper.toEntity(newCommentDto);
        comment.setCreatedOn(LocalDateTime.now());
        comment.setAuthorId(user.getId());
        comment.setEventId(event.getId());

        Comment savedComment = commentRepository.save(comment);

        log.info("Комментарий создан с id: {}", savedComment.getId());

        CommentDto result = commentMapper.toDto(savedComment);
        result.setAuthor(
                new UserShortDto(
                        user.getId(),
                        user.getName()
                )
        );

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ShortCommentDto getComment(Long commentId) {
        log.info("Получение комментария с id: {}", commentId);

        Comment comment = getCommentOrElseThrow(commentId);

        return commentMapper.toShortDto(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public CommentDto getCommentByUser(Long userId, Long commentId) {
        log.info("Получение комментария с id: {}", commentId);

        getUserOrElseThrow(userId);

        Comment comment = getCommentOrElseThrow(commentId);

        CommentDto result = commentMapper.toDto(comment);
        result.setAuthor(getUserShortDto(comment.getAuthorId()));

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getUserComments(Long userId, GetCommentsDtoParams params) {
        log.info("Получение комментариев пользователя с параметрами: {}", params);

        Pageable page = getPageWithSortOnCreatedOn(params.getFrom(), params.getSize());

        List<Comment> result = commentRepository.findByAuthorId(userId, page);

        return mapToListCommentDto(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShortCommentDto> getEventComments(Long eventId, GetCommentsDtoParams params) {
        log.info("Получение комментариев ивента с параметрами: {}", params);

        Pageable page = getPageWithSortOnCreatedOn(params.getFrom(), params.getSize());

        List<Comment> result = commentRepository.findByEventId(eventId, page);

        return mapToListShortCommentDto(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsByParams(GetCommentsAdminDtoParams params) {
        log.info("Получение комментариев админом с параметрами: {}", params);

        Pageable pageable = getPageWithSortOnCreatedOn(params.getFrom(), params.getSize());

        Specification<Comment> commentSpecification = CommentSpecification.withFilters(
                params.getIds(),
                params.getUserId(),
                params.getEventId(),
                params.getRangeStart(),
                params.getRangeEnd()
        );

        List<Comment> comments = commentRepository.findAll(commentSpecification, pageable).getContent();

        return mapToListCommentDto(comments);
    }

    @Override
    public CommentDto updateCommentByAdmin(Long commentId, UpdateCommentDto updateCommentDto) {
        log.info("Обновление комментария с id: {}", commentId);

        Comment comment = getCommentOrElseThrow(commentId);

        String text = updateCommentDto.getText();
        if (text != null) {
            comment.setText(text);
            comment.setEditedOn(LocalDateTime.now());
        }
        Comment savedComment = commentRepository.save(comment);
        log.info("Комментарий обновлен {}", savedComment);

        CommentDto result = commentMapper.toDto(savedComment);
        result.setAuthor(getUserShortDto(comment.getAuthorId()));
        return result;
    }

    @Override
    public CommentDto updateCommentByUser(Long userId, Long commentId, UpdateCommentDto updateCommentDto) {
        log.info("Обновление комментария с id: {}", commentId);

        UserDto user = getUserOrElseThrow(userId);
        Comment comment = getCommentOrElseThrow(commentId);

        throwIfUserNotAuthorComment(userId, comment);

        String text = updateCommentDto.getText();
        if (text != null) {
            comment.setText(text);
            comment.setEditedOn(LocalDateTime.now());
        }
        Comment savedComment = commentRepository.save(comment);
        log.info("Комментарий обновлен {}", savedComment);

        CommentDto result = commentMapper.toDto(savedComment);
        result.setAuthor(new UserShortDto(user.getId(), user.getName()));
        return result;
    }

    @Override
    public void deleteCommentByAdmin(Long commentId) {
        log.info("Удаление комментария с id: {}", commentId);

        Comment comment = getCommentOrElseThrow(commentId);

        commentRepository.delete(comment);

        log.info("Комментарий с id {} удален", commentId);
    }

    @Override
    public void deleteCommentByUser(Long userId, Long commentId) {
        log.info("Удаление комментария с id: {}", commentId);

        Comment comment = getCommentOrElseThrow(commentId);

        throwIfUserNotAuthorComment(userId, comment);

        commentRepository.delete(comment);

        log.info("Комментарий с id {} удален", commentId);
    }

    private List<CommentDto> mapToListCommentDto(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return Collections.emptyList();
        }

        return comments.stream()
                .map(comment -> {
                            CommentDto dto = commentMapper.toDto(comment);
                            dto.setAuthor(getUserShortDto(comment.getAuthorId()));
                            return dto;
                        }
                )
                .toList();
    }

    private List<ShortCommentDto> mapToListShortCommentDto(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return Collections.emptyList();
        }

        return comments.stream()
                .map(commentMapper::toShortDto)
                .toList();
    }

    private PageRequest getPageWithSortOnCreatedOn(Integer from, Integer size) {
        return PageRequest.of(
                from / size,
                size,
                Sort.by("createdOn").descending());
    }

    private UserDto getUserOrElseThrow(Long userId) {
        return commentRemoteService.getUserById(userId);
    }


    private EventRequestInfoDto getEventOrElseThrow(Long eventId) {
        return commentRemoteService.getEventRequestInfo(eventId);
    }

    private Comment getCommentOrElseThrow(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment with id " + commentId + " not found"));
    }

    private void throwIfUserNotAuthorComment(Long userId, Comment comment) {
        if (!comment.getAuthorId().equals(userId)) {
            throw new ForbiddenException("User with id " + userId + " not author comment");
        }
    }

    private UserShortDto getUserShortDto(Long userId) {
        UserDto user = getUserOrElseThrow(userId);
        return new UserShortDto(user.getId(), user.getName());
    }
}