package com.ebikes.assignments.controllers;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.ebikes.assignments.services.assignments.AssignmentService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RequestMapping("/assignments")
@RestController
public class AssignmentController {

  private final AssignmentService assignmentService;

  @PreAuthorize("isAuthenticated()")
  @GetMapping("/{assignmentId}")
  public ResponseEntity<SuccessResponse<AssignmentResponse>> getById(
      @PathVariable UUID assignmentId) {
    return ResponseEntity.ok(SuccessResponse.of(assignmentService.getById(assignmentId)));
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping("/by-order/{orderId}")
  public ResponseEntity<SuccessResponse<AssignmentResponse>> getByOrderId(
      @PathVariable UUID orderId) {
    return ResponseEntity.ok(SuccessResponse.of(assignmentService.getByOrderId(orderId)));
  }

  @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'BRANCH_ADMIN', 'SYSTEM_ADMIN')")
  @PostMapping("/manual")
  public ResponseEntity<SuccessResponse<AssignmentResponse>> manualAssign(
      @RequestBody @Valid ManualAssignRequest request) {
    return ResponseEntity.ok(
        SuccessResponse.of(
            assignmentService.manualAssign(request.orderId(), request.agentId(), request.reason()),
            "Assignment created manually"));
  }

  @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'BRANCH_ADMIN', 'SYSTEM_ADMIN')")
  @PostMapping("/{assignmentId}/cancel")
  public ResponseEntity<SuccessResponse<AssignmentResponse>> cancel(
      @PathVariable UUID assignmentId, @RequestBody @Valid CancelAssignmentRequest request) {
    return ResponseEntity.ok(
        SuccessResponse.of(
            assignmentService.cancel(assignmentId, request.reason()), "Assignment cancelled"));
  }
}
