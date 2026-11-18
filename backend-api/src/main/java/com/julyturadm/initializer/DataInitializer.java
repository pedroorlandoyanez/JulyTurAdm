package com.julyturadm.initializer;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.julyturadm.domain.entity.Permission;
import com.julyturadm.domain.entity.Role;
import com.julyturadm.domain.entity.RolePermission;
import com.julyturadm.domain.repository.PermissionRepository;
import com.julyturadm.domain.repository.RolePermissionRepository;
import com.julyturadm.domain.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

//---
// Proteção inicial, quando formos executar o cadastro dos dados, também 
// verificaremos o banco de destino e a estratégia de execução.
// Só executa quando a propriedade estiver explicitamente definida como true.
//---
@Component
@ConditionalOnProperty(
        prefix = "julyturadm.initializer",
        name = "enabled",
        havingValue = "true"
)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    //--
    // - `@Transactional` garante que a inicialização seja executada como uma única transação: se ocorrer
    //                    uma falha, as alterações serão revertidas.
    //---
    @Override
    @Transactional
    public void run(String... args) {

        System.out.println("Inicializando dados básicos do JulyTurAdm...");

        // 1. Criar ou recuperar as roles.
        Role admin = findOrCreateRole("ADMIN");
        Role rh = findOrCreateRole("RH");

        findOrCreateRole("CONTAS_PAGAR");
        findOrCreateRole("COBRANCA");
        findOrCreateRole("CONTABILIDADE");
        findOrCreateRole("VENDAS");

        // 2. Criar ou recuperar as permissões.
        Permission userCreate = findOrCreatePermission("USER_CREATE");
        Permission userRead = findOrCreatePermission("USER_READ");
        Permission userDisable = findOrCreatePermission("USER_DISABLE");
        Permission userEnable = findOrCreatePermission("USER_ENABLE");

        // 3. Associar permissões administrativas de usuários.
        // Somente ADMIN e RH recebem essas quatro permissões.
        grantPermission(admin, userCreate);
        grantPermission(admin, userRead);
        grantPermission(admin, userDisable);
        grantPermission(admin, userEnable);

        grantPermission(rh, userCreate);
        grantPermission(rh, userRead);
        grantPermission(rh, userDisable);
        grantPermission(rh, userEnable);

        System.out.println("Dados básicos inicializados com sucesso.");
    }
      
    // `findOrCreateRole()` e `findOrCreatePermission()` evitam criar registros duplicados.
    private Role findOrCreateRole(String name) {

        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(Role.create(name)));
    }

    // `findOrCreateRole()` e `findOrCreatePermission()` evitam criar registros duplicados.
    private Permission findOrCreatePermission(String name) {

        return permissionRepository.findByName(name)
                .orElseGet(() -> permissionRepository.save(Permission.create(name)));
    }

    // - `grantPermission()` verifica se a associação já existe antes de gravá-la.
    // - `RolePermission.create()` recebe entidades já persistidas, com seus IDs disponíveis.
    private void grantPermission(Role role, Permission permission) {

        boolean alreadyExists =
                rolePermissionRepository.existsByRole_IdAndPermission_Id(
                        role.getId(),
                        permission.getId()
                );

        if (!alreadyExists) {
            RolePermission association =
                    RolePermission.create(role, permission);

            rolePermissionRepository.save(association);
        }
    }
}
