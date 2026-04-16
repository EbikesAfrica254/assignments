package com.ebikes.assignments.support.infrastructure;

import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.ebikes.assignments.support.context.ExecutionContext;
import com.ebikes.assignments.support.fixtures.SecurityFixtures;

public class WithExecutionContext implements BeforeEachCallback, AfterEachCallback {

  @Override
  public void beforeEach(@NonNull ExtensionContext context) {
    ExecutionContext.set(
        SecurityFixtures.TEST_USER_ID,
        SecurityFixtures.TEST_ORGANIZATION_ID,
        null,
        SecurityFixtures.TEST_EMAIL,
        Set.of(),
        SecurityFixtures.TEST_PHONE_NUMBER,
        Set.of());
  }

  @Override
  public void afterEach(@NonNull ExtensionContext context) {
    ExecutionContext.clear();
  }
}
