package com.julyturadm.domain.repository;

import com.julyturadm.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAllByEnabledTrue();

    // ----------------------------------------------------------------------------------
    // Por que essa consulta?
    // - Recupera o usuário pelo e-mail.
    // - Carrega os papéis associados.
    // - Carrega as permissões de cada papel.
    // - Usa `LEFT JOIN FETCH` para não excluir usuários que ainda não possuem
    // papéis.
    // - Usa `DISTINCT` para evitar usuários duplicados no resultado.
    // ----------------------------------------------------------------------------------
    // @Query("""
    // SELECT DISTINCT u
    // FROM User u
    // LEFT JOIN FETCH u.userRoles ur
    // LEFT JOIN FETCH ur.role r
    // LEFT JOIN FETCH r.rolePermissions rp
    // LEFT JOIN FETCH rp.permission
    // WHERE u.email = :email
    // """)
    // Optional<User> findByEmailWithRolesAndPermissions(
    // @Param("email") String email
    // );
    @Query("""
        SELECT DISTINCT u
        FROM User u
        LEFT JOIN FETCH u.userRoles ur
        LEFT JOIN FETCH ur.role
        WHERE u.email = :email
        """)
    Optional<User> findByEmailWithRoles(
    @Param("email") String email);
}
