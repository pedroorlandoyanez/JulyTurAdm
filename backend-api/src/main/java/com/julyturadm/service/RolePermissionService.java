package com.julyturadm.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.julyturadm.common.exception.BusinessException;
import com.julyturadm.common.exception.ResourceNotFoundException;
import com.julyturadm.domain.entity.Permission;
import com.julyturadm.domain.entity.Role;
import com.julyturadm.domain.entity.RolePermission;
import com.julyturadm.domain.repository.PermissionRepository;
import com.julyturadm.domain.repository.RolePermissionRepository;
import com.julyturadm.domain.repository.RoleRepository;
import com.julyturadm.dto.response.RolePermissionResponse;

import lombok.RequiredArgsConstructor;

import com.julyturadm.domain.entity.id.RolePermissionId;

@Service
@RequiredArgsConstructor
@Transactional
public class RolePermissionService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Transactional(readOnly = true)
    public List<RolePermissionResponse> findPermissionsByRoleId(Long roleId) {

        Role role = findRoleOrThrow(roleId);

        return rolePermissionRepository.findAllByRole_Id(roleId)
                .stream()
                .map(rolePermission -> toResponse(role, rolePermission.getPermission()))
                .toList();
    }

    public void assignPermissionToRole(Long roleId, Long permissionId) {

        Role role = findRoleOrThrow(roleId);

        Permission permission = findPermissionOrThrow(permissionId);

        if (rolePermissionRepository.existsByRole_IdAndPermission_Id(
                roleId, permissionId)) {

            throw new BusinessException(
                    "Essa permissão já está associada ao perfil.");
        }

        RolePermission rolePermission =
                RolePermission.create(role, permission);

        rolePermissionRepository.save(rolePermission);
    }

    public void removePermissionFromRole(Long roleId, Long permissionId) {

        Role role = findRoleOrThrow(roleId);

        Permission permission = findPermissionOrThrow(permissionId);

        RolePermissionId id =
                new RolePermissionId(roleId, permissionId);

        RolePermission rolePermission = rolePermissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Associação entre perfil e permissão não encontrada."));

        rolePermissionRepository.delete(rolePermission);
    }

    private Role findRoleOrThrow(Long roleId) {

        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil não encontrado: " + roleId));
    }

    private Permission findPermissionOrThrow(Long permissionId) {

        return permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Permissão não encontrada: " + permissionId));
    }

    private RolePermissionResponse toResponse(
            Role role,
            Permission permission) {

        return new RolePermissionResponse(
                role.getId(),
                role.getName(),
                permission.getId(),
                permission.getName());
    }
}
