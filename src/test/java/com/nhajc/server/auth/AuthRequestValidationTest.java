package com.nhajc.server.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.nhajc.server.auth.dto.LoginRequest;
import com.nhajc.server.auth.dto.RegisterRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class AuthRequestValidationTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void stripsIdentifiersBeforeValidationButPreservesPassword() {
        var request = new RegisterRequest("  dai  ", "  dai@example.com  ", "  secret pass  ");
        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.username()).isEqualTo("dai");
        assertThat(request.email()).isEqualTo("dai@example.com");
        assertThat(request.password()).isEqualTo("  secret pass  ");
    }

    @Test
    void acceptsLengthBoundaries() {
        assertThat(validator.validate(new RegisterRequest("abc", "dai@example.com", "x".repeat(12)))).isEmpty();
        assertThat(validator.validate(new RegisterRequest("u".repeat(50), "dai@example.com", "x".repeat(72))))
                .isEmpty();
    }

    @ParameterizedTest
    @MethodSource("invalidRegistrations")
    void rejectsInvalidRegistration(RegisterRequest request) {
        assertThat(validator.validate(request)).isNotEmpty();
    }

    static Stream<RegisterRequest> invalidRegistrations() {
        return Stream.of(
                new RegisterRequest(null, "dai@example.com", "x".repeat(12)),
                new RegisterRequest("  ", "dai@example.com", "x".repeat(12)),
                new RegisterRequest("ab", "dai@example.com", "x".repeat(12)),
                new RegisterRequest("u".repeat(51), "dai@example.com", "x".repeat(12)),
                new RegisterRequest("dai@example.com", "dai@example.com", "x".repeat(12)),
                new RegisterRequest("dai user", "dai@example.com", "x".repeat(12)),
                new RegisterRequest("dai\u00a0user", "dai@example.com", "x".repeat(12)),
                new RegisterRequest("dai", null, "x".repeat(12)),
                new RegisterRequest("dai", "invalid", "x".repeat(12)),
                new RegisterRequest("dai", "a".repeat(255) + "@example.com", "x".repeat(12)),
                new RegisterRequest("dai", "dai@example.com", null),
                new RegisterRequest("dai", "dai@example.com", " ".repeat(12)),
                new RegisterRequest("dai", "dai@example.com", "x".repeat(11)),
                new RegisterRequest("dai", "dai@example.com", "x".repeat(73)));
    }

    @Test
    void loginPreservesLegacyShortPasswordsAndStripsIdentifier() {
        var request = new LoginRequest("  Dai@Example.com  ", "old");
        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.usernameOrEmail()).isEqualTo("Dai@Example.com");
        assertThat(request.password()).isEqualTo("old");
    }

    @Test
    void loginRejectsOversizedIdentifier() {
        assertThat(validator.validate(new LoginRequest("u".repeat(255), "password"))).isNotEmpty();
    }
}
