package com.julyturadm.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.julyturadm.config.TestDatabaseSafetyConfiguration;
import com.julyturadm.domain.entity.Permission;
import com.julyturadm.domain.entity.Role;
import com.julyturadm.domain.entity.RolePermission;
import com.julyturadm.domain.entity.User;
import com.julyturadm.domain.entity.UserRole;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

// import de métodos GET/DELETE/POST e PUT
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
// 
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


//---
// Esperamos validar sete testes:
//
//1. RH consegue atribuir um perfil funcional.
//2. RH consegue remover um perfil funcional.
//3. RH não consegue atribuir `ADMIN`.
//4. RH não consegue remover `ADMIN`.
//5. Usuário de setor não consegue atribuir perfis.
//6. RH recebe `409 Conflict` ao tentar atribuir um perfil duplicado.
//7. ADMIN consegue atribuir um perfil funcional.
//---
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDatabaseSafetyConfiguration.class)
@Transactional
class UserRoleControllerIntegrationTest {

    private static final String PASSWORD = "Test@123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    private User rhUser;
    private User adminUser;
    private User sectorUser;
    private User targetUser;

    private Role rhRole;
    private Role adminRole;
    private Role sectorRole;
    private Role functionalRole;

    @BeforeEach
    void setUp() {

        // 1. Criar os perfis utilizados nos testes.
        rhRole = roleRepository.save(Role.create("RH"));
        adminRole = roleRepository.save(Role.create("ADMIN"));
        sectorRole = roleRepository.save(Role.create("CONTABILIDADE"));
        functionalRole = roleRepository.save(Role.create("VENDAS"));

        // 2. Criar a permissão necessária para autenticação
        // e para os endpoints de consulta de usuários.
        Permission userRead = permissionRepository.save(
                Permission.create("USER_READ"));

        // 3. ADMIN e RH recebem USER_READ.
        rolePermissionRepository.save(
                RolePermission.create(rhRole, userRead));

        rolePermissionRepository.save(
                RolePermission.create(adminRole, userRead));

        // 4. Criar os usuários de teste.
        rhUser = createUser("rh");
        adminUser = createUser("admin");
        sectorUser = createUser("sector");
        targetUser = createUser("target");

        entityManager.flush();

        // 5. Associar os perfis aos usuários que atuarão nos testes.
        userRoleRepository.save(UserRole.create(rhUser, rhRole));
        userRoleRepository.save(UserRole.create(adminUser, adminRole));
        userRoleRepository.save(UserRole.create(sectorUser, sectorRole));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void rhShouldAssignFunctionalRole() throws Exception {

        String token = obtainAccessToken(rhUser.getEmail());

        mockMvc.perform(put("/api/users/{userId}/roles/{roleId}",
                        targetUser.getId(), functionalRole.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(functionalRole.getId()))
                .andExpect(jsonPath("$.name").value("VENDAS"));

        assertEquals(
                true,
                userRoleRepository.existsByUser_IdAndRole_Id(
                        targetUser.getId(), functionalRole.getId()));
    }

    @Test
    void rhShouldRemoveFunctionalRole() throws Exception {

        userRoleRepository.save(
                UserRole.create(targetUser, functionalRole));

        entityManager.flush();

        String token = obtainAccessToken(rhUser.getEmail());

        mockMvc.perform(delete("/api/users/{userId}/roles/{roleId}",
                        targetUser.getId(), functionalRole.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertEquals(
                false,
                userRoleRepository.existsByUser_IdAndRole_Id(
                        targetUser.getId(), functionalRole.getId()));
    }

    @Test
    void rhShouldNotAssignAdminRole() throws Exception {

        String token = obtainAccessToken(rhUser.getEmail());

        mockMvc.perform(put("/api/users/{userId}/roles/{roleId}",
                        targetUser.getId(), adminRole.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        assertEquals(
                false,
                userRoleRepository.existsByUser_IdAndRole_Id(
                        targetUser.getId(), adminRole.getId()));
    }

    @Test
    void rhShouldNotRemoveAdminRole() throws Exception {

        userRoleRepository.save(
                UserRole.create(targetUser, adminRole));

        entityManager.flush();

        String token = obtainAccessToken(rhUser.getEmail());

        mockMvc.perform(delete("/api/users/{userId}/roles/{roleId}",
                        targetUser.getId(), adminRole.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        assertEquals(
                true,
                userRoleRepository.existsByUser_IdAndRole_Id(
                        targetUser.getId(), adminRole.getId()));
    }

    @Test
    void sectorUserShouldNotAssignRoles() throws Exception {

        String token = obtainAccessToken(sectorUser.getEmail());

        mockMvc.perform(put("/api/users/{userId}/roles/{roleId}",
                        targetUser.getId(), functionalRole.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        assertEquals(
                false,
                userRoleRepository.existsByUser_IdAndRole_Id(
                        targetUser.getId(), functionalRole.getId()));
    }

    @Test
    void rhShouldReceiveConflictWhenAssigningDuplicateRole() throws Exception {

        userRoleRepository.save(
                UserRole.create(targetUser, functionalRole));

        entityManager.flush();

        String token = obtainAccessToken(rhUser.getEmail());

        mockMvc.perform(put("/api/users/{userId}/roles/{roleId}",
                        targetUser.getId(), functionalRole.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void adminShouldAssignFunctionalRole() throws Exception {

        String token = obtainAccessToken(adminUser.getEmail());

        mockMvc.perform(put("/api/users/{userId}/roles/{roleId}",
                        targetUser.getId(), functionalRole.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("VENDAS"));

        assertEquals(
                true,
                userRoleRepository.existsByUser_IdAndRole_Id(
                        targetUser.getId(), functionalRole.getId()));
    }

    //---
    @Test
    void shouldFindUserRolesWithUserReadPermission() throws Exception {

        // Preparar um perfil funcional para o usuário consultado.
        userRoleRepository.save(
                UserRole.create(targetUser, functionalRole));

        entityManager.flush();

        // Autenticar um usuário RH, que possui USER_READ.
        String token = obtainAccessToken(rhUser.getEmail());

        mockMvc.perform(get("/api/users/{userId}/roles",
                        targetUser.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(functionalRole.getId()))
                .andExpect(jsonPath("$[0].name").value("VENDAS"));
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {

        String token = obtainAccessToken(rhUser.getEmail());

        mockMvc.perform(get("/api/users/{userId}/roles", Long.MAX_VALUE)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void sectorUserShouldNotFindUserRolesWithoutUserReadPermission() throws Exception {

        // O usuário CONTABILIDADE não possui USER_READ.
        String token = obtainAccessToken(sectorUser.getEmail());

        mockMvc.perform(get("/api/users/{userId}/roles",
                        targetUser.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
    
    //---
    private User createUser(String prefix) {

        String uniqueEmail = prefix + "-"
                + UUID.randomUUID().toString().substring(0, 8)
                + "@test.com";

        User user = User.create(
                "Usuário " + prefix,
                uniqueEmail,
                passwordEncoder.encode(PASSWORD));

        return userRepository.save(user);
    }

    //---
    // obtainAccessToken: Método de autenticação, envia `LoginRequest`
    // e extrai `accessToken` da resposta JSON.
    //--- 
    private String obtainAccessToken(String email) throws Exception {

        String requestBody = objectMapper.writeValueAsString(
                new LoginRequest(email, PASSWORD));

        String responseBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode responseJson = objectMapper.readTree(responseBody);

        return responseJson.get("accessToken").asText();
    }
}
