package com.julyturadm.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.julyturadm.dto.response.RolePermissionResponse;
import com.julyturadm.service.RolePermissionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/roles/{roleId}/permissions")
@RequiredArgsConstructor
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RolePermissionResponse>> findPermissionsByRoleId(
            @PathVariable Long roleId) {

        return ResponseEntity.ok(
                rolePermissionService.findPermissionsByRoleId(roleId));
    }

    @PutMapping("/{permissionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> assignPermissionToRole(
            @PathVariable Long roleId,
            @PathVariable Long permissionId) {

        rolePermissionService.assignPermissionToRole(roleId, permissionId);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{permissionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removePermissionFromRole(
            @PathVariable Long roleId,
            @PathVariable Long permissionId) {

        rolePermissionService.removePermissionFromRole(roleId, permissionId);

        return ResponseEntity.noContent().build();
    }
}
