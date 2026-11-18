package com.julyturadm.initializer;

import com.julyturadm.config.TestDatabaseSafetyConfiguration;
import com.julyturadm.domain.repository.PermissionRepository;
import com.julyturadm.domain.repository.RolePermissionRepository;
import com.julyturadm.domain.repository.RoleRepository;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

//---
// O DataInitializerIntegrationTest, habilita o inicializador exclusivamente para
// aquela classe de teste, por meio desta propriedade.
//---
@SpringBootTest(properties = {
                "julyturadm.initializer.enabled=true"
})
@ActiveProfiles("test")
@Import(TestDatabaseSafetyConfiguration.class)
class DataInitializerIntegrationTest {

        @Autowired
        private DataInitializer dataInitializer;

        @Autowired
        private RoleRepository roleRepository;

        @Autowired
        private PermissionRepository permissionRepository;

        @Autowired
        private RolePermissionRepository rolePermissionRepository;

        @Test
        void shouldCreateInitialRolesPermissionsAndRelationships() {

                assertTrue(roleRepository.existsByName("ADMIN"));
                assertTrue(roleRepository.existsByName("RH"));
                assertTrue(roleRepository.existsByName("CONTAS_PAGAR"));
                assertTrue(roleRepository.existsByName("COBRANCA"));
                assertTrue(roleRepository.existsByName("CONTABILIDADE"));
                assertTrue(roleRepository.existsByName("VENDAS"));

                assertTrue(permissionRepository.existsByName("USER_CREATE"));
                assertTrue(permissionRepository.existsByName("USER_READ"));
                assertTrue(permissionRepository.existsByName("USER_DISABLE"));
                assertTrue(permissionRepository.existsByName("USER_ENABLE"));

                var admin = roleRepository.findByName("ADMIN").orElseThrow();
                var rh = roleRepository.findByName("RH").orElseThrow();

                assertEquals(4,
                                rolePermissionRepository.findAllByRole_Id(admin.getId()).size());

                assertEquals(4,
                                rolePermissionRepository.findAllByRole_Id(rh.getId()).size());

                for (String roleName : new String[] {
                                "CONTAS_PAGAR",
                                "COBRANCA",
                                "CONTABILIDADE",
                                "VENDAS"
                }) {
                        var role = roleRepository.findByName(roleName).orElseThrow();

                        assertEquals(0,
                                        rolePermissionRepository.findAllByRole_Id(role.getId()).size());
                }
        }

        @Test
        void shouldNotDuplicateDataWhenInitializerRunsAgain() {

                long rolesBefore = roleRepository.count();
                long permissionsBefore = permissionRepository.count();
                long relationshipsBefore = rolePermissionRepository.count();

                dataInitializer.run();

                assertEquals(rolesBefore, roleRepository.count());
                assertEquals(permissionsBefore, permissionRepository.count());
                assertEquals(relationshipsBefore, rolePermissionRepository.count());
        }
}
