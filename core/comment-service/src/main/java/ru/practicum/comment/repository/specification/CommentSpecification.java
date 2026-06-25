package ru.practicum.comment.repository.specification;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.comment.model.Comment;

import java.time.LocalDateTime;
import java.util.List;

public class CommentSpecification {

    public static Specification<Comment> withFilters(List<Long> ids,
                                                     Long userId,
                                                     Long eventId,
                                                     LocalDateTime rangeStart,
                                                     LocalDateTime rangeEnd) {

        return (root, query, cb) -> {
            root.fetch("author", JoinType.LEFT);
            root.fetch("event", JoinType.LEFT);

            query.distinct(true);

            Predicate predicates = cb.conjunction();

            if (ids != null && !ids.isEmpty()) {
                predicates = cb.and(predicates, root.get("id").in(ids));
            }

            if (userId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("author").get("id"), userId));
            }

            if (eventId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("event").get("id"), eventId));
            }

            if (rangeStart != null) {
                predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("createdOn"), rangeStart));
            }

            if (rangeEnd != null) {
                predicates = cb.and(predicates, cb.lessThanOrEqualTo(root.get("createdOn"), rangeEnd));
            }

            return predicates;
        };
    }
}
