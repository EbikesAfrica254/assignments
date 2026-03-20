package com.ebikes.assignments.services.assignments.strategies;

import com.ebikes.assignments.database.entities.Assignment;
import com.ebikes.assignments.database.entities.OrderContext;
import com.ebikes.assignments.enums.AssignmentStrategy;

public interface AssignmentStrategyExecutor {

  boolean execute(Assignment assignment, OrderContext orderContext);

  AssignmentStrategy supports();
}
