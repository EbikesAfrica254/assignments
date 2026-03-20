package com.ebikes.assignments.support.database;

import java.time.OffsetDateTime;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.ebikes.assignments.dtos.requests.filters.bases.BaseFilter;
import com.ebikes.assignments.enums.ResponseCode;
import com.ebikes.assignments.exceptions.ValidationException;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FilterUtilities {

  public static Pageable buildPageable(BaseFilter filter, Set<String> allowedSortFields) {
    String sortBy = filter.getSortBy();
    if (!allowedSortFields.contains(sortBy)) {
      throw new ValidationException(
          ResponseCode.INVALID_ARGUMENTS,
          "Invalid sort field specified. Allowed fields: " + allowedSortFields,
          "sortBy",
          sortBy);
    }
    Sort.Direction direction = Sort.Direction.fromString(filter.getSortDirection());
    Sort sort = Sort.by(direction, sortBy);
    int zeroIndexedPage = filter.getPage() - 1;
    return PageRequest.of(zeroIndexedPage, filter.getSize(), sort);
  }

  public static <T> Specification<T> offsetDateTimeAfter(String fieldPath, OffsetDateTime after) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.greaterThanOrEqualTo(root.get(fieldPath), after);
  }

  public static <T> Specification<T> offsetDateTimeBefore(String fieldPath, OffsetDateTime before) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.lessThanOrEqualTo(root.get(fieldPath), before);
  }
}
