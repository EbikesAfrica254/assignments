package com.ebikes.assignments.support.security;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.ebikes.assignments.enums.UserRole;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class RBACUtilities {

  public enum RoleTier {
    AGENT,
    BRANCH,
    ORGANIZATION,
    SYSTEM,
    UNKNOWN
  }

  public static boolean hasAnyRole(Set<UserRole> roles, UserRole... candidates) {
    for (UserRole candidate : candidates) {
      if (roles.contains(candidate)) {
        return true;
      }
    }
    return false;
  }

  public static boolean hasSystemAdminRole(Set<UserRole> roles) {
    return roles.contains(UserRole.SYSTEM_ADMIN);
  }

  public static boolean isBranchRole(UserRole role) {
    return role.name().startsWith("BRANCH_") || role == UserRole.AGENT;
  }

  public static boolean isOrganizationRole(UserRole role) {
    return role.name().startsWith("ORGANIZATION_");
  }

  public static Set<UserRole> parseRoles(Set<String> roleNames) {
    return roleNames.stream()
        .map(UserRole::fromString)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  public static RoleTier resolveRoleTier(Set<UserRole> roles) {
    if (roles.contains(UserRole.SYSTEM_ADMIN)) {
      return RoleTier.SYSTEM;
    }
    if (roles.contains(UserRole.AGENT)) {
      return RoleTier.AGENT;
    }
    if (roles.stream().anyMatch(RBACUtilities::isOrganizationRole)) {
      return RoleTier.ORGANIZATION;
    }
    if (roles.stream().anyMatch(RBACUtilities::isBranchRole)) {
      return RoleTier.BRANCH;
    }
    return RoleTier.UNKNOWN;
  }
}
