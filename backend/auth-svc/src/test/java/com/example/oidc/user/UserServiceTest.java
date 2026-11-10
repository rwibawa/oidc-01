package com.example.oidc.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@SpringBootTest(properties = {
        "spring.profiles.active=dev",
        "spring.datasource.url=jdbc:h2:mem:user-tests;DB_CLOSE_DELAY=-1",
        "spring.security.oauth2.client.registration.google.client-id=test-client",
        "spring.security.oauth2.client.registration.google.client-secret=test-secret"
})
class UserServiceTest {

    @Autowired
    private UserService users;

    @Autowired
    private UserRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void firstSignInCreatesPersistentInternalUser() {
        var identity = identity("https://accounts.google.com", "123", "Taylor", "a@example.com");
        User user = users.provision(identity);

        assertThat(user.getId()).isNotNull();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isEqualTo(user.getCreatedAt());
        assertThat(users.currentUser(identity).getId()).isEqualTo(user.getId());
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void laterSignInUpdatesProfileWithoutChangingIdentity() {
        User first = users.provision(
                identity("https://accounts.google.com", "123", "Old Name", "old@example.com"));
        User updated = users.provision(
                identity("https://accounts.google.com", "123", "New Name", "new@example.com"));

        assertThat(updated.getId()).isEqualTo(first.getId());
        assertThat(updated.getCreatedAt()).isEqualTo(first.getCreatedAt());
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(first.getUpdatedAt());
        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getEmail()).isEqualTo("new@example.com");
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void emailDoesNotLinkDistinctSubjectsOrIssuers() {
        User first = users.provision(identity("https://accounts.google.com", "123", "A", "same@example.com"));
        User second = users.provision(identity("https://accounts.google.com", "456", "A", "same@example.com"));
        User third = users.provision(identity("https://another.example.com", "123", "A", "same@example.com"));

        assertThat(List.of(first.getId(), second.getId(), third.getId())).doesNotHaveDuplicates();
        assertThat(repository.count()).isEqualTo(3);
    }

    @Test
    void missingProfileUsesSubjectAndAllowsNullEmail() {
        User user = users.provision(identity("https://accounts.google.com", "123", null, null));
        assertThat(user.getName()).isEqualTo("123");
        assertThat(user.getEmail()).isNull();
    }

    @Test
    void databaseRejectsDuplicateExternalIdentity() {
        users.provision(identity("https://accounts.google.com", "123", "A", null));
        var duplicate = new User("https://accounts.google.com", "123", Instant.now());
        duplicate.updateProfile("B", null, Instant.now());

        assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private OidcUser identity(String issuer, String subject, String name, String email) {
        var token = OidcIdToken.withTokenValue("test-token")
                .issuer(issuer).subject(subject)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300));
        if (name != null) {
            token.claim("name", name);
        }
        if (email != null) {
            token.claim("email", email);
        }
        return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_USER")), token.build());
    }
}
