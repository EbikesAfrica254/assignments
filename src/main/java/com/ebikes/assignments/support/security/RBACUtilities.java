package com.ebikes.assignments.support.security;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.ebikes.assignments.enums.UserRole;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RBACUtilities {

  private static final Set<UserRole> ADMIN_ROLES =
      Set.of(UserRole.SYSTEM_ADMIN, UserRole.ORGANIZATION_ADMIN, UserRole.BRANCH_ADMIN);

  public static boolean hasAdminRole(Set<UserRole> roles) {
    return roles.stream().anyMatch(ADMIN_ROLES::contains);
  }

  public static boolean hasAdminRoleFromNames(Set<String> roleNames) {
    return hasAdminRole(parseRoles(roleNames));
  }

  public static Set<UserRole> parseRoles(Set<String> roleNames) {
    return roleNames.stream()
        .map(UserRole::fromString)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }
}
