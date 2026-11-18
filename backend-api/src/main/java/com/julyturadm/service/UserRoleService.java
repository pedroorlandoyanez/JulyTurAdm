package com.julyturadm.service;

import com.julyturadm.common.exception.BusinessException;
import com.julyturadm.common.exception.ResourceNotFoundException;
import com.julyturadm.domain.entity.Role;
import com.julyturadm.domain.entity.User;
import com.julyturadm.domain.entity.UserRole;
import com.julyturadm.domain.repository.RoleRepository;
import com.julyturadm.domain.repository.UserRepository;
import com.julyturadm.domain.repository.UserRoleRepository;
import com.julyturadm.dto.response.RoleResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

//---
//O que essa implementação garante?
//- ADMIN pode atribuir e remover qualquer perfil.
//- RH pode atribuir e remover perfis funcionais.
//- RH não pode atribuir nem remover `ADMIN`.
//- Usuários sem essas autoridades não podem realizar essas operações.
//- A consulta dos perfis não permite alterar associações.
//---
@Service
@RequiredArgsConstructor
@Transactional
public class UserRoleService {

    private static final String ADMIN_ROLE = "ADMIN";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    public RoleResponse assignRole(Long userId, Long roleId) {

        User user = findUser(userId);
        Role role = findRole(roleId);

        validateRoleAssignment(role);

        if (userRoleRepository.existsByUser_IdAndRole_Id(userId, roleId)) {
            throw new BusinessException(
                    "Este perfil já está associado ao usuário.");
        }

        UserRole userRole = UserRole.create(user, role);

        userRoleRepository.save(userRole);

        return toResponse(role);
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> findUserRoles(Long userId) {

        findUser(userId);

        return userRoleRepository.findAllByUser_Id(userId)
                .stream()
                .map(userRole -> toResponse(userRole.getRole()))
                .toList();
    }

    public void removeRole(Long userId, Long roleId) {

        findUser(userId);
        Role role = findRole(roleId);

        validateRoleAssignment(role);

        UserRole userRole = userRoleRepository
                .findById(new com.julyturadm.domain.entity.id.UserRoleId(
                        userId, roleId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Associação entre usuário e perfil não encontrada."));

        userRoleRepository.delete(userRole);
    }

    private void validateRoleAssignment(Role role) {

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException(
                    "Usuário não autenticado.");
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        boolean isRh = authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_RH"));

        if (isAdmin) {
            return;
        }

        if (isRh && !ADMIN_ROLE.equals(role.getName())) {
            return;
        }

        throw new AccessDeniedException(
                "Você não tem permissão para atribuir ou remover este perfil.");
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuário não encontrado com o ID: " + userId));
    }

    private Role findRole(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil não encontrado com o ID: " + roleId));
    }

    private RoleResponse toResponse(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getCreatedAt(),
                role.getUpdatedAt());
    }
}