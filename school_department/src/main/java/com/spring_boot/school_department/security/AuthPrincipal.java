package com.spring_boot.school_department.security;

import com.spring_boot.school_department.enums.Role;

public record AuthPrincipal(
        Long accountId,
        String username,
        Role role,
        String jti,
        String sessionId
) {
}
