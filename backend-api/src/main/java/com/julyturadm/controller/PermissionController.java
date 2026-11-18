package com.julyturadm.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.julyturadm.dto.request.PermissionCreateRequest;
import com.julyturadm.dto.request.PermissionUpdateRequest;
import com.julyturadm.dto.response.PermissionResponse;
import com.julyturadm.service.PermissionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PermissionResponse> createPermission(
            @Valid @RequestBody PermissionCreateRequest request) {

        PermissionResponse response =
                permissionService.createPermission(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PermissionResponse> findPermissionById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                permissionService.findPermissionById(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PermissionResponse>> findAllPermissions() {

        return ResponseEntity.ok(
                permissionService.findAllPermissions());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PermissionResponse> updatePermission(
            @PathVariable Long id,
            @Valid @RequestBody PermissionUpdateRequest request) {

        return ResponseEntity.ok(
                permissionService.updatePermission(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePermission(
            @PathVariable Long id) {

        permissionService.deletePermission(id);

        return ResponseEntity.noContent().build();
    }
}
