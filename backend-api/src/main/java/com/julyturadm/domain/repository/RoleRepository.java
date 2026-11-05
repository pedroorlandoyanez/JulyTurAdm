package com.julyturadm.domain.repository;

import com.julyturadm.domain.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);

    //---
    // `JOIN r.userRoles ur`: identifica os papéis associados ao usuário.
    // `LEFT JOIN FETCH r.rolePermissions`: carrega as permissões desses papéis.
    // `LEFT JOIN FETCH rp.permission`: carrega os dados de cada permissão.
    //---
    @Query("""
        SELECT DISTINCT r
        FROM Role r
        JOIN r.userRoles ur
        LEFT JOIN FETCH r.rolePermissions rp
        LEFT JOIN FETCH rp.permission
        WHERE ur.user.email = :email
    """)
    List<Role> findAllByUserEmailWithPermissions(
    @Param("email") String email
    );
}