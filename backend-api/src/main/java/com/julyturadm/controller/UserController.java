package com.julyturadm.controller;

import java.util.List;

import com.julyturadm.dto.request.UserCreateRequest;
import com.julyturadm.dto.response.UserResponse;
import com.julyturadm.service.UserService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;

import com.julyturadm.dto.response.RoleResponse;
import com.julyturadm.service.UserRoleService;

//---
//- Usuário autenticado sem `USER_CREATE` não poderá criar usuários.
//- Usuário com `USER_READ` poderá consultar usuários.
//- Usuário sem `USER_DISABLE` não poderá desativar contas.
//- Usuário sem `USER_ENABLE` não poderá reativar contas.
//- Requisições sem autenticação continuarão sendo rejeitadas.
//---
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private final UserRoleService userRoleService;

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserCreateRequest request) {

        UserResponse response = userService.createUser(request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<UserResponse> findUserById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
            userService.findUserById(id)
        );
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<UserResponse> findUserByEmail(
            @PathVariable String email) {

        return ResponseEntity.ok(
            userService.findUserByEmail(email)
        );
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<List<UserResponse>> findAllActiveUsers() {

        return ResponseEntity.ok(
            userService.findAllActiveUsers()
        );
    }

    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('USER_DISABLE')")
    public ResponseEntity<UserResponse> disableUser(
            @PathVariable Long id) {

        return ResponseEntity.ok(
            userService.disableUser(id)
        );
    }

    @PatchMapping("/{id}/enable")
    @PreAuthorize("hasAuthority('USER_ENABLE')")
    public ResponseEntity<UserResponse> enableUser(
            @PathVariable Long id) {

        return ResponseEntity.ok(
            userService.enableUser(id)
        );
    }

    //---
    // Por que usamos duas verificações de segurança?
    // 1. @PreAuthorize no controller permite que somente ADMIN e RH acessem as operações
    //    de atribuição e remoção.
    // 2. validateRoleAssignment() no UserRoleService verifica o perfil do usuário autenticado
    //    e impede especificamente que RH atribua ou remova ADMIN.
    //---
    @GetMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<List<RoleResponse>> findUserRoles(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
            userRoleService.findUserRoles(userId)
        );
    }

    //
    @PutMapping("/{userId}/roles/{roleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public ResponseEntity<RoleResponse> assignRole(
            @PathVariable Long userId,
            @PathVariable Long roleId) {

        return ResponseEntity.ok(
            userRoleService.assignRole(userId, roleId)
        );
    }

    // 
    @DeleteMapping("/{userId}/roles/{roleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public ResponseEntity<Void> removeRole(
            @PathVariable Long userId,
            @PathVariable Long roleId) {

        userRoleService.removeRole(userId, roleId);

        return ResponseEntity.noContent().build();
    }    
}
