package com.julyturadm.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.julyturadm.common.exception.BusinessException;
import com.julyturadm.common.exception.ResourceNotFoundException;
import com.julyturadm.domain.entity.Permission;
import com.julyturadm.domain.repository.PermissionRepository;
import com.julyturadm.domain.repository.RolePermissionRepository;
import com.julyturadm.dto.request.PermissionCreateRequest;
import com.julyturadm.dto.request.PermissionUpdateRequest;
import com.julyturadm.dto.response.PermissionResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public PermissionResponse createPermission(PermissionCreateRequest request) {

        if (permissionRepository.existsByName(request.name())) {
            throw new BusinessException("Já existe uma permissão com esse nome.");
        }

        Permission permission = Permission.create(request.name());

        Permission savedPermission = permissionRepository.save(permission);

        return toResponse(savedPermission);
    }

    @Transactional(readOnly = true)
    public PermissionResponse findPermissionById(Long id) {

        Permission permission = findPermissionOrThrow(id);

        return toResponse(permission);
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> findAllPermissions() {

        return permissionRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PermissionResponse updatePermission(
            Long id,
            PermissionUpdateRequest request) {

        Permission permission = findPermissionOrThrow(id);

        if (permissionRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new BusinessException("Já existe outra permissão com esse nome.");
        }

        permission.setName(request.name());

        Permission updatedPermission = permissionRepository.save(permission);

        return toResponse(updatedPermission);
    }

    public void deletePermission(Long id) {

        Permission permission = findPermissionOrThrow(id);

        if (rolePermissionRepository.existsByPermission_Id(id)) {
            throw new BusinessException(
                    "Não é possível excluir a permissão: ela está associada a perfis.");
        }

        permissionRepository.delete(permission);
    }

    private Permission findPermissionOrThrow(Long id) {

        return permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Permissão não encontrada: " + id));
    }

    private PermissionResponse toResponse(Permission permission) {

        return new PermissionResponse(
                permission.getId(),
                permission.getName(),
                permission.getCreatedAt(),
                permission.getUpdatedAt());
    }
}
