package com.ebikes.assignments.job;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ebikes.assignments.database.entities.AssignmentOffer;
import com.ebikes.assignments.database.repositories.AssignmentOfferRepository;
import com.ebikes.assignments.enums.OfferStatus;
import com.ebikes.assignments.services.assignments.OfferExpiryService;
import com.ebikes.assignments.support.context.ExecutionContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OfferExpiryJob {

  private final AssignmentOfferRepository assignmentOfferRepository;
  private final OfferExpiryService offerExpiryService;

  @Scheduled(fixedDelayString = "${assignments.expiry.poll-interval-ms:15000}")
  public void processExpiredOffers() {
    List<AssignmentOffer> expired =
        assignmentOfferRepository.findByStatusAndExpiresAtBefore(
            OfferStatus.CREATED, OffsetDateTime.now(ZoneOffset.UTC));

    if (expired.isEmpty()) {
      return;
    }

    log.debug("Processing {} expired offers", expired.size());

    ExecutionContext.setSystem();
    try {
      expired.forEach(offerExpiryService::processOffer);
    } finally {
      ExecutionContext.clear();
    }
  }
}
