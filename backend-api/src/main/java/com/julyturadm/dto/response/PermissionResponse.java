package com.julyturadm.dto.response;

import java.time.LocalDateTime;

public record PermissionResponse(
        Long id,
        String name,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
