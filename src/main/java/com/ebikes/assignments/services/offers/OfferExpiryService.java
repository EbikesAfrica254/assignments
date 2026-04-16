package com.ebikes.assignments.services.offers;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.assignments.configurations.properties.ServiceProperties;
import com.ebikes.assignments.database.projections.AssignmentReference;
import com.ebikes.assignments.database.repositories.OfferRepository;
import com.ebikes.assignments.enums.OfferStatus;
import com.ebikes.assignments.mappers.OfferMapper;
import com.ebikes.assignments.support.context.ExecutionContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class OfferExpiryService {

  private final ApplicationEventPublisher applicationEventPublisher;
  private final OfferRepository offerRepository;
  private final OfferMapper offerMapper;
  private final ServiceProperties serviceProperties;

  public OffsetDateTime offerExpiresAt() {
    return OffsetDateTime.now(ZoneOffset.UTC)
        .plusMinutes(serviceProperties.getOffers().getExpiryMinutes());
  }

  @Transactional
  public void processExpired() {
    List<UUID> expiredIds =
        offerRepository.findIdsByStatusAndExpiresAtBefore(
            OfferStatus.CREATED, OffsetDateTime.now(ZoneOffset.UTC));

    if (expiredIds.isEmpty()) {
      return;
    }

    log.debug("Expiring {} offers in bulk", expiredIds.size());

    try {
      offerRepository.bulkUpdateStatus(expiredIds, OfferStatus.EXPIRED);

      List<AssignmentReference> assignmentIds =
          offerRepository.findAssignmentReferencesByIds(expiredIds);

      assignmentIds.forEach(
          reference ->
              applicationEventPublisher.publishEvent(offerMapper.toExpiredTrigger(reference)));

      log.info(
          "Bulk expired {} offers across {} assignments", expiredIds.size(), assignmentIds.size());

    } finally {
      ExecutionContext.clear();
    }
  }
}
