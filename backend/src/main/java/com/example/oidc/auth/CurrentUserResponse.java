package com.example.oidc.auth;

public record CurrentUserResponse(String subject, String name, String email) {
}
