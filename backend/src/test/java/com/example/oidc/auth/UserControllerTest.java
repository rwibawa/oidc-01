package com.example.oidc.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.oidc.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void currentUserRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void currentUserReturnsOidcIdentityClaims() throws Exception {
        mockMvc.perform(get("/api/me").with(oidcLogin()
                        .idToken(token -> token
                                .subject("google-sub-123")
                                .claim("name", "Taylor Example")
                                .claim("email", "taylor@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("google-sub-123"))
                .andExpect(jsonPath("$.name").value("Taylor Example"))
                .andExpect(jsonPath("$.email").value("taylor@example.com"));
    }

    @Test
    void csrfEndpointReturnsHeaderAndToken() throws Exception {
        mockMvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
}
