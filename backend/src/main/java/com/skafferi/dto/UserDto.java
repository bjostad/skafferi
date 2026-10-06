package com.skafferi.dto;

public record UserDto(
        String id,
        String username,
        String displayName,
        String email,
        String role,
        String avatarColor
) {}
