package com.julyturadm.domain.repository;

import com.julyturadm.domain.entity.UserRole;
import com.julyturadm.domain.entity.id.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleRepository
        extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findAllByUser_Id(Long userId);

    List<UserRole> findAllByRole_Id(Long roleId);

    boolean existsByUser_IdAndRole_Id(Long userId, Long roleId);
}