package com.ebikes.assignments.support.audit;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.ebikes.assignments.constants.ApplicationConstants;
import com.ebikes.assignments.constants.EventConstants.Source;
import com.ebikes.assignments.constants.MDCKeys;
import com.ebikes.assignments.database.entities.bases.BaseEntity;
import com.ebikes.assignments.dtos.events.outgoing.AuditEvent;
import com.ebikes.assignments.enums.AuditOutcome;
import com.ebikes.assignments.publishers.AuditEventPublisher;
import com.ebikes.assignments.support.context.ExecutionContext;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@Component
@RequiredArgsConstructor
public class AuditTemplate {

  private final AuditEventPublisher auditEventPublisher;

  public <T extends BaseEntity & Auditable, E extends Exception> void execute(
      T entity, String organizationId, String eventType, ThrowingRunnable<E> operation) {
    execute(
        entity,
        organizationId,
        eventType,
        () -> {
          operation.run();
          return null;
        });
  }

  @SneakyThrows
  public <T extends BaseEntity & Auditable, E extends Exception> T execute(
      T entity, String organizationId, String eventType, ThrowingSupplier<T, E> operation) {
    String entityType = entity.getClass().getSimpleName().toUpperCase();
    try {
      T result = operation.get();
      auditEventPublisher.publishSuccess(
          buildEvent(entity, entityType, organizationId, eventType, AuditOutcome.SUCCESS, null),
          buildRoutingKey(entityType));
      return result;
    } catch (Exception e) {
      auditEventPublisher.publishFailure(
          buildEvent(
              entity, entityType, organizationId, eventType, AuditOutcome.FAILURE, e.getMessage()),
          buildRoutingKey(entityType));
      throw e;
    }
  }

  private <T extends BaseEntity & Auditable> AuditEvent buildEvent(
      T entity,
      String entityType,
      String organizationId,
      String eventType,
      AuditOutcome outcome,
      String failureReason) {

    String actorId =
        switch (ExecutionContext.get()) {
          case ExecutionContext.UserContext uc -> uc.userId();
          case ExecutionContext.SystemContext ignored -> ApplicationConstants.SYSTEM_ID;
        };

    return new AuditEvent(
        entity.getId(),
        entityType,
        eventType,
        failureReason,
        MDC.get(MDCKeys.IP_ADDRESS),
        entity.toAuditMetadata(),
        organizationId,
        outcome,
        null,
        null,
        actorId);
  }

  private String buildRoutingKey(String entityType) {
    return Source.HOST_SERVICE + "." + entityType.toLowerCase() + ".audit";
  }
}
