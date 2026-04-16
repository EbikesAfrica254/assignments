package com.ebikes.assignments.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ebikes.assignments.enums.UserRole;
import com.ebikes.assignments.services.offers.OfferService;
import com.ebikes.assignments.support.fixtures.ResponseFixtures;
import com.ebikes.assignments.support.fixtures.SecurityFixtures;
import com.ebikes.assignments.support.infrastructure.AbstractControllerTest;

@DisplayName("OfferController")
@WebMvcTest(OfferController.class)
class OfferControllerTest extends AbstractControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private OfferService offerService;

  @Nested
  @DisplayName("accept")
  class Accept {

    @Test
    @DisplayName("should return 200 with offer response and message")
    void shouldReturnOfferOnAccept() throws Exception {
      when(offerService.accept(any(), any())).thenReturn(ResponseFixtures.offer());

      mockMvc
          .perform(
              post("/assignment-offers/{offerId}/accept", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.AGENT)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value("SUCCESS"))
          .andExpect(jsonPath("$.message").value("Offer accepted"))
          .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("should delegate to service with correct arguments")
    void shouldDelegateToServiceWithCorrectArguments() throws Exception {
      UUID offerId = UUID.randomUUID();
      when(offerService.accept(any(), any())).thenReturn(ResponseFixtures.offer());

      mockMvc
          .perform(
              post("/assignment-offers/{offerId}/accept", offerId)
                  .with(authenticatedJwt(UserRole.AGENT)))
          .andExpect(status().isOk());

      verify(offerService).accept(offerId, SecurityFixtures.TEST_USER_ID);
    }

    @Test
    @DisplayName("should return 401 when not authenticated")
    void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
      mockMvc
          .perform(post("/assignment-offers/{offerId}/accept", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return 403 when role is not permitted")
    void shouldReturnForbiddenWhenInsufficientRole() throws Exception {
      mockMvc
          .perform(
              post("/assignment-offers/{offerId}/accept", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.ORGANIZATION_ADMIN)))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("decline")
  class Decline {

    @Test
    @DisplayName("should return 200 with offer response and message")
    void shouldReturnOfferOnDecline() throws Exception {
      when(offerService.decline(any(), any())).thenReturn(ResponseFixtures.offer());

      mockMvc
          .perform(
              post("/assignment-offers/{offerId}/decline", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.AGENT)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value("SUCCESS"))
          .andExpect(jsonPath("$.message").value("Offer declined"))
          .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("should delegate to service with correct arguments")
    void shouldDelegateToServiceWithCorrectArguments() throws Exception {
      UUID offerId = UUID.randomUUID();
      when(offerService.decline(any(), any())).thenReturn(ResponseFixtures.offer());

      mockMvc
          .perform(
              post("/assignment-offers/{offerId}/decline", offerId)
                  .with(authenticatedJwt(UserRole.AGENT)))
          .andExpect(status().isOk());

      verify(offerService).decline(offerId, SecurityFixtures.TEST_USER_ID);
    }

    @Test
    @DisplayName("should return 401 when not authenticated")
    void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
      mockMvc
          .perform(
              post("/assignment-offers/{offerId}/decline", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return 403 when role is not permitted")
    void shouldReturnForbiddenWhenInsufficientRole() throws Exception {
      mockMvc
          .perform(
              post("/assignment-offers/{offerId}/decline", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.ORGANIZATION_ADMIN)))
          .andExpect(status().isForbidden());
    }
  }
}
