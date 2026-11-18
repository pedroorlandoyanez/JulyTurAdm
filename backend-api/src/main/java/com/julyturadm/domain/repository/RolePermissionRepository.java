package com.julyturadm.domain.repository;

import com.julyturadm.domain.entity.RolePermission;
import com.julyturadm.domain.entity.id.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionRepository
        extends JpaRepository<RolePermission, RolePermissionId> {

    List<RolePermission> findAllByRole_Id(Long roleId);

    List<RolePermission> findAllByPermission_Id(Long permissionId);

    boolean existsByRole_IdAndPermission_Id(
            Long roleId,
            Long permissionId
    );

    // Verifica se o perfil possui permissões associadas.
    boolean existsByRole_Id(Long roleId);
         
    // Verifica se a permissão está associada a algum perfil.
    boolean existsByPermission_Id(Long permissionId);
    
}