package com.julyturadm.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.julyturadm.dto.request.RoleCreateRequest;
import com.julyturadm.dto.request.RoleUpdateRequest;
import com.julyturadm.dto.response.RoleResponse;
import com.julyturadm.service.RoleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    //---
    // Todos os metodos restritos ao ADMIN
    // O uso de `hasRole('ADMIN')` está alinhado ao padrão do Spring Security:
    // ele verifica a autoridade `ROLE_ADMIN`.
    //---

    // Cria um novo perfil.
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoleResponse> createRole(
            @Valid @RequestBody RoleCreateRequest request) {

        RoleResponse response = roleService.createRole(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Consulta um perfil pelo ID.
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoleResponse> findRoleById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                roleService.findRoleById(id));
    }

    // Lista todos os perfis.
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RoleResponse>> findAllRoles() {

        return ResponseEntity.ok(
                roleService.findAllRoles());
    }

    // Atualiza o nome de um perfil.
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoleResponse> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleUpdateRequest request) {

        return ResponseEntity.ok(
                roleService.updateRole(id, request));
    }

    // Exclui um perfil, respeitando as regras de integridade.
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRole(
            @PathVariable Long id) {

        roleService.deleteRole(id);

        return ResponseEntity.noContent().build();
    }
}
