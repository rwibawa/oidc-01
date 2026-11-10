package com.example.oidc.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.oidc.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.profiles.active=dev",
        "spring.datasource.url=jdbc:h2:mem:controller-tests;DB_CLOSE_DELAY=-1",
        "spring.security.oauth2.client.registration.google.client-id=test-client",
        "spring.security.oauth2.client.registration.google.client-secret=test-secret"
})
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService users;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void currentUserRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void currentUserReturnsOidcIdentityClaims() throws Exception {
        var identity = new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                OidcIdToken.withTokenValue("test-token")
                        .issuer("https://accounts.google.com")
                        .subject("google-sub-123")
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(300))
                        .claim("name", "Taylor Example")
                        .claim("email", "taylor@example.com")
                        .build());
        var user = users.provision(identity);
        mockMvc.perform(get("/api/me").with(oidcLogin().oidcUser(identity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.subject").value("google-sub-123"))
                .andExpect(jsonPath("$.name").value("Taylor Example"))
                .andExpect(jsonPath("$.email").value("taylor@example.com"));
    }

    @Test
    void unprovisionedSessionRequiresSignInAgain() throws Exception {
        mockMvc.perform(get("/api/me").with(oidcLogin()
                        .idToken(token -> token.issuer("https://accounts.google.com")
                                .subject("unprovisioned"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRequiresCsrf() throws Exception {
        mockMvc.perform(post("/api/logout").with(oidcLogin()))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutWithCsrfSucceeds() throws Exception {
        var csrfResult = mockMvc.perform(get("/api/csrf")).andReturn();
        var token = objectMapper.readTree(csrfResult.getResponse().getContentAsString());
        var cookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
        mockMvc.perform(post("/api/logout").with(oidcLogin())
                        .cookie(cookie)
                        .header(token.get("headerName").asText(), token.get("token").asText()))
                .andExpect(status().isNoContent());
    }

    @Test
    void csrfEndpointReturnsHeaderAndToken() throws Exception {
        mockMvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
}
