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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//---
// O teste terá preparação de três usuários:
// ADMIN, RH e CONTABILIDADE.
// O ADMIN terá autoridade `ROLE_ADMIN`, enquanto RH e
// CONTABILIDADE não poderão administrar o catálogo.
//---
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDatabaseSafetyConfiguration.class)
@Transactional
class RoleControllerIntegrationTest {

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

    private User adminUser;
    private User rhUser;
    private User sectorUser;

    private Role adminRole;
    private Role rhRole;
    private Role sectorRole;

    @BeforeEach
    void setUp() {

        adminRole = roleRepository.save(Role.create("ADMIN"));
        rhRole = roleRepository.save(Role.create("RH"));
        sectorRole = roleRepository.save(Role.create("CONTABILIDADE"));

        adminUser = createUser("admin");
        rhUser = createUser("rh");
        sectorUser = createUser("sector");

        userRoleRepository.save(UserRole.create(adminUser, adminRole));
        userRoleRepository.save(UserRole.create(rhUser, rhRole));
        userRoleRepository.save(UserRole.create(sectorUser, sectorRole));

        entityManager.flush();
        entityManager.clear();
    }

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

    // ---
    @Test
    void adminShouldCreateRole() throws Exception {

        String token = obtainAccessToken(adminUser.getEmail());

        String requestBody = """
                {
                    "name": "NOVO_PERFIL"
                }
                """;

        mockMvc.perform(post("/api/roles")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("NOVO_PERFIL"));
    }

    @Test
    void adminShouldFindRoleById() throws Exception {

        String token = obtainAccessToken(adminUser.getEmail());

        mockMvc.perform(get("/api/roles/{id}", rhRole.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(rhRole.getId()))
                .andExpect(jsonPath("$.name").value("RH"));
    }

    @Test
    void adminShouldFindAllRoles() throws Exception {

        String token = obtainAccessToken(adminUser.getEmail());

        mockMvc.perform(get("/api/roles")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void rhShouldNotAccessRoleCatalog() throws Exception {

        String token = obtainAccessToken(rhUser.getEmail());

        mockMvc.perform(get("/api/roles")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void sectorUserShouldNotAccessRoleCatalog() throws Exception {

        String token = obtainAccessToken(sectorUser.getEmail());

        mockMvc.perform(get("/api/roles")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserShouldNotAccessRoleCatalog() throws Exception {

        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isUnauthorized());
    }

    // ---
    @Test
    void adminShouldUpdateRole() throws Exception {

        String token = obtainAccessToken(adminUser.getEmail());

        String requestBody = """
                {
                    "name": "RH_ATUALIZADO"
                }
                """;

        mockMvc.perform(put("/api/roles/{id}", rhRole.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("RH_ATUALIZADO"));
    }

    @Test
    void adminShouldNotUpdateRoleWithDuplicateName() throws Exception {

        String token = obtainAccessToken(adminUser.getEmail());

        String requestBody = """
                {
                    "name": "ADMIN"
                }
                """;

        mockMvc.perform(put("/api/roles/{id}", rhRole.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isConflict());
    }

    @Test
    void adminShouldNotUpdateNonexistentRole() throws Exception {

        String token = obtainAccessToken(adminUser.getEmail());

        String requestBody = """
                {
                    "name": "PERFIL_INEXISTENTE"
                }
                """;

        mockMvc.perform(put("/api/roles/{id}", 999999L)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isNotFound());
    }

    //---
    @Test
    void adminShouldDeleteRoleWithoutAssociations() throws Exception {

        String token = obtainAccessToken(adminUser.getEmail());

        Role roleToDelete = roleRepository.save(
                Role.create("PERFIL_PARA_EXCLUIR"));

        mockMvc.perform(delete("/api/roles/{id}", roleToDelete.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertEquals(false, roleRepository.existsById(roleToDelete.getId()));
    }

    @Test
    void adminShouldNotDeleteRoleAssociatedWithUser() throws Exception {

        String token = obtainAccessToken(adminUser.getEmail());

        Role roleToDelete = roleRepository.save(
                Role.create("PERFIL_COM_USUARIO"));

        userRoleRepository.save(
                UserRole.create(sectorUser, roleToDelete));

        entityManager.flush();

        mockMvc.perform(delete("/api/roles/{id}", roleToDelete.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void adminShouldNotDeleteRoleWithAssociatedPermission() throws Exception {

        String token = obtainAccessToken(adminUser.getEmail());

        Role roleToDelete = roleRepository.save(
                Role.create("PERFIL_COM_PERMISSAO"));

        Permission permission = permissionRepository.save(
                Permission.create("PERMISSAO_TESTE_EXCLUSAO"));

        rolePermissionRepository.save(
                RolePermission.create(roleToDelete, permission));

        entityManager.flush();

        mockMvc.perform(delete("/api/roles/{id}", roleToDelete.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void adminShouldNotDeleteNonexistentRole() throws Exception {

        String token = obtainAccessToken(adminUser.getEmail());

        mockMvc.perform(delete("/api/roles/{id}", 999999L)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

}
