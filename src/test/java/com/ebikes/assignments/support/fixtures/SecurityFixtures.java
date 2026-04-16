package com.ebikes.assignments.support.fixtures;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.ebikes.assignments.enums.UserRole;
import com.ebikes.assignments.support.context.ExecutionContext;

public final class SecurityFixtures {

  public static final String TEST_USER_ID = "00000000-0000-0000-0000-000000000001";
  public static final String TEST_ORGANIZATION_ID = "00000000-0000-0000-0000-000000000002";
  public static final String TEST_EMAIL = "test@ebikes.test";
  public static final String TEST_PHONE_NUMBER = "+254700000001";
  public static final String OTHER_ORGANIZATION_ID = "00000000-0000-0000-0000-000000000099";
  public static final String OTHER_USER_ID = "00000000-0000-0000-0000-000000000097";
  public static final String OTHER_EMAIL = "other@other.test";

  private SecurityFixtures() {}

  public static void setExecutionContext(
      String activeOrganization, String activeBranch, UserRole... roles) {
    ExecutionContext.set(
        TEST_USER_ID,
        activeOrganization,
        activeBranch,
        TEST_EMAIL,
        Set.of(),
        TEST_PHONE_NUMBER,
        Arrays.stream(roles).map(UserRole::name).collect(Collectors.toSet()));
  }

  public static void setExecutionContextWithRecipients(
      String activeOrganization, String email, String phoneNumber, UserRole... roles) {
    ExecutionContext.set(
        TEST_USER_ID,
        activeOrganization,
        null,
        email,
        Set.of(),
        phoneNumber,
        Arrays.stream(roles).map(UserRole::name).collect(Collectors.toSet()));
  }

  public static Jwt jwt(UserRole... roles) {
    Instant now = Instant.now();

    return Jwt.withTokenValue("test-token")
        .header("alg", "none")
        .subject(TEST_USER_ID)
        .issuedAt(now)
        .expiresAt(now.plusSeconds(3600))
        .claim("email", TEST_EMAIL)
        .claim("phone_number", TEST_PHONE_NUMBER)
        .claim("active_organization", TEST_ORGANIZATION_ID)
        .claim("active_branch", null)
        .claim("groups", List.of())
        .claim("realm_access", Map.of("roles", Arrays.stream(roles).map(UserRole::name).toList()))
        .claim("scope", "openid email profile")
        .build();
  }

  public static Jwt jwt(
      String userId,
      String activeOrganization,
      String activeBranch,
      List<String> groups,
      Map<String, Object> realmAccess) {
    Instant now = Instant.now();

    Jwt.Builder builder =
        Jwt.withTokenValue("test-token")
            .header("alg", "none")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(3600))
            .claim("email", TEST_EMAIL)
            .claim("phone_number", TEST_PHONE_NUMBER)
            .claim("active_organization", activeOrganization)
            .claim("active_branch", activeBranch)
            .claim("groups", groups)
            .claim("scope", "openid email profile");

    if (userId != null) {
      builder.subject(userId);
    }

    if (realmAccess != null) {
      builder.claim("realm_access", realmAccess);
    }

    return builder.build();
  }

  public static JwtAuthenticationToken authentication(
      String userId,
      String activeOrganization,
      String activeBranch,
      List<String> groups,
      Map<String, Object> realmAccess) {
    return new JwtAuthenticationToken(
        jwt(userId, activeOrganization, activeBranch, groups, realmAccess));
  }

  public static JwtRequestPostProcessor authenticatedJwt() {
    return authenticatedJwt(UserRole.ORGANIZATION_ADMIN);
  }

  public static JwtRequestPostProcessor authenticatedJwt(UserRole... roles) {
    return SecurityMockMvcRequestPostProcessors.jwt()
        .jwt(jwt(roles))
        .authorities(
            Arrays.stream(roles)
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .collect(Collectors.toList()));
  }

  public static RequestPostProcessor anonymous() {
    return SecurityMockMvcRequestPostProcessors.anonymous();
  }
}
