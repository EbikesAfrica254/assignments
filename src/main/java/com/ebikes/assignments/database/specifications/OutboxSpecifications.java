package com.ebikes.assignments.database.specifications;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.ebikes.assignments.database.entities.Outbox;
import com.ebikes.assignments.dtos.requests.filters.OutboxFilter;
import com.ebikes.assignments.enums.OutboxStatus;
import com.ebikes.assignments.support.database.FilterUtilities;

public class OutboxSpecifications {

  public static final String FIELD_CREATED_AT = "createdAt";
  public static final String FIELD_EVENT_TYPE = "eventType";
  public static final String FIELD_RETRY_COUNT = "retryCount";
  public static final String FIELD_ROUTING_KEY = "routingKey";
  public static final String FIELD_STATUS = "status";
  public static final String FIELD_UPDATED_AT = "updatedAt";

  public static final Set<String> ALLOWED_SORT_FIELDS =
      Set.of(FIELD_CREATED_AT, FIELD_EVENT_TYPE, FIELD_RETRY_COUNT, FIELD_STATUS, FIELD_UPDATED_AT);

  private OutboxSpecifications() {
    // prevent instantiation
  }

  public static Specification<Outbox> buildSpecification(OutboxFilter filter) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getCreatedAtAfter(),
          offsetDateTimeAfter(FIELD_CREATED_AT, filter.getCreatedAtAfter()));
      addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getCreatedAtBefore(),
          offsetDateTimeBefore(FIELD_CREATED_AT, filter.getCreatedAtBefore()));
      addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getEventType(),
          hasEventType(filter.getEventType()));
      addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getMaxRetryCount(),
          hasMaxRetryCount(filter.getMaxRetryCount()));
      addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getMinRetryCount(),
          hasMinRetryCount(filter.getMinRetryCount()));
      addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getRoutingKey(),
          hasRoutingKey(filter.getRoutingKey()));
      addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getStatus(),
          hasStatus(filter.getStatus()));
      addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getUpdatedAtAfter(),
          offsetDateTimeAfter(FIELD_UPDATED_AT, filter.getUpdatedAtAfter()));
      addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getUpdatedAtBefore(),
          offsetDateTimeBefore(FIELD_UPDATED_AT, filter.getUpdatedAtBefore()));

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }

  public static Specification<Outbox> hasEventType(String eventType) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(FIELD_EVENT_TYPE), eventType);
  }

  public static Specification<Outbox> hasMaxRetryCount(Integer maxRetryCount) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.lessThanOrEqualTo(root.get(FIELD_RETRY_COUNT), maxRetryCount);
  }

  public static Specification<Outbox> hasMinRetryCount(Integer minRetryCount) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.greaterThanOrEqualTo(root.get(FIELD_RETRY_COUNT), minRetryCount);
  }

  public static Specification<Outbox> hasRoutingKey(String routingKey) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(FIELD_ROUTING_KEY), routingKey);
  }

  public static Specification<Outbox> hasStatus(OutboxStatus status) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(FIELD_STATUS), status);
  }

  public static Specification<Outbox> offsetDateTimeAfter(String fieldPath, OffsetDateTime after) {
    return FilterUtilities.offsetDateTimeAfter(fieldPath, after);
  }

  public static Specification<Outbox> offsetDateTimeBefore(
      String fieldPath, OffsetDateTime before) {
    return FilterUtilities.offsetDateTimeBefore(fieldPath, before);
  }

  private static void addIfPresent(
      List<Predicate> predicates,
      jakarta.persistence.criteria.Root<Outbox> root,
      jakarta.persistence.criteria.CriteriaQuery<?> query,
      jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
      Object value,
      Specification<Outbox> spec) {
    if (value != null && !(value instanceof String s && s.isBlank())) {
      predicates.add(spec.toPredicate(root, query, criteriaBuilder));
    }
  }
}
