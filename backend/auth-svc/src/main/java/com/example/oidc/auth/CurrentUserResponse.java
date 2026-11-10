package com.example.oidc.auth;

import java.util.UUID;

public record CurrentUserResponse(UUID id, String subject, String name, String email) {
}
