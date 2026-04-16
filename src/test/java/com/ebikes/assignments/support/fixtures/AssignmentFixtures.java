package com.ebikes.assignments.support.fixtures;

import com.ebikes.assignments.database.entities.Assignment;
import com.ebikes.assignments.database.entities.OrderContext;
import com.ebikes.assignments.enums.AssignmentStatus;
import com.ebikes.assignments.enums.AssignmentStrategy;

public final class AssignmentFixtures {

  private AssignmentFixtures() {}

  public static Assignment awaitingResponse(OrderContext orderContext) {
    return base(orderContext).build();
  }

  public static Assignment cancelled(OrderContext orderContext) {
    Assignment assignment = base(orderContext).build();
    assignment.cancel("Cancelled by test");
    return assignment;
  }

  public static Assignment failed(OrderContext orderContext) {
    Assignment assignment = base(orderContext).build();
    assignment.fail("Failed by test");
    return assignment;
  }

  public static Assignment succeeded(OrderContext orderContext) {
    Assignment assignment = base(orderContext).build();
    assignment.succeed("agent-001");
    return assignment;
  }

  public static Assignment withStrategy(OrderContext orderContext, AssignmentStrategy strategy) {
    return base(orderContext).strategy(strategy).build();
  }

  public static Assignment withStrategyAwaitingResponse(
      OrderContext orderContext, AssignmentStrategy strategy) {
    return base(orderContext).strategy(strategy).status(AssignmentStatus.AWAITING_RESPONSE).build();
  }

  private static Assignment.AssignmentBuilder<?, ?> base(OrderContext orderContext) {
    return Assignment.builder()
        .status(AssignmentStatus.AWAITING_RESPONSE)
        .orderContext(orderContext)
        .strategy(AssignmentStrategy.PREASSIGNED);
  }
}
