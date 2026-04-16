package com.ebikes.assignments.support.infrastructure;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@Import(PostgresContainerConfig.class)
@TestConfiguration(proxyBeanMethods = false)
public class IntegrationContainersConfig {

  private static final RabbitMQContainer RABBITMQ =
      new RabbitMQContainer("rabbitmq:4.2.4-management-alpine");

  static {
    RABBITMQ.start();
  }

  @Bean
  @ServiceConnection
  public RabbitMQContainer rabbitMqContainer() {
    return RABBITMQ;
  }
}
