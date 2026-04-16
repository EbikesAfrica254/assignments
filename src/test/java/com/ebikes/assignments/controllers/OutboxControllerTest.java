package com.ebikes.assignments.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ebikes.assignments.dtos.requests.filters.OutboxFilter;
import com.ebikes.assignments.dtos.responses.api.PaginatedResponse;
import com.ebikes.assignments.dtos.responses.outbox.OutboxResponse;
import com.ebikes.assignments.enums.UserRole;
import com.ebikes.assignments.services.events.OutboxService;
import com.ebikes.assignments.support.infrastructure.AbstractControllerTest;

@DisplayName("OutboxController")
@WebMvcTest(OutboxController.class)
class OutboxControllerTest extends AbstractControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private OutboxService outboxService;

  @Nested
  @DisplayName("search")
  class Search {

    @Test
    @DisplayName("should return 200 with paginated outbox response")
    void shouldReturnPaginatedOutboxResponse() throws Exception {
      PaginatedResponse<OutboxResponse> response =
          new PaginatedResponse<>(List.of(), true, true, "Outbox events retrieved", 0, 20, 0, 0);
      when(outboxService.search(any())).thenReturn(response);

      mockMvc
          .perform(get("/outbox").with(authenticatedJwt(UserRole.SYSTEM_ADMIN)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").exists())
          .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("should delegate to service with correct filter")
    void shouldDelegateToServiceWithCorrectFilter() throws Exception {
      PaginatedResponse<OutboxResponse> response =
          new PaginatedResponse<>(List.of(), true, true, "Outbox events retrieved", 0, 20, 0, 0);
      when(outboxService.search(any())).thenReturn(response);

      mockMvc
          .perform(get("/outbox").with(authenticatedJwt(UserRole.SYSTEM_ADMIN)))
          .andExpect(status().isOk());

      verify(outboxService).search(any(OutboxFilter.class));
    }

    @Test
    @DisplayName("should return 401 when not authenticated")
    void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
      mockMvc.perform(get("/outbox").with(anonymous())).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return 403 when role is not permitted")
    void shouldReturnForbiddenWhenInsufficientRole() throws Exception {
      mockMvc
          .perform(get("/outbox").with(authenticatedJwt(UserRole.AGENT)))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("retry")
  class Retry {

    @Test
    @DisplayName("should return 204 on retry")
    void shouldReturnNoContent() throws Exception {
      mockMvc
          .perform(
              patch("/outbox/{id}/retry", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.SYSTEM_ADMIN)))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should delegate to service with correct id")
    void shouldDelegateToServiceWithCorrectId() throws Exception {
      UUID id = UUID.randomUUID();

      mockMvc
          .perform(patch("/outbox/{id}/retry", id).with(authenticatedJwt(UserRole.SYSTEM_ADMIN)))
          .andExpect(status().isNoContent());

      verify(outboxService).retry(id);
    }

    @Test
    @DisplayName("should return 401 when not authenticated")
    void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
      mockMvc
          .perform(patch("/outbox/{id}/retry", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return 403 when role is not permitted")
    void shouldReturnForbiddenWhenInsufficientRole() throws Exception {
      mockMvc
          .perform(
              patch("/outbox/{id}/retry", UUID.randomUUID()).with(authenticatedJwt(UserRole.AGENT)))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("retryAll")
  class RetryAll {

    @Test
    @DisplayName("should return 200 with count and message")
    void shouldReturnCountWithMessage() throws Exception {
      when(outboxService.retryAllFailed()).thenReturn(3);

      mockMvc
          .perform(post("/outbox/failed/retry").with(authenticatedJwt(UserRole.SYSTEM_ADMIN)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value("SUCCESS"))
          .andExpect(jsonPath("$.data").value(3))
          .andExpect(jsonPath("$.message").value("3 failed event(s) reset to pending"));
    }

    @Test
    @DisplayName("should return 401 when not authenticated")
    void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
      mockMvc
          .perform(post("/outbox/failed/retry").with(anonymous()))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return 403 when role is not permitted")
    void shouldReturnForbiddenWhenInsufficientRole() throws Exception {
      mockMvc
          .perform(post("/outbox/failed/retry").with(authenticatedJwt(UserRole.AGENT)))
          .andExpect(status().isForbidden());
    }
  }
}
