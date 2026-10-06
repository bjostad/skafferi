package com.skafferi.dto;

public record BringSyncDto(
        String email,
        String password,
        String listUuid,
        boolean autoSync
) {}
