package com.nhajc.server.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.nhajc.server.auth.controller.AuthController;
import com.nhajc.server.auth.dto.LoginRequest;
import com.nhajc.server.auth.service.AuthService;
import com.nhajc.server.common.exception.ApiExceptionHandler;
import com.nhajc.server.config.SecurityConfig;
import com.nhajc.server.storage.exception.ObjectStorageException;
import com.nhajc.server.user.controller.UserController;
import com.nhajc.server.user.dto.UpdateProfileRequest;
import com.nhajc.server.user.dto.UserResponse;
import com.nhajc.server.user.entity.UserRole;
import com.nhajc.server.user.service.UserService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = AuthHttpTest.TestConfig.class)
@TestPropertySource(properties = "JWT_SECRET=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
class AuthHttpTest {
    @Autowired private WebApplicationContext context;

    @Autowired private AuthService auth;

    @Autowired private UserService users;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        reset(auth, users);
        mvc =
                MockMvcBuilders.webAppContextSetup(context)
                        .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
                        .build();
    }

    @Test
    void publicRegisterAcceptsNormalizedIdentifiersWithoutBearer() throws Exception {
        mvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"username":"  dai  ","email":"  dai@example.com  ","password":"secret-pass-123"}
                                        """))
                .andExpect(status().isCreated());
        verify(auth)
                .register(new com.nhajc.server.auth.dto.RegisterRequest("dai", "dai@example.com", "secret-pass-123"));
    }

    @Test
    void invalidRegistrationReturns400BeforeCallingService() throws Exception {
        mvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"username":"dai@example.com","email":"dai@example.com","password":"short"}
                                        """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(auth);
    }

    @Test
    void unexpectedRegisterFailureReturnsGeneric500() throws Exception {
        when(auth.register(any())).thenThrow(new IllegalStateException("private database detail"));
        mvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"username":"dai","email":"dai@example.com","password":"secret-pass-123"}
                                        """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred. Please try again."));
    }

    @Test
    void unexpectedLoginFailureReturns500InsteadOf401() throws Exception {
        when(auth.login(any())).thenThrow(new IllegalStateException("private database detail"));
        mvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"usernameOrEmail":"dai","password":"secret-pass-123"}
                                        """))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void expected401And409ArePreserved() throws Exception {
        when(auth.login(any())).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad credentials"));
        mvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"usernameOrEmail":"dai","password":"wrong"}
                                        """))
                .andExpect(status().isUnauthorized());

        when(auth.register(any())).thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Already exists"));
        mvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"username":"dai","email":"dai@example.com","password":"secret-pass-123"}
                                        """))
                .andExpect(status().isConflict());
    }

    @Test
    void storageFailureStillReturns503() throws Exception {
        when(auth.login(any())).thenThrow(new ObjectStorageException("private storage detail", null));
        mvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"usernameOrEmail":"dai","password":"secret-pass-123"}
                                        """))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void errorDispatchIsAllowedButDirectUnauthenticatedRequestIsNot() throws Exception {
        mvc.perform(
                        get("/error")
                                .with(request -> {
                                    request.setDispatcherType(DispatcherType.ERROR);
                                    return request;
                                }))
                .andExpect(status().isInternalServerError());
        mvc.perform(get("/error")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void oldShortPasswordStillReachesLoginService() throws Exception {
        mvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"usernameOrEmail":" dai ","password":"old"}
                                        """))
                .andExpect(status().isOk());
        verify(auth).login(new LoginRequest("dai", "old"));
    }

    @Test
    void avatarFileIsPassedToProfileService() throws Exception {
        UUID userId = UUID.randomUUID();
        var file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[] {1, 2, 3});
        when(users.updateProfile(any(), any(), any())).thenReturn(profile(userId));
        mvc.perform(
                        multipart(HttpMethod.PUT, "/api/v1/users/me")
                                .file(file)
                                .file(new MockMultipartFile("data", "", "application/json", "{}".getBytes()))
                                .header("Authorization", "Bearer " + accessToken(userId)))
                .andExpect(status().isOk());
        verify(users).updateProfile(userId.toString(), new UpdateProfileRequest(), file);
    }

    @Test
    void duplicateFileAndTextPartAreRejected() throws Exception {
        UUID userId = UUID.randomUUID();
        var file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[] {1, 2, 3});
        when(users.updateProfile(any(), any(), any())).thenReturn(profile(userId));
        mvc.perform(
                        multipart(HttpMethod.PUT, "/api/v1/users/me")
                                .file(file)
                                .file(new MockMultipartFile("data", "", "application/json", "{}".getBytes()))
                                .param("file", "avatar.png")
                                .header("Authorization", "Bearer " + accessToken(userId)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(users);
    }

    @Test
    void avatarSentAsTextReturnsActionable400BeforeCallingService() throws Exception {
        mvc.perform(
                        multipart(HttpMethod.PUT, "/api/v1/users/me")
                                .file(new MockMultipartFile("data", "", "application/json", "{}".getBytes()))
                                .param("file", "C:/Pictures/avatar.png")
                                .header("Authorization", "Bearer " + accessToken(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.detail")
                                .value("file must be a File part in multipart/form-data, not text or a file path."));
        verifyNoInteractions(users);
    }

    @Test
    void multipartWithoutAvatarStillBindsNullForRemoval() throws Exception {
        UUID userId = UUID.randomUUID();
        when(users.updateProfile(any(), any(), any())).thenReturn(profile(userId));
        mvc.perform(
                        multipart(HttpMethod.PUT, "/api/v1/users/me")
                                .file(new MockMultipartFile("data", "", "application/json", "{}".getBytes()))
                                .header("Authorization", "Bearer " + accessToken(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").isEmpty());
        verify(users).updateProfile(userId.toString(), new UpdateProfileRequest(), null);
    }

    private String accessToken(UUID userId) {
        Instant now = Instant.now();
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer("nhajc")
                        .subject(userId.toString())
                        .issuedAt(now)
                        .expiresAt(now.plusSeconds(60))
                        .claim("role", "USER")
                        .build();
        return context.getBean(JwtEncoder.class)
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private UserResponse profile(UUID userId) {
        return new UserResponse(userId, "dai", "dai@example.com", Instant.now(), UserRole.USER, null);
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({
        SecurityConfig.class,
        AuthController.class,
        UserController.class,
        ApiExceptionHandler.class,
        com.nhajc.server.common.validation.MultipartRequests.class,
        ErrorEndpoint.class
    })
    static class TestConfig {
        @Bean
        AuthService authService() {
            return mock(AuthService.class);
        }

        @Bean
        UserService userService() {
            return mock(UserService.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @RestController
    static class ErrorEndpoint {
        @GetMapping("/error")
        ResponseEntity<Void> error() {
            return ResponseEntity.internalServerError().build();
        }
    }
}
