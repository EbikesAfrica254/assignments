package com.ebikes.assignments.support.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.ebikes.assignments.enums.UserRole;
import com.ebikes.assignments.support.security.RBACUtilities.RoleTier;

@DisplayName("RBACUtilities")
class RBACUtilitiesTest {

  @Nested
  @DisplayName("hasAnyRole")
  class HasAnyRole {

    @Test
    @DisplayName("should return false when roles is empty")
    void shouldReturnFalseWhenRolesEmpty() {
      assertThat(RBACUtilities.hasAnyRole(Set.of(), UserRole.AGENT)).isFalse();
    }

    @Test
    @DisplayName("should return false when no candidates match")
    void shouldReturnFalseWhenNoCandidatesMatch() {
      assertThat(
              RBACUtilities.hasAnyRole(
                  Set.of(UserRole.CUSTOMER), UserRole.AGENT, UserRole.SYSTEM_ADMIN))
          .isFalse();
    }

    @Test
    @DisplayName("should return true when one candidate matches")
    void shouldReturnTrueWhenOneCandidateMatches() {
      assertThat(RBACUtilities.hasAnyRole(Set.of(UserRole.AGENT), UserRole.AGENT)).isTrue();
    }

    @Test
    @DisplayName("should return true when multiple candidates match")
    void shouldReturnTrueWhenMultipleCandidatesMatch() {
      assertThat(
              RBACUtilities.hasAnyRole(
                  Set.of(UserRole.AGENT, UserRole.SYSTEM_ADMIN),
                  UserRole.AGENT,
                  UserRole.SYSTEM_ADMIN))
          .isTrue();
    }
  }

  @Nested
  @DisplayName("hasSystemAdminRole")
  class HasSystemAdminRole {

    @Test
    @DisplayName("should return false when roles is empty")
    void shouldReturnFalseWhenRolesEmpty() {
      assertThat(RBACUtilities.hasSystemAdminRole(Set.of())).isFalse();
    }

    @Test
    @DisplayName("should return false when SYSTEM_ADMIN is absent")
    void shouldReturnFalseWhenSystemAdminAbsent() {
      assertThat(RBACUtilities.hasSystemAdminRole(Set.of(UserRole.ORGANIZATION_ADMIN))).isFalse();
    }

    @Test
    @DisplayName("should return true when SYSTEM_ADMIN is present")
    void shouldReturnTrueWhenSystemAdminPresent() {
      assertThat(RBACUtilities.hasSystemAdminRole(Set.of(UserRole.SYSTEM_ADMIN))).isTrue();
    }
  }

  @Nested
  @DisplayName("isBranchRole")
  class IsBranchRole {

    static Stream<UserRole> branchRoles() {
      return Stream.of(
          UserRole.BRANCH_ADMIN,
          UserRole.BRANCH_CHECKER,
          UserRole.BRANCH_FLEET_MANAGER,
          UserRole.BRANCH_FLEET_SUPPORT,
          UserRole.BRANCH_INVENTORY_MANAGER,
          UserRole.BRANCH_MAKER,
          UserRole.BRANCH_OPERATOR);
    }

    @Test
    @DisplayName("should return false for CUSTOMER")
    void shouldReturnFalseForCustomer() {
      assertThat(RBACUtilities.isBranchRole(UserRole.CUSTOMER)).isFalse();
    }

    @Test
    @DisplayName("should return false for ORGANIZATION roles")
    void shouldReturnFalseForOrganizationRole() {
      assertThat(RBACUtilities.isBranchRole(UserRole.ORGANIZATION_ADMIN)).isFalse();
    }

    @Test
    @DisplayName("should return false for SYSTEM_ADMIN")
    void shouldReturnFalseForSystemAdmin() {
      assertThat(RBACUtilities.isBranchRole(UserRole.SYSTEM_ADMIN)).isFalse();
    }

    @Test
    @DisplayName("should return true for AGENT")
    void shouldReturnTrueForAgent() {
      assertThat(RBACUtilities.isBranchRole(UserRole.AGENT)).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("should return true for all BRANCH_ roles")
    @MethodSource("branchRoles")
    void shouldReturnTrueForAllBranchRoles(UserRole role) {
      assertThat(RBACUtilities.isBranchRole(role)).isTrue();
    }
  }

  @Nested
  @DisplayName("isOrganizationRole")
  class IsOrganizationRole {

    static Stream<UserRole> organizationRoles() {
      return Stream.of(
          UserRole.ORGANIZATION_ADMIN,
          UserRole.ORGANIZATION_CHECKER,
          UserRole.ORGANIZATION_FLEET_MANAGER,
          UserRole.ORGANIZATION_FLEET_SUPPORT,
          UserRole.ORGANIZATION_INVENTORY_MANAGER,
          UserRole.ORGANIZATION_MAKER,
          UserRole.ORGANIZATION_OPERATOR);
    }

    @Test
    @DisplayName("should return false for AGENT")
    void shouldReturnFalseForAgent() {
      assertThat(RBACUtilities.isOrganizationRole(UserRole.AGENT)).isFalse();
    }

    @Test
    @DisplayName("should return false for BRANCH roles")
    void shouldReturnFalseForBranchRole() {
      assertThat(RBACUtilities.isOrganizationRole(UserRole.BRANCH_ADMIN)).isFalse();
    }

    @Test
    @DisplayName("should return false for CUSTOMER")
    void shouldReturnFalseForCustomer() {
      assertThat(RBACUtilities.isOrganizationRole(UserRole.CUSTOMER)).isFalse();
    }

    @Test
    @DisplayName("should return false for SYSTEM_ADMIN")
    void shouldReturnFalseForSystemAdmin() {
      assertThat(RBACUtilities.isOrganizationRole(UserRole.SYSTEM_ADMIN)).isFalse();
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("should return true for all ORGANIZATION_ roles")
    @MethodSource("organizationRoles")
    void shouldReturnTrueForAllOrganizationRoles(UserRole role) {
      assertThat(RBACUtilities.isOrganizationRole(role)).isTrue();
    }
  }

  @Nested
  @DisplayName("parseRoles")
  class ParseRoles {

    @Test
    @DisplayName("should filter out unrecognized role strings")
    void shouldFilterOutUnrecognizedRoles() {
      assertThat(RBACUtilities.parseRoles(Set.of("UNKNOWN_ROLE", "INVALID"))).isEmpty();
    }

    @Test
    @DisplayName("should handle mixed valid and invalid role strings")
    void shouldHandleMixedValidAndInvalidStrings() {
      Set<UserRole> result = RBACUtilities.parseRoles(Set.of("AGENT", "INVALID"));
      assertThat(result).containsExactly(UserRole.AGENT);
    }

    @Test
    @DisplayName("should map valid role name strings to enum values")
    void shouldMapValidRoleStrings() {
      Set<UserRole> result = RBACUtilities.parseRoles(Set.of("AGENT", "SYSTEM_ADMIN"));
      assertThat(result).containsExactlyInAnyOrder(UserRole.AGENT, UserRole.SYSTEM_ADMIN);
    }

    @Test
    @DisplayName("should return empty set for empty input")
    void shouldReturnEmptySetForEmptyInput() {
      assertThat(RBACUtilities.parseRoles(Set.of())).isEmpty();
    }
  }

  @Nested
  @DisplayName("resolveRoleTier")
  class ResolveRoleTier {

    @Test
    @DisplayName("should return AGENT when AGENT present without SYSTEM_ADMIN")
    void shouldReturnAgentWhenAgentPresent() {
      assertThat(RBACUtilities.resolveRoleTier(Set.of(UserRole.AGENT))).isEqualTo(RoleTier.AGENT);
    }

    @Test
    @DisplayName("should return BRANCH when only BRANCH roles present")
    void shouldReturnBranchWhenOnlyBranchRolesPresent() {
      assertThat(RBACUtilities.resolveRoleTier(Set.of(UserRole.BRANCH_ADMIN)))
          .isEqualTo(RoleTier.BRANCH);
    }

    @Test
    @DisplayName("should return ORGANIZATION when ORGANIZATION role present without higher tier")
    void shouldReturnOrganizationWhenOrganizationRolePresent() {
      assertThat(RBACUtilities.resolveRoleTier(Set.of(UserRole.ORGANIZATION_ADMIN)))
          .isEqualTo(RoleTier.ORGANIZATION);
    }

    @Test
    @DisplayName("should return SYSTEM when SYSTEM_ADMIN present alongside other roles")
    void shouldReturnSystemWhenSystemAdminPresentAlongsideOtherRoles() {
      assertThat(
              RBACUtilities.resolveRoleTier(
                  Set.of(UserRole.SYSTEM_ADMIN, UserRole.AGENT, UserRole.ORGANIZATION_ADMIN)))
          .isEqualTo(RoleTier.SYSTEM);
    }

    @Test
    @DisplayName("should return SYSTEM when only SYSTEM_ADMIN present")
    void shouldReturnSystemWhenSystemAdminPresent() {
      assertThat(RBACUtilities.resolveRoleTier(Set.of(UserRole.SYSTEM_ADMIN)))
          .isEqualTo(RoleTier.SYSTEM);
    }

    @Test
    @DisplayName("should return SYSTEM over AGENT when both present")
    void shouldReturnSystemOverAgent() {
      assertThat(RBACUtilities.resolveRoleTier(Set.of(UserRole.SYSTEM_ADMIN, UserRole.AGENT)))
          .isEqualTo(RoleTier.SYSTEM);
    }

    @Test
    @DisplayName("should return UNKNOWN for empty roles")
    void shouldReturnUnknownForEmptyRoles() {
      assertThat(RBACUtilities.resolveRoleTier(Set.of())).isEqualTo(RoleTier.UNKNOWN);
    }

    @Test
    @DisplayName("should return UNKNOWN for unrecognized tier roles like CUSTOMER")
    void shouldReturnUnknownForCustomer() {
      assertThat(RBACUtilities.resolveRoleTier(Set.of(UserRole.CUSTOMER)))
          .isEqualTo(RoleTier.UNKNOWN);
    }
  }
}
