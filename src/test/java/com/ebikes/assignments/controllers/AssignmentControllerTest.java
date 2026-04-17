package com.ebikes.assignments.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ebikes.assignments.dtos.requests.ManualAssignRequest;
import com.ebikes.assignments.enums.UserRole;
import com.ebikes.assignments.services.assignments.AssignmentService;
import com.ebikes.assignments.support.fixtures.RequestFixtures;
import com.ebikes.assignments.support.fixtures.ResponseFixtures;
import com.ebikes.assignments.support.infrastructure.AbstractControllerTest;

import tools.jackson.databind.ObjectMapper;

@DisplayName("AssignmentController")
@WebMvcTest(AssignmentController.class)
class AssignmentControllerTest extends AbstractControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private AssignmentService assignmentService;

  @Nested
  @DisplayName("getById")
  class GetById {

    @Test
    @DisplayName("should return 200 with assignment response")
    void shouldReturnAssignmentWhenFound() throws Exception {
      UUID assignmentId = UUID.randomUUID();
      when(assignmentService.getById(assignmentId)).thenReturn(ResponseFixtures.assignment());

      mockMvc
          .perform(get("/assignments/{assignmentId}", assignmentId).with(authenticatedJwt()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value("SUCCESS"))
          .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("should return 401 when not authenticated")
    void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
      mockMvc
          .perform(get("/assignments/{assignmentId}", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("getByOrderId")
  class GetByOrderId {

    @Test
    @DisplayName("should return 200 with assignment response")
    void shouldReturnAssignmentByOrderId() throws Exception {
      UUID orderId = UUID.randomUUID();
      when(assignmentService.getByOrderId(orderId)).thenReturn(ResponseFixtures.assignment());

      mockMvc
          .perform(get("/assignments/by-order/{orderId}", orderId).with(authenticatedJwt()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value("SUCCESS"))
          .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("should return 401 when not authenticated")
    void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
      mockMvc
          .perform(get("/assignments/by-order/{orderId}", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("manualAssign")
  class ManualAssign {

    @Test
    @DisplayName("should return 200 with assignment response and message")
    void shouldReturnAssignmentOnManualAssign() throws Exception {
      when(assignmentService.manualAssign(any(), any(), any()))
          .thenReturn(ResponseFixtures.assignment());

      mockMvc
          .perform(
              post("/assignments/manual")
                  .with(authenticatedJwt(UserRole.ORGANIZATION_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(RequestFixtures.manualAssign())))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value("SUCCESS"))
          .andExpect(jsonPath("$.message").value("Assignment created manually"));
    }

    @Test
    @DisplayName("should delegate to service with correct arguments")
    void shouldDelegateToServiceWithCorrectArguments() throws Exception {
      UUID orderId = UUID.randomUUID();
      ManualAssignRequest request = RequestFixtures.manualAssignWithOrderId(orderId);
      when(assignmentService.manualAssign(any(), any(), any()))
          .thenReturn(ResponseFixtures.assignment());

      mockMvc
          .perform(
              post("/assignments/manual")
                  .with(authenticatedJwt(UserRole.ORGANIZATION_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk());

      verify(assignmentService).manualAssign(orderId, request.agentId(), request.reason());
    }

    @Test
    @DisplayName("should return 401 when not authenticated")
    void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
      mockMvc
          .perform(
              post("/assignments/manual")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(RequestFixtures.manualAssign())))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return 403 when role is not permitted")
    void shouldReturnForbiddenWhenInsufficientRole() throws Exception {
      mockMvc
          .perform(
              post("/assignments/manual")
                  .with(authenticatedJwt(UserRole.AGENT))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(RequestFixtures.manualAssign())))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("cancel")
  class Cancel {

    @Test
    @DisplayName("should return 200 with assignment response and message")
    void shouldReturnAssignmentOnCancel() throws Exception {
      when(assignmentService.cancel(any(), any())).thenReturn(ResponseFixtures.assignment());

      mockMvc
          .perform(
              post("/assignments/{assignmentId}/cancel", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.ORGANIZATION_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(RequestFixtures.cancel())))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value("SUCCESS"))
          .andExpect(jsonPath("$.message").value("Assignment cancelled"));
    }

    @Test
    @DisplayName("should delegate to service with correct arguments")
    void shouldDelegateToServiceWithCorrectArguments() throws Exception {
      UUID assignmentId = UUID.randomUUID();
      var request = RequestFixtures.cancel();
      when(assignmentService.cancel(any(), any())).thenReturn(ResponseFixtures.assignment());

      mockMvc
          .perform(
              post("/assignments/{assignmentId}/cancel", assignmentId)
                  .with(authenticatedJwt(UserRole.ORGANIZATION_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk());

      verify(assignmentService).cancel(assignmentId, request.reason());
    }

    @Test
    @DisplayName("should return 401 when not authenticated")
    void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
      mockMvc
          .perform(
              post("/assignments/{assignmentId}/cancel", UUID.randomUUID())
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(RequestFixtures.cancel())))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return 403 when role is not permitted")
    void shouldReturnForbiddenWhenInsufficientRole() throws Exception {
      mockMvc
          .perform(
              post("/assignments/{assignmentId}/cancel", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.AGENT))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(RequestFixtures.cancel())))
          .andExpect(status().isForbidden());
    }
  }
}
