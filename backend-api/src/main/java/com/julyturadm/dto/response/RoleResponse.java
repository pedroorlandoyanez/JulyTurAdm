package com.julyturadm.dto.response;

import java.time.LocalDateTime;

public record RoleResponse(

    Long id,

    String name,

    LocalDateTime createdAt,

    LocalDateTime updatedAt

) {
}