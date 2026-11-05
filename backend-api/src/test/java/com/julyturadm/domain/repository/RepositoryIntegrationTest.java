package com.julyturadm.domain.repository;

import com.julyturadm.config.TestDatabaseSafetyConfiguration;
import com.julyturadm.domain.entity.Permission;
import com.julyturadm.domain.entity.Role;
import com.julyturadm.domain.entity.RolePermission;
import com.julyturadm.domain.entity.User;
import com.julyturadm.domain.entity.UserRole;
import com.julyturadm.domain.entity.id.RolePermissionId;
import com.julyturadm.domain.entity.id.UserRoleId;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestDatabaseSafetyConfiguration.class)
@Transactional
class RepositoryIntegrationTest {

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private RoleRepository roleRepository;

        @Autowired
        private PermissionRepository permissionRepository;

        @Autowired
        private UserRoleRepository userRoleRepository;

        @Autowired
        private RolePermissionRepository rolePermissionRepository;

        @Autowired
        private EntityManager entityManager;

        private <T> T createEntity(Class<T> type) throws Exception {
                Constructor<T> constructor = type.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance();
        }

        private String uniqueValue(String prefix) {
                return prefix + "_" + UUID.randomUUID();
        }

        @Test
        void shouldConnectOnlyToTestDatabase() {

                String database = jdbcTemplate.queryForObject(
                                "SELECT DATABASE()",
                                String.class);

                assertEquals("bd_paytur_test", database);
        }

        @Test
        void shouldPersistAndFindUserByEmail() throws Exception {

                User user = createEntity(User.class);

                String email = uniqueValue("user") + "@test.com";

                user.setName("Usuario Teste");
                user.setEmail(email);
                user.setPassword("password-test");
                user.setEnabled(true);

                User savedUser = userRepository.saveAndFlush(user);

                assertNotNull(savedUser.getId());

                assertTrue(userRepository.existsByEmail(email));

                User foundUser = userRepository.findByEmail(email)
                                .orElseThrow();

                assertEquals(email, foundUser.getEmail());
                assertEquals("Usuario Teste", foundUser.getName());
                assertTrue(foundUser.getEnabled());

                assertTrue(
                                userRepository.findAllByEnabledTrue()
                                                .stream()
                                                .anyMatch(item -> item.getId().equals(savedUser.getId())));
        }

        @Test
        void shouldPersistAndFindRole() throws Exception {

                Role role = createEntity(Role.class);

                String roleName = uniqueValue("ROLE_TEST");

                role.setName(roleName);

                Role savedRole = roleRepository.saveAndFlush(role);

                assertNotNull(savedRole.getId());
                assertTrue(roleRepository.existsByName(roleName));

                Role foundRole = roleRepository.findByName(roleName)
                                .orElseThrow();

                assertEquals(roleName, foundRole.getName());
        }

        @Test
        void shouldPersistAndFindPermission() throws Exception {

                Permission permission = createEntity(Permission.class);

                String permissionName = uniqueValue("PERMISSION_TEST");

                permission.setName(permissionName);

                Permission savedPermission = permissionRepository.saveAndFlush(permission);

                assertNotNull(savedPermission.getId());
                assertTrue(permissionRepository.existsByName(permissionName));

                Permission foundPermission = permissionRepository.findByName(permissionName)
                                .orElseThrow();

                assertEquals(permissionName, foundPermission.getName());
        }

        @Test
        void shouldPersistUserRoleRelationship() throws Exception {

                User user = createEntity(User.class);

                user.setName("Usuario Relacionamento");
                user.setEmail(uniqueValue("relationship") + "@test.com");
                user.setPassword("password-test");
                user.setEnabled(true);

                user = userRepository.saveAndFlush(user);

                Role role = createEntity(Role.class);

                // role.setName(uniqueValue("ROLE_USER_TEST"));
                role.setName(uniqueValue("RU"));

                role = roleRepository.saveAndFlush(role);

                UserRole userRole = createEntity(UserRole.class);

                userRole.setId(new UserRoleId(user.getId(), role.getId()));
                userRole.setUser(user);
                userRole.setRole(role);

                userRoleRepository.saveAndFlush(userRole);

                UserRoleId id = new UserRoleId(
                                user.getId(),
                                role.getId());

                assertTrue(userRoleRepository.existsById(id));

                assertTrue(
                                userRoleRepository.existsByUser_IdAndRole_Id(
                                                user.getId(),
                                                role.getId()));

                assertEquals(
                                1,
                                userRoleRepository.findAllByUser_Id(user.getId()).size());

                assertEquals(
                                1,
                                userRoleRepository.findAllByRole_Id(role.getId()).size());
        }

        @Test
        void shouldPersistRolePermissionRelationship() throws Exception {

                Role role = createEntity(Role.class);

                // role.setName(uniqueValue("ROLE_PERMISSION_TEST"));
                role.setName(uniqueValue("RP"));

                role = roleRepository.saveAndFlush(role);

                Permission permission = createEntity(Permission.class);

                permission.setName(uniqueValue("PERMISSION_ROLE_TEST"));

                permission = permissionRepository.saveAndFlush(permission);

                RolePermission rolePermission = createEntity(RolePermission.class);

                rolePermission.setId(
                                new RolePermissionId(role.getId(), permission.getId()));

                rolePermission.setRole(role);
                rolePermission.setPermission(permission);

                rolePermissionRepository.saveAndFlush(rolePermission);

                RolePermissionId id = new RolePermissionId(
                                role.getId(),
                                permission.getId());

                assertTrue(rolePermissionRepository.existsById(id));

                assertTrue(
                                rolePermissionRepository.existsByRole_IdAndPermission_Id(
                                                role.getId(),
                                                permission.getId()));

                assertEquals(
                                1,
                                rolePermissionRepository.findAllByRole_Id(role.getId()).size());

                assertEquals(
                                1,
                                rolePermissionRepository
                                                .findAllByPermission_Id(permission.getId()).size());
        }

        // teste de integração que crie um usuário, um perfil e uma permissão no banco
        // de testes e valide o carregamento conjunto.
        @Test
        void shouldLoadUserWithRolesAndPermissionsForAuthentication()
                        throws Exception {

                User user = createEntity(User.class);
                user.setName("Usuario Autenticacao");
                user.setEmail(uniqueValue("auth") + "@test.com");
                user.setPassword("password-test");
                user.setEnabled(true);
                user = userRepository.saveAndFlush(user);

                Role role = createEntity(Role.class);
                role.setName(uniqueValue("AUTH_ROLE"));
                role = roleRepository.saveAndFlush(role);

                Permission permission = createEntity(Permission.class);
                permission.setName(uniqueValue("AUTH_PERMISSION"));
                permission = permissionRepository.saveAndFlush(permission);

                UserRole userRole = createEntity(UserRole.class);
                userRole.setId(new UserRoleId(user.getId(), role.getId()));
                userRole.setUser(user);
                userRole.setRole(role);
                userRoleRepository.saveAndFlush(userRole);

                RolePermission rolePermission = createEntity(RolePermission.class);
                rolePermission.setId(
                                new RolePermissionId(role.getId(), permission.getId()));
                rolePermission.setRole(role);
                rolePermission.setPermission(permission);

                // Pedro em 05/11/2026
                rolePermissionRepository.saveAndFlush(rolePermission);

                // Sincroniza os registros com o banco.
                entityManager.flush();

                // Limpa o contexto de persistência.
                entityManager.clear();

                // Primeira consulta: usuário e papéis.
                User loadedUser = userRepository
                                .findByEmailWithRoles(user.getEmail())
                                .orElseThrow();

                // Segunda consulta: papéis e permissões.
                List<Role> loadedRoles = roleRepository.findAllByUserEmailWithPermissions(
                                user.getEmail());

                // Valida usuário.
                assertEquals(user.getEmail(), loadedUser.getEmail());
                assertEquals(1, loadedUser.getUserRoles().size());

                // Valida papel.
                UserRole foundUserRole = loadedUser.getUserRoles().get(0);

                assertEquals(role.getName(), foundUserRole.getRole().getName());

                // Valida carregamento das permissões.
                assertEquals(1, loadedRoles.size());
                assertEquals(1, loadedRoles.get(0).getRolePermissions().size());

                assertEquals(
                                permission.getName(),
                                loadedRoles.get(0)
                                                .getRolePermissions()
                                                .get(0)
                                                .getPermission()
                                                .getName());
        }

}