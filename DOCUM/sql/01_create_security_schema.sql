
-- ============================================================
-- JulyTurAdm
-- Script: 01_create_security_schema.sql
-- Banco: bd_paytur
-- MySQL: 8.0
--
-- Modelo inicial de autenticação e autorização:
-- users, roles, permissions,
-- users_roles, roles_permissions
--
-- Auditoria MySQL somente nas entidades principais.
-- ============================================================

USE bd_paytur;

-- ============================================================
-- 1. USERS
-- ============================================================

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    created_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT PK_users PRIMARY KEY (id),
    CONSTRAINT UK_users_email UNIQUE (email)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4;

-- ============================================================
-- 2. ROLES
-- ============================================================

CREATE TABLE roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,

    created_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT PK_roles PRIMARY KEY (id),
    CONSTRAINT UK_roles_name UNIQUE (name)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4;

-- ============================================================
-- 3. PERMISSIONS
-- ============================================================

CREATE TABLE permissions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(80) NOT NULL,

    created_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT PK_permissions PRIMARY KEY (id),
    CONSTRAINT UK_permissions_name UNIQUE (name)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4;

-- ============================================================
-- 4. USERS_ROLES
-- Associação entre usuários e roles.
-- Chave primária composta: user_id + role_id.
-- Sem auditoria.
-- ============================================================

CREATE TABLE users_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,

    CONSTRAINT PK_users_roles
        PRIMARY KEY (user_id, role_id),

    INDEX IX_users_roles_role_id (role_id),

    CONSTRAINT FK_ur_user
        FOREIGN KEY (user_id)
        REFERENCES users (id),

    CONSTRAINT FK_ur_role
        FOREIGN KEY (role_id)
        REFERENCES roles (id)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4;

-- ============================================================
-- 5. ROLES_PERMISSIONS
-- Associação entre roles e permissions.
-- Chave primária composta: role_id + permission_id.
-- Sem auditoria.
-- ============================================================

CREATE TABLE roles_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,

    CONSTRAINT PK_roles_permissions
        PRIMARY KEY (role_id, permission_id),

    INDEX IX_roles_permissions_permission_id (permission_id),

    CONSTRAINT FK_rp_role
        FOREIGN KEY (role_id)
        REFERENCES roles (id),

    CONSTRAINT FK_rp_permission
        FOREIGN KEY (permission_id)
        REFERENCES permissions (id)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4;

-- ============================================================
-- FIM DO SCRIPT
-- ============================================================
