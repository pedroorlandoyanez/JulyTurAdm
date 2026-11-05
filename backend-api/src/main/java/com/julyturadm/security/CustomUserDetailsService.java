package com.julyturadm.security;

import com.julyturadm.domain.entity.Role;
import com.julyturadm.domain.entity.User;
import com.julyturadm.domain.repository.RoleRepository;
import com.julyturadm.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Integra o usuário ao Spring Security.
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        // 1. Busca o usuário e carrega seus papéis.
        User user = userRepository
                .findByEmailWithRoles(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Credenciais inválidas."
                ));

        // 2. Impede autenticação de usuário desativado.
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new UsernameNotFoundException(
                    "Credenciais inválidas."
            );
        }

        // 3. Busca os papéis e suas permissões.
        List<Role> roles = roleRepository
                .findAllByUserEmailWithPermissions(email);

        // 4. Constrói as autoridades do usuário.
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();

        for (Role role : roles) {

            // Papel funcional.
            authorities.add(
                    new SimpleGrantedAuthority("ROLE_" + role.getName())
            );

            // Permissões granulares.
            role.getRolePermissions().forEach(rolePermission -> {

                String permissionName =
                        rolePermission.getPermission().getName();

                authorities.add(
                        new SimpleGrantedAuthority(permissionName)
                );
            });
        }

        // 5. Retorna o usuário para o Spring Security.
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .disabled(false)
                .build();
    }
}
