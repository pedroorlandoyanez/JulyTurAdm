package com.julyturadm.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.julyturadm.config.TestDatabaseSafetyConfiguration;
import com.julyturadm.domain.entity.Permission;
import com.julyturadm.domain.entity.Role;
import com.julyturadm.domain.entity.RolePermission;
import com.julyturadm.domain.entity.User;
import com.julyturadm.domain.entity.UserRole;
import com.julyturadm.domain.entity.id.RolePermissionId;
import com.julyturadm.domain.entity.id.UserRoleId;

import com.julyturadm.domain.repository.PermissionRepository;
import com.julyturadm.domain.repository.RolePermissionRepository;
import com.julyturadm.domain.repository.RoleRepository;
import com.julyturadm.domain.repository.UserRepository;
import com.julyturadm.domain.repository.UserRoleRepository;

import com.julyturadm.dto.request.LoginRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDatabaseSafetyConfiguration.class)
@Transactional
class AuthControllerIntegrationTest {

        private static final String LOGIN_URL = "/api/auth/login";
        private static final String PASSWORD = "SenhaTeste123";

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private JwtDecoder jwtDecoder;

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

        @PersistenceContext
        private EntityManager entityManager;

        private User user;
        private String permissionName;
        private String roleName;

        @BeforeEach
        void setUp() throws Exception {

                String uniqueId = UUID.randomUUID().toString();

                // 1. Cria o usuário com senha BCrypt.
                user = User.create(
                                "Usuario Teste",
                                "auth_" + uniqueId + "@teste.com",
                                passwordEncoder.encode(PASSWORD));

                user = userRepository.saveAndFlush(user);

                // 2. Cria o perfil.
                Role role = createEntity(Role.class);
                role.setName("TEST_ADMIN_" + uniqueId);
                roleName = role.getName();

                role = roleRepository.saveAndFlush(role);

                // 3. Cria a permissão.
                Permission permission = createEntity(Permission.class);

                //permissionName = "USER_READ_" + uniqueId;
                permissionName = "USER_READ";
                permission.setName(permissionName);

                permission = permissionRepository.saveAndFlush(permission);

                // 4. Associa o usuário ao perfil.
                UserRole userRole = createEntity(UserRole.class);

                userRole.setId(
                                new UserRoleId(user.getId(), role.getId()));

                userRole.setUser(user);
                userRole.setRole(role);

                userRoleRepository.saveAndFlush(userRole);

                // 5. Associa a permissão ao perfil.
                RolePermission rolePermission = createEntity(RolePermission.class);

                rolePermission.setId(
                                new RolePermissionId(role.getId(), permission.getId()));

                rolePermission.setRole(role);
                rolePermission.setPermission(permission);

                rolePermissionRepository.saveAndFlush(rolePermission);

                // Garante que os próximos acessos recarreguem as entidades do banco.
                entityManager.flush();
                entityManager.clear();

        }

        private <T> T createEntity(Class<T> type) throws Exception {

                Constructor<T> constructor = type.getDeclaredConstructor();
                constructor.setAccessible(true);

                return constructor.newInstance();
        }

        private String createLoginJson(String email, String password)
                        throws Exception {

                return objectMapper.writeValueAsString(
                                new LoginRequest(email, password));
        }

        private String obtainAccessToken() throws Exception {

                String responseBody = mockMvc.perform(
                                post(LOGIN_URL)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(createLoginJson(
                                                                user.getEmail(),
                                                                PASSWORD)))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                JsonNode response = objectMapper.readTree(responseBody);

                return response.get("accessToken").asText();
        }

        @Test
        void shouldAuthenticateUserAndReturnValidJwt() throws Exception {
                String responseBody = mockMvc.perform(
                                post(LOGIN_URL)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(createLoginJson(
                                                                user.getEmail(),
                                                                PASSWORD)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                                .andExpect(jsonPath("$.expiresIn").value(1800))
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                JsonNode response = objectMapper.readTree(responseBody);

                String accessToken = response.get("accessToken").asText();

                // Valida a assinatura e a validade do JWT.
                Jwt jwt = jwtDecoder.decode(accessToken);

                assertEquals(user.getEmail(), jwt.getSubject());

                // Valida os perfis e as permissões presentes no token.
                assertTrue(
                                jwt.getClaimAsStringList("roles")
                                                .contains("ROLE_" + roleName));

                assertTrue(
                                jwt.getClaimAsStringList("permissions")
                                                .contains(permissionName));
        }

        @Test
        void shouldReturnUnauthorizedWhenPasswordIsIncorrect() throws Exception {

                mockMvc.perform(
                                post(LOGIN_URL)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(createLoginJson(
                                                                user.getEmail(),
                                                                "SenhaIncorreta")))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.status").value(401))
                                .andExpect(jsonPath("$.message")
                                                .value("E-mail ou senha inválidos."));
        }

        @Test
        void shouldReturnUnauthorizedWhenUserDoesNotExist() throws Exception {

                mockMvc.perform(
                                post(LOGIN_URL)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(createLoginJson(
                                                                "inexistente@teste.com",
                                                                PASSWORD)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        void shouldReturnBadRequestWhenEmailIsInvalid() throws Exception {

                mockMvc.perform(
                                post(LOGIN_URL)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(createLoginJson(
                                                                "email-invalido",
                                                                PASSWORD)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        void shouldReturnBadRequestWhenEmailIsBlank() throws Exception {

                mockMvc.perform(
                                post(LOGIN_URL)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(createLoginJson(
                                                                "",
                                                                PASSWORD)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        void shouldReturnBadRequestWhenPasswordIsBlank() throws Exception {

                mockMvc.perform(
                                post(LOGIN_URL)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(createLoginJson(
                                                                user.getEmail(),
                                                                "")))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        void shouldReturnUnauthorizedWhenAccessingUsersWithoutJwt()
                        throws Exception {

                mockMvc.perform(get("/api/users"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldAllowAccessingUsersWithValidJwt()
                        throws Exception {

                String accessToken = obtainAccessToken();

                mockMvc.perform(
                                get("/api/users")
                                                .header("Authorization", "Bearer " + accessToken))
                                .andExpect(status().isOk());
        }

        @Test
        void shouldReturnUnauthorizedWhenAccessingUsersWithInvalidJwt()
                        throws Exception {

                mockMvc.perform(
                                get("/api/users")
                                                .header("Authorization", "Bearer token-invalido"))
                                .andExpect(status().isUnauthorized());
        }

}
