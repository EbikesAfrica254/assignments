package com.ebikes.assignments.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ebikes.assignments.services.offers.OfferExpiryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OfferExpiryJob {

  private final OfferExpiryService offerExpiryService;

  @Scheduled(fixedDelayString = "${assignments.expiry.poll-interval-ms:15000}")
  public void processExpiredOffers() {
    offerExpiryService.processExpired();
  }
}
