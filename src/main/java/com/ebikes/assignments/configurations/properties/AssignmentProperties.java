package com.ebikes.assignments.configurations.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "assignments")
@Getter
@Setter
public class AssignmentProperties {

  private int broadcastMaxCandidates = 5;
  private int offerExpiryMaxAttempts = 3;
  private int offerExpiryMinutes = 10;
  private int shortlistMaxCandidates = 20;
}
