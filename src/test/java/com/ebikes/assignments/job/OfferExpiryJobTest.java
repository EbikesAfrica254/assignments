package com.ebikes.assignments.job;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.assignments.services.offers.OfferExpiryService;

@DisplayName("OfferExpiryJob")
@ExtendWith(MockitoExtension.class)
class OfferExpiryJobTest {

  @Mock private OfferExpiryService offerExpiryService;

  private OfferExpiryJob job;

  @BeforeEach
  void setUp() {
    job = new OfferExpiryJob(offerExpiryService);
  }

  @Nested
  @DisplayName("processExpiredOffers")
  class ProcessExpiredOffers {

    @Test
    @DisplayName("should delegate to offer expiry service")
    void shouldDelegateToOfferExpiryService() {
      job.processExpiredOffers();

      verify(offerExpiryService).processExpired();
    }
  }
}
