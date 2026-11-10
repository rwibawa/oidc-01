package com.example.oidc.user;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class UserService {

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public User provision(OidcUser identity) {
        String issuer = identity.getIssuer().toString();
        String subject = identity.getSubject();
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        User user = repository.findByIssuerAndSubject(issuer, subject)
                .orElseGet(() -> new User(issuer, subject, now));
        String name = identity.getFullName();
        user.updateProfile(name == null || name.isBlank() ? subject : name,
                identity.getEmail(), now);
        return repository.saveAndFlush(user);
    }

    @Transactional(readOnly = true)
    public User currentUser(OidcUser identity) {
        return repository.findByIssuerAndSubject(
                        identity.getIssuer().toString(), identity.getSubject())
                .orElseThrow(() -> new ResponseStatusException(
                        UNAUTHORIZED, "No internal user exists for this session; sign in again."));
    }
}
