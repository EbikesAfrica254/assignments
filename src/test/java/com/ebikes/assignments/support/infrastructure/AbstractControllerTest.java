package com.ebikes.assignments.support.infrastructure;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.ebikes.assignments.configurations.properties.SecurityProperties;
import com.ebikes.assignments.configurations.security.SecurityConfiguration;
import com.ebikes.assignments.enums.UserRole;
import com.ebikes.assignments.support.fixtures.SecurityFixtures;

import tools.jackson.databind.ObjectMapper;

@ActiveProfiles("test")
@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith({MockitoExtension.class, WithExecutionContext.class})
@Import({
  AbstractControllerTest.TestConfig.class,
  SecurityConfiguration.class,
})
@Tag("controller")
public abstract class AbstractControllerTest {

  @Autowired ObjectMapper objectMapper;
  @MockitoBean protected JwtDecoder jwtDecoder;

  @TestConfiguration
  static class TestConfig {

    @Bean
    public SecurityProperties securityProperties() {
      SecurityProperties props = new SecurityProperties();
      props.setPublicEndpoints(List.of("/actuator/health", "/actuator/prometheus"));
      props.setScopes(List.of("email", "openid", "profile"));
      return props;
    }
  }

  protected JwtRequestPostProcessor authenticatedJwt() {
    return SecurityFixtures.authenticatedJwt();
  }

  protected JwtRequestPostProcessor authenticatedJwt(UserRole... role) {
    return SecurityFixtures.authenticatedJwt(role);
  }

  protected RequestPostProcessor anonymous() {
    return SecurityFixtures.anonymous();
  }

  protected String toJson(Object value) {
    return objectMapper.writeValueAsString(value);
  }
}
