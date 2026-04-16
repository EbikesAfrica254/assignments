package com.ebikes.assignments.listeners.internal;

import jakarta.persistence.OptimisticLockException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractInternalEventHandler<T> {

  public final void handle(T trigger) {
    try {
      process(trigger);
    } catch (OptimisticLockException e) {
      log.warn(
          "Optimistic lock conflict — concurrent modification detected, skipping: trigger={}",
          trigger);
    }
  }

  protected abstract void process(T trigger);
}
