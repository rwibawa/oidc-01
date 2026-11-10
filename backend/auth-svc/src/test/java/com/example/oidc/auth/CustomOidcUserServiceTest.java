package com.example.oidc.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.oidc.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class CustomOidcUserServiceTest {

    @Test
    void provisionsValidatedIdentityBeforeReturningPrincipal() {
        UserService users = mock(UserService.class);
        OidcUserRequest request = mock(OidcUserRequest.class);
        OidcUser identity = mock(OidcUser.class);
        OAuth2UserService<OidcUserRequest, OidcUser> delegate = ignored -> identity;
        var service = new CustomOidcUserService(users, delegate);

        assertThat(service.loadUser(request)).isSameAs(identity);
        verify(users).provision(identity);
    }

    @Test
    void persistenceFailureFailsAuthentication() {
        UserService users = mock(UserService.class);
        OidcUser identity = mock(OidcUser.class);
        when(users.provision(identity)).thenThrow(
                new DataAccessResourceFailureException("database unavailable"));
        var service = new CustomOidcUserService(users, ignored -> identity);

        assertThatThrownBy(() -> service.loadUser(mock(OidcUserRequest.class)))
                .isInstanceOfSatisfying(OAuth2AuthenticationException.class,
                        exception -> assertThat(exception.getError().getErrorCode())
                                .isEqualTo("user_provisioning_failed"));
    }
}
