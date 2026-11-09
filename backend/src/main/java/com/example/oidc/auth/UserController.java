package com.example.oidc.auth;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getHeaderName(), csrfToken.getToken());
    }

    @GetMapping("/me")
    public CurrentUserResponse me(@AuthenticationPrincipal OidcUser user) {
        String name = user.getFullName();
        if (name == null || name.isBlank()) {
            name = user.getSubject();
        }
        return new CurrentUserResponse(user.getSubject(), name, user.getEmail());
    }
}
