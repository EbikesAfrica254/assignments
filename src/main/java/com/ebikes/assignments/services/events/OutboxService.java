package com.ebikes.assignments.services.events;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.assignments.database.entities.Outbox;
import com.ebikes.assignments.database.repositories.OutboxRepository;
import com.ebikes.assignments.database.specifications.OutboxSpecifications;
import com.ebikes.assignments.dtos.requests.filters.OutboxFilter;
import com.ebikes.assignments.dtos.responses.api.PaginatedResponse;
import com.ebikes.assignments.dtos.responses.outbox.OutboxResponse;
import com.ebikes.assignments.enums.OutboxStatus;
import com.ebikes.assignments.enums.ResponseCode;
import com.ebikes.assignments.exceptions.ResourceNotFoundException;
import com.ebikes.assignments.mappers.OutboxMapper;
import com.ebikes.assignments.support.database.FilterUtilities;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class OutboxService {
  private final OutboxMapper mapper;
  private final OutboxRepository repository;

  @Transactional
  public void bulkPublish(String eventType, List<?> payloads, String routingKey) {
    List<Outbox> outboxes =
        payloads.stream().map(payload -> build(eventType, payload, routingKey)).toList();
    repository.saveAll(outboxes);
    log.debug("Outbox records created: eventType={}, count={}", eventType, outboxes.size());
  }

  @Transactional
  public void publish(String eventType, Object payload, String routingKey) {
    Outbox outbox =
        Outbox.builder().eventType(eventType).payload(payload).routingKey(routingKey).build();

    repository.save(outbox);

    log.debug("Outbox record created: eventType={}, outboxId={}", eventType, outbox.getId());
  }

  @Transactional
  public void retry(UUID outboxId) {
    log.info("Retrying failed outbox event: outboxId={}", outboxId);

    Outbox outbox = requireById(outboxId);
    outbox.resetForRetry();
    repository.save(outbox);

    log.info("Outbox event reset to PENDING: outboxId={}", outboxId);
  }

  @Transactional
  public int retryAllFailed() {
    log.info("Retrying all failed outbox events");

    List<Outbox> failedEvents = repository.findByStatusOrderByIdAsc(OutboxStatus.FAILED);
    failedEvents.forEach(Outbox::resetForRetry);
    repository.saveAll(failedEvents);

    log.info("Reset {} failed outbox events to PENDING", failedEvents.size());

    return failedEvents.size();
  }

  @Transactional(readOnly = true)
  public PaginatedResponse<OutboxResponse> search(OutboxFilter filter) {
    Specification<Outbox> spec = OutboxSpecifications.buildSpecification(filter);
    Pageable pageable =
        FilterUtilities.buildPageable(filter, OutboxSpecifications.ALLOWED_SORT_FIELDS);
    Page<OutboxResponse> page = repository.findAll(spec, pageable).map(mapper::toResponse);
    return PaginatedResponse.from("Outbox events retrieved", page);
  }

  public Outbox requireById(UUID outboxId) {
    return repository
        .findById(outboxId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND, "Outbox event not found: " + outboxId));
  }

  private Outbox build(String eventType, Object payload, String routingKey) {
    return Outbox.builder().eventType(eventType).payload(payload).routingKey(routingKey).build();
  }
}
