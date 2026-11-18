package com.julyturadm.dto.response;

public record RolePermissionResponse(
        Long roleId,
        String roleName,
        Long permissionId,
        String permissionName
) {
}
