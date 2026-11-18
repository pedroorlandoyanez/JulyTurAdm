package com.julyturadm.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.julyturadm.domain.entity.Role;
import com.julyturadm.domain.repository.RoleRepository;
import com.julyturadm.domain.repository.RolePermissionRepository;
import com.julyturadm.domain.repository.UserRoleRepository;
import com.julyturadm.dto.request.RoleCreateRequest;
import com.julyturadm.dto.request.RoleUpdateRequest;
import com.julyturadm.dto.response.RoleResponse;
import com.julyturadm.common.exception.BusinessException;
import com.julyturadm.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public RoleResponse createRole(RoleCreateRequest request) {

        if (roleRepository.existsByName(request.name())) {
            throw new BusinessException(
                    "Já existe um perfil com esse nome.");
        }

        Role role = Role.create(request.name());

        Role savedRole = roleRepository.save(role);

        return toResponse(savedRole);
    }

    @Transactional(readOnly = true)
    public RoleResponse findRoleById(Long id) {

        Role role = findRoleOrThrow(id);

        return toResponse(role);
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> findAllRoles() {

        return roleRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public RoleResponse updateRole(Long id, RoleUpdateRequest request) {

        Role role = findRoleOrThrow(id);

        if (roleRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new BusinessException(
                    "Já existe outro perfil com esse nome.");
        }

        role.setName(request.name());

        Role updatedRole = roleRepository.save(role);

        return toResponse(updatedRole);
    }

    public void deleteRole(Long id) {

        Role role = findRoleOrThrow(id);

        if (userRoleRepository.existsByRole_Id(id)) {
            throw new BusinessException(
                    "Não é possível excluir o perfil: ele está associado a usuários.");
        }

        if (rolePermissionRepository.existsByRole_Id(id)) {
            throw new BusinessException(
                    "Não é possível excluir o perfil: ele possui permissões associadas.");
        }

        roleRepository.delete(role);
    }

    private Role findRoleOrThrow(Long id) {

        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil não encontrado: " + id));
    }

    private RoleResponse toResponse(Role role) {

        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getCreatedAt(),
                role.getUpdatedAt());
    }
}
