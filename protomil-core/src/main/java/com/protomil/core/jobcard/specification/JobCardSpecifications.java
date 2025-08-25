package com.protomil.core.jobcard.specification;

import com.protomil.core.jobcard.domain.JobCard;
import com.protomil.core.jobcard.domain.enums.JobStatus;
import com.protomil.core.jobcard.domain.enums.Priority;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JobCardSpecifications {

    public static Specification<JobCard> hasStatus(JobStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? null : criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<JobCard> hasAssignedTo(Long assignedTo) {
        return (root, query, criteriaBuilder) ->
                assignedTo == null ? null : criteriaBuilder.equal(root.get("assignedTo"), assignedTo);
    }

    public static Specification<JobCard> hasCreatedBy(Long createdBy) {
        return (root, query, criteriaBuilder) ->
                createdBy == null ? null : criteriaBuilder.equal(root.get("createdBy"), createdBy);
    }

    public static Specification<JobCard> hasPriority(Priority priority) {
        return (root, query, criteriaBuilder) ->
                priority == null ? null : criteriaBuilder.equal(root.get("priority"), priority);
    }

    public static Specification<JobCard> hasTemplateId(Long templateId) {
        return (root, query, criteriaBuilder) ->
                templateId == null ? null : criteriaBuilder.equal(root.get("template").get("id"), templateId);
    }

    public static Specification<JobCard> isOverdue() {
        return (root, query, criteriaBuilder) -> {
            LocalDateTime now = LocalDateTime.now();
            return criteriaBuilder.and(
                    criteriaBuilder.lessThan(root.get("targetCompletionDate"), now),
                    criteriaBuilder.not(root.get("status").in(JobStatus.COMPLETED, JobStatus.CANCELLED))
            );
        };
    }

    public static Specification<JobCard> hasStatusAndAssignedTo(JobStatus status, UUID assignedTo) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (assignedTo != null) {
                predicates.add(criteriaBuilder.equal(root.get("assignedTo"), assignedTo));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<JobCard> createdBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            if (startDate == null && endDate == null) {
                return null;
            }
            if (startDate == null) {
                return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDate);
            }
            if (endDate == null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDate);
            }
            return criteriaBuilder.between(root.get("createdAt"), startDate, endDate);
        };
    }

    public static Specification<JobCard> hasTargetDateBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            if (startDate == null && endDate == null) {
                return null;
            }
            if (startDate == null) {
                return criteriaBuilder.lessThanOrEqualTo(root.get("targetCompletionDate"), endDate);
            }
            if (endDate == null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("targetCompletionDate"), startDate);
            }
            return criteriaBuilder.between(root.get("targetCompletionDate"), startDate, endDate);
        };
    }
}