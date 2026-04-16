package com.ebikes.assignments.listeners.internal;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ebikes.assignments.dtos.events.internal.OfferDeclinedTrigger;
import com.ebikes.assignments.services.assignments.OrchestrationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OfferDeclinedHandler extends AbstractInternalEventHandler<OfferDeclinedTrigger> {

  private final OrchestrationService orchestrationService;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onOfferDeclined(OfferDeclinedTrigger trigger) {
    handle(trigger);
  }

  @Override
  protected void process(OfferDeclinedTrigger trigger) {
    orchestrationService.onOfferDeclined(trigger);
  }
}
