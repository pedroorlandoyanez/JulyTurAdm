package com.julyturadm.domain.entity;

import com.julyturadm.domain.entity.id.RolePermissionId;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "roles_permissions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RolePermission {

    // - `RolePermissionId` recebe os IDs dos dois registros.
    @EmbeddedId
    private RolePermissionId id;

    // ---
    // - `setRole()` estabelece o relacionamento com o perfil.
    // - `@MapsId` mantém os campos da chave composta sincronizados com os
    //    relacionamentos no momento da persistência.
    // ---
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("roleId")
    @JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "FK_rp_role"))
    private Role role;

    // ---
    // - `setPermission()` estabelece o relacionamento com a permissão.
    // - `@MapsId` mantém os campos da chave composta sincronizados com os
    //    relacionamentos no momento da persistência.
    // ---
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("permissionId")
    @JoinColumn(name = "permission_id", nullable = false, foreignKey = @ForeignKey(name = "FK_rp_permission"))
    private Permission permission;
       
    // ---
    // Esse método permite criar uma role e uma permissao sem expor publicamente o 
    // construtor da entidade.
    // ---    
    public static RolePermission create(
            Role role,
            Permission permission) {

        RolePermission rolePermission = new RolePermission();

        rolePermission.setId(
                new RolePermissionId(
                        role.getId(),
                        permission.getId()));

        rolePermission.setRole(role);
        rolePermission.setPermission(permission);

        return rolePermission;
    }

}