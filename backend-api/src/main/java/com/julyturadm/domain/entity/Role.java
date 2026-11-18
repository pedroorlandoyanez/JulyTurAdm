package com.julyturadm.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;    

    @OneToMany(
        mappedBy = "role",
        fetch = FetchType.LAZY
    )
    private List<UserRole> userRoles = new ArrayList<>();

    @OneToMany(
        mappedBy = "role",
        fetch = FetchType.LAZY
    )
    private List<RolePermission> rolePermissions = new ArrayList<>();

    // Esse método permite criar um perfil sem expor publicamente o construtor da entidade.
    public static Role create(String name) {

        Role role = new Role();

        role.setName(name);

        return role;
    }

}
