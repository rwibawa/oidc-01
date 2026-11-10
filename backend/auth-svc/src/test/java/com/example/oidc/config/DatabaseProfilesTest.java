package com.example.oidc.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DatabaseProfilesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer());

    @Test
    void defaultProfileUsesPersistentH2() {
        contextRunner.run(context -> {
            assertThat(context.getEnvironment().getDefaultProfiles()).containsExactly("dev");
            assertThat(context.getEnvironment().getProperty("spring.datasource.url"))
                    .isEqualTo("jdbc:h2:file:./data/oidc");
            assertThat(context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto"))
                    .isEqualTo("validate");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"qa", "staging", "production"})
    void deployedProfilesUsePostgresql(String profile) {
        contextRunner.withPropertyValues(
                "spring.profiles.active=" + profile,
                "DATABASE_URL=jdbc:postgresql://db:5432/oidc",
                "DATABASE_USERNAME=oidc",
                "DATABASE_PASSWORD=test-password")
                .run(context -> {
                    assertThat(context.getEnvironment().getProperty("spring.datasource.url"))
                            .isEqualTo("jdbc:postgresql://db:5432/oidc");
                    assertThat(context.getEnvironment().getProperty("spring.datasource.driver-class-name"))
                            .isEqualTo("org.postgresql.Driver");
                    assertThat(context.getEnvironment().getProperty("spring.datasource.username"))
                            .isEqualTo("oidc");
                    assertThat(context.getEnvironment().getProperty("spring.datasource.password"))
                            .isEqualTo("test-password");
                });
    }
}
