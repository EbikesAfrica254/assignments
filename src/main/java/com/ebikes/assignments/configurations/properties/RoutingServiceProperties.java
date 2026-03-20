package com.ebikes.assignments.configurations.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "services.routing")
@Getter
@Setter
public class RoutingServiceProperties {

  private String baseUrl;
}
