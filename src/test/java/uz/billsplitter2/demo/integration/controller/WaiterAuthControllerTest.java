package uz.billsplitter2.demo.integration.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uz.billsplitter2.demo.config.SecurityConfig;
import uz.billsplitter2.demo.controller.WaiterAuthController;
import uz.billsplitter2.demo.dto.request.LogoutRequest;
import uz.billsplitter2.demo.dto.request.RefreshTokenRequest;
import uz.billsplitter2.demo.dto.request.WaiterLoginRequest;
import uz.billsplitter2.demo.dto.response.WaiterAuthResponse;
import uz.billsplitter2.demo.dto.response.WaiterDto;
import uz.billsplitter2.demo.entity.Waiter;
import uz.billsplitter2.demo.enums.WaiterRole;
import uz.billsplitter2.demo.exception.UnauthorizedException;
import uz.billsplitter2.demo.mapper.WaiterMapper;
import uz.billsplitter2.demo.security.KeycloakJwtConverter;
import uz.billsplitter2.demo.security.SecurityContext;
import uz.billsplitter2.demo.service.WaiterAuthService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WaiterAuthController.class)
@Import({SecurityConfig.class, KeycloakJwtConverter.class})
class WaiterAuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    WaiterAuthService waiterAuthService;

    @MockitoBean
    SecurityContext securityContext;

    @MockitoBean
    WaiterMapper waiterMapper;

    @Test
    @DisplayName("Login returns tokens and waiter data")
    void loginReturnsTokens() throws Exception {
        WaiterDto waiterDto = WaiterDto.builder()
                .id(UUID.randomUUID())
                .username("waiter1")
                .keycloakId("kc-id")
                .role(WaiterRole.WAITER)
                .active(true)
                .build();

        WaiterAuthResponse response = WaiterAuthResponse.builder()
                .accessToken("access")
                .refreshToken("refresh")
                .expiresIn(300L)
                .tokenType("Bearer")
                .waiter(waiterDto)
                .build();
        Mockito.when(waiterAuthService.login(any(WaiterLoginRequest.class))).thenReturn(response);

        String body = """
                {
                  "username": "waiter1",
                  "password": "pass"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/waiters/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"))
                .andExpect(jsonPath("$.waiter.username").value("waiter1"));
    }

    @Test
    @DisplayName("Login with invalid credentials returns 401")
    void loginWithInvalidCredentialsReturns401() throws Exception {
        Mockito.when(waiterAuthService.login(any(WaiterLoginRequest.class)))
                .thenThrow(new UnauthorizedException("Invalid credentials"));

        String body = """
                {
                  "username": "invalid",
                  "password": "wrong"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/waiters/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Refresh token returns new tokens and waiter data")
    void refreshTokenReturnsNewTokens() throws Exception {
        WaiterDto waiterDto = WaiterDto.builder()
                .id(UUID.randomUUID())
                .username("waiter2")
                .keycloakId("kc-id-2")
                .role(WaiterRole.WAITER)
                .active(true)
                .build();

        WaiterAuthResponse response = WaiterAuthResponse.builder()
                .accessToken("new-access")
                .refreshToken("new-refresh")
                .expiresIn(600L)
                .tokenType("Bearer")
                .waiter(waiterDto)
                .build();
        Mockito.when(waiterAuthService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(response);

        String body = """
                {
                  "refreshToken": "old-refresh"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/waiters/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"))
                .andExpect(jsonPath("$.expiresIn").value(600))
                .andExpect(jsonPath("$.waiter.username").value("waiter2"));
    }

    @Test
    @DisplayName("Refresh with invalid token returns 401")
    void refreshWithInvalidTokenReturns401() throws Exception {
        Mockito.when(waiterAuthService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new UnauthorizedException("Invalid refresh token"));

        String body = """
                {
                  "refreshToken": "invalid-refresh"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/waiters/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Logout returns 204 no content")
    void logoutReturnsNoContent() throws Exception {
        String body = """
                {
                  "refreshToken": "refresh-token"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/waiters/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        Mockito.verify(waiterAuthService).logout(any(LogoutRequest.class));
    }

    @Test
    @DisplayName("Logout with invalid token returns 401")
    void logoutWithInvalidTokenReturns401() throws Exception {
        Mockito.doThrow(new UnauthorizedException("Failed to logout from Keycloak"))
                .when(waiterAuthService).logout(any(LogoutRequest.class));

        String body = """
                {
                  "refreshToken": "invalid-refresh"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/waiters/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("me returns waiter profile for role WAITER")
    @WithMockUser(roles = "WAITER")
    void meReturnsWaiterProfile() throws Exception {
        UUID waiterId = UUID.randomUUID();
        Waiter waiter = Waiter.builder()
                .username("waiter2")
                .keycloakId("kc-2")
                .role(WaiterRole.WAITER)
                .active(true)
                .build();
        waiter.setId(waiterId);
        Mockito.when(securityContext.getCurrentWaiter()).thenReturn(waiter);

        WaiterDto dto = WaiterDto.builder()
                .id(waiterId)
                .username(waiter.getUsername())
                .keycloakId(waiter.getKeycloakId())
                .role(waiter.getRole())
                .active(waiter.getActive())
                .build();
        Mockito.when(waiterMapper.toDto(waiter)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/auth/waiters/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("waiter2"))
                .andExpect(jsonPath("$.keycloakId").value("kc-2"));

        Mockito.verify(securityContext).getCurrentWaiter();
        Mockito.verify(waiterMapper).toDto(eq(waiter));
    }

    @Test
    @DisplayName("me without auth returns 401")
    void meRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/auth/waiters/me"))
                .andExpect(status().isUnauthorized());
    }
}
