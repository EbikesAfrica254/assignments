package com.ebikes.assignments.configurations.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "services.assignments")
@Getter
@Setter
public class ServiceProperties {

  private Candidates candidates = new Candidates();
  private Offers offers = new Offers();

  @Data
  public static class Candidates {
    private Integer broadcastMax;
    private Integer shortlistMax;
  }

  @Data
  public static class Offers {
    private Integer expiryMaxAttempts;
    private Integer expiryMinutes;
  }
}
