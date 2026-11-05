package com.julyturadm.security;

import com.julyturadm.domain.entity.User;
import com.julyturadm.domain.repository.RoleRepository;
import com.julyturadm.domain.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.julyturadm.domain.entity.Permission;
import com.julyturadm.domain.entity.Role;
import com.julyturadm.domain.entity.RolePermission;

class CustomUserDetailsServiceTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);

        userDetailsService = new CustomUserDetailsService(
                userRepository,
                roleRepository);
    }

    @Test
    void shouldRejectUserThatDoesNotExist() {

        when(userRepository.findByEmailWithRoles("inexistente@email.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(
                        "inexistente@email.com"));

        verifyNoInteractions(roleRepository);
    }

    @Test
    void shouldRejectDisabledUser() {

        User user = mock(User.class);

        when(userRepository.findByEmailWithRoles("desativado@email.com"))
                .thenReturn(Optional.of(user));

        when(user.getEnabled()).thenReturn(false);

        assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(
                        "desativado@email.com"));

        verifyNoInteractions(roleRepository);
    }

    @Test
    void shouldLoadActiveUserWithoutRoles() {

        User user = mock(User.class);

        when(userRepository.findByEmailWithRoles("semperfil@email.com"))
                .thenReturn(Optional.of(user));

        when(user.getEnabled()).thenReturn(true);
        when(user.getEmail()).thenReturn("semperfil@email.com");
        when(user.getPassword()).thenReturn("$2a$10$senhaCriptografada");

        when(roleRepository.findAllByUserEmailWithPermissions(
                "semperfil@email.com")).thenReturn(List.of());

        UserDetails userDetails = userDetailsService.loadUserByUsername("semperfil@email.com");

        assertEquals("semperfil@email.com", userDetails.getUsername());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.getAuthorities().isEmpty());
    }
      
    @Test
    void shouldLoadActiveUserWithRolesAndPermissions() {

        // Usuário ativo.
        User user = mock(User.class);

        when(userRepository.findByEmailWithRoles("admin@email.com"))
                .thenReturn(Optional.of(user));

        when(user.getEnabled()).thenReturn(true);
        when(user.getEmail()).thenReturn("admin@email.com");
        when(user.getPassword()).thenReturn("$2a$10$senhaCriptografada");

        // Perfil funcional ADMIN.
        Role role = mock(Role.class);
        when(role.getName()).thenReturn("ADMIN");

        // Primeira permissão.
        Permission permissionCreate = mock(Permission.class);
        when(permissionCreate.getName()).thenReturn("USER_CREATE");

        RolePermission rolePermissionCreate = mock(RolePermission.class);
        when(rolePermissionCreate.getPermission())
                .thenReturn(permissionCreate);

        // Segunda permissão.
        Permission permissionUpdate = mock(Permission.class);
        when(permissionUpdate.getName()).thenReturn("USER_UPDATE");

        RolePermission rolePermissionUpdate = mock(RolePermission.class);
        when(rolePermissionUpdate.getPermission())
                .thenReturn(permissionUpdate);

        when(role.getRolePermissions()).thenReturn(
                List.of(rolePermissionCreate, rolePermissionUpdate));

        when(roleRepository.findAllByUserEmailWithPermissions(
                "admin@email.com")).thenReturn(List.of(role));

        // Execução.
        UserDetails userDetails = userDetailsService.loadUserByUsername("admin@email.com");

        // Verificações.
        assertEquals("admin@email.com", userDetails.getUsername());

        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("USER_CREATE")));

        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("USER_UPDATE")));

        assertEquals(3, userDetails.getAuthorities().size());
    }

}
