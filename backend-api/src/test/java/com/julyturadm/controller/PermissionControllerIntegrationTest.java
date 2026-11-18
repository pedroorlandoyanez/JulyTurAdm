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
// Por que manter esses componentes?
//- `PermissionRepository`: prepara e verifica permissões.
//- `RolePermissionRepository`: cria associações para testar a integridade referencial.
//- `EntityManager`: garante a sincronização dos registros antes das requisições.
//- `TestDatabaseSafetyConfiguration`: protege o banco de produção.
//- `obtainAccessToken()`: permite testar a autorização real com JWT.
//---
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDatabaseSafetyConfiguration.class)
@Transactional
class PermissionControllerIntegrationTest {

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
    void adminShouldCreatePermission() throws Exception {

        String token = obtainAccessToken(adminUser.getEmail());

        String requestBody = """
                {
                    "name": "PERMISSION_TEST"
                }
                """;

        mockMvc.perform(post("/api/permissions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("PERMISSION_TEST"));
    }

    @Test
    void adminShouldFindPermissionById() throws Exception {

        String token = obtainAccessToken(adminUser.getEmail());

        Permission permission = permissionRepository.save(
                Permission.create("USER_READ"));

        entityManager.flush();

        mockMvc.perform(get("/api/permissions/{id}", permission.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(permission.getId()))
                .andExpect(jsonPath("$.name").value("USER_READ"));
    }

    @Test
    void adminShouldFindAllPermissions() throws Exception {

        String token = obtainAccessToken(adminUser.getEmail());

        permissionRepository.save(Permission.create("USER_CREATE"));
        permissionRepository.save(Permission.create("USER_READ"));

        entityManager.flush();

        mockMvc.perform(get("/api/permissions")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void rhShouldNotAccessPermissionCatalog() throws Exception {

        String token = obtainAccessToken(rhUser.getEmail());

        mockMvc.perform(get("/api/permissions")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void sectorUserShouldNotAccessPermissionCatalog() throws Exception {

        String token = obtainAccessToken(sectorUser.getEmail());

        mockMvc.perform(get("/api/permissions")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserShouldNotAccessPermissionCatalog() throws Exception {

        mockMvc.perform(get("/api/permissions"))
                .andExpect(status().isUnauthorized());
    }

    // ---
    @Test
    void adminShouldUpdatePermission() throws Exception {

        String token = obtainAccessToken(adminUser.getEmail());

        Permission permission = permissionRepository.save(
                Permission.create("PERMISSION_OLD"));

        entityManager.flush();

        String requestBody = """
                {
                    "name": "PERMISSION_NEW"
                }
                """;

        mockMvc.perform(put("/api/permissions/{id}", permission.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(permission.getId()))
                .andExpect(jsonPath("$.name").value("PERMISSION_NEW"));
    }

    @Test
    void adminShouldNotUpdatePermissionWithExistingName() throws Exception {

        String token = obtainAccessToken(adminUser.getEmail());

        permissionRepository.save(Permission.create("PERMISSION_ONE"));

        Permission permission = permissionRepository.save(
                Permission.create("PERMISSION_TWO"));

        entityManager.flush();

        String requestBody = """
                {
                    "name": "PERMISSION_ONE"
                }
                """;

        mockMvc.perform(put("/api/permissions/{id}", permission.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isConflict());
    }

    @Test
    void adminShouldNotUpdateNonexistentPermission() throws Exception {

        String token = obtainAccessToken(adminUser.getEmail());

        String requestBody = """
                {
                    "name": "PERMISSION_NEW"
                }
                """;

        mockMvc.perform(put("/api/permissions/{id}", 999999L)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminShouldDeletePermissionWithoutAssociations() throws Exception {

        String token = obtainAccessToken(adminUser.getEmail());

        Permission permission = permissionRepository.save(
                Permission.create("PERMISSION_TO_DELETE"));

        entityManager.flush();

        mockMvc.perform(delete("/api/permissions/{id}", permission.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertEquals(false, permissionRepository.existsById(permission.getId()));
    }

    @Test
    void adminShouldNotDeletePermissionAssociatedWithRole() throws Exception {

        String token = obtainAccessToken(adminUser.getEmail());

        Permission permission = permissionRepository.save(
                Permission.create("PERMISSION_ASSOCIATED"));

        Role role = roleRepository.findByName("CONTABILIDADE")
                .orElseThrow();

        rolePermissionRepository.save(
                RolePermission.create(role, permission));

        entityManager.flush();

        mockMvc.perform(delete("/api/permissions/{id}", permission.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());

        assertEquals(true, permissionRepository.existsById(permission.getId()));
    }

    @Test
    void adminShouldNotDeleteNonexistentPermission() throws Exception {

        String token = obtainAccessToken(adminUser.getEmail());

        mockMvc.perform(delete("/api/permissions/{id}", 999999L)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
    
    //---
    @Test
    void adminShouldNotCreatePermissionWithDuplicateName() throws Exception {

        String token = obtainAccessToken(adminUser.getEmail());

        permissionRepository.save(
                Permission.create("PERMISSION_DUPLICATE"));

        entityManager.flush();

        String requestBody = """
                {
                    "name": "PERMISSION_DUPLICATE"
                }
                """;

        mockMvc.perform(post("/api/permissions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isConflict());
    }

    @Test
    void adminShouldNotCreatePermissionWithBlankName() throws Exception {

        String token = obtainAccessToken(adminUser.getEmail());

        String requestBody = """
                {
                    "name": "   "
                }
                """;

        mockMvc.perform(post("/api/permissions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminShouldNotCreatePermissionWithNameLongerThan80Characters() throws Exception {

        String token = obtainAccessToken(adminUser.getEmail());

        String longName = "A".repeat(81);

        String requestBody = objectMapper.writeValueAsString(
                java.util.Map.of("name", longName));

        mockMvc.perform(post("/api/permissions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }

}
