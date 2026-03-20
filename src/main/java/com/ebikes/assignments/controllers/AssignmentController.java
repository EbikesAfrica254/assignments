package com.ebikes.assignments.controllers;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ebikes.assignments.dtos.requests.CancelAssignmentRequest;
import com.ebikes.assignments.dtos.requests.ManualAssignRequest;
import com.ebikes.assignments.dtos.responses.api.SuccessResponse;
import com.ebikes.assignments.dtos.responses.assignments.AssignmentResponse;
import com.ebikes.assignments.enums.ResponseCode;
import com.ebikes.assignments.enums.UserRole;
import com.ebikes.assignments.exceptions.AuthorizationException;
import com.ebikes.assignments.services.assignments.AssignmentService;
import com.ebikes.assignments.support.context.ExecutionContext;
import com.ebikes.assignments.support.security.RBACUtilities;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RequestMapping("/assignments")
@RestController
public class AssignmentController {

  private final AssignmentService assignmentService;

  @GetMapping("/{assignmentId}")
  public ResponseEntity<SuccessResponse<AssignmentResponse>> getById(
      @PathVariable UUID assignmentId) {
    return ResponseEntity.ok(SuccessResponse.of(assignmentService.getById(assignmentId)));
  }

  @GetMapping("/by-order/{orderId}")
  public ResponseEntity<SuccessResponse<AssignmentResponse>> getByOrderId(
      @PathVariable UUID orderId) {
    return ResponseEntity.ok(SuccessResponse.of(assignmentService.getByOrderId(orderId)));
  }

  @PostMapping("/manual")
  public ResponseEntity<SuccessResponse<AssignmentResponse>> manualAssign(
      @RequestBody @Valid ManualAssignRequest request) {
    enforceFleetManagerOrAbove();
    return ResponseEntity.ok(
        SuccessResponse.of(
            assignmentService.manualAssign(request.orderId(), request.agentId(), request.reason()),
            "Assignment created manually"));
  }

  @PostMapping("/{assignmentId}/cancel")
  public ResponseEntity<SuccessResponse<AssignmentResponse>> cancel(
      @PathVariable UUID assignmentId, @RequestBody @Valid CancelAssignmentRequest request) {
    enforceFleetManagerOrAbove();
    return ResponseEntity.ok(
        SuccessResponse.of(
            assignmentService.cancel(assignmentId, request.reason()), "Assignment cancelled"));
  }

  private void enforceFleetManagerOrAbove() {
    if (!RBACUtilities.hasAdminRoleFromNames(ExecutionContext.getRoles())
        && !ExecutionContext.getRoles().contains(UserRole.ORGANIZATION_FLEET_MANAGER.name())
        && !ExecutionContext.getRoles().contains(UserRole.BRANCH_FLEET_MANAGER.name())) {
      throw new AuthorizationException(
          ResponseCode.FORBIDDEN, "Access denied: fleet manager or admin role required");
    }
  }
}
