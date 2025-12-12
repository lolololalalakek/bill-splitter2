package uz.billsplitter2.demo.unit.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import uz.billsplitter2.demo.dto.request.LogoutRequest;
import uz.billsplitter2.demo.dto.request.RefreshTokenRequest;
import uz.billsplitter2.demo.dto.request.WaiterLoginRequest;
import uz.billsplitter2.demo.dto.response.KeycloakTokenResponse;
import uz.billsplitter2.demo.dto.response.WaiterAuthResponse;
import uz.billsplitter2.demo.dto.response.WaiterDto;
import uz.billsplitter2.demo.entity.Waiter;
import uz.billsplitter2.demo.enums.WaiterRole;
import uz.billsplitter2.demo.exception.UnauthorizedException;
import uz.billsplitter2.demo.mapper.WaiterMapper;
import uz.billsplitter2.demo.repository.WaiterRepository;
import uz.billsplitter2.demo.service.impl.WaiterAuthServiceImpl;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaiterAuthServiceImplTest {

    @Mock
    RestClient restClient;

    @Mock
    RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    RestClient.RequestBodySpec requestBodySpec;

    @Mock
    RestClient.ResponseSpec responseSpec;

    @Mock
    JwtDecoder jwtDecoder;

    @Mock
    WaiterRepository waiterRepository;

    @Mock
    WaiterMapper waiterMapper;

    @Captor
    ArgumentCaptor<Waiter> waiterCaptor;

    @InjectMocks
    WaiterAuthServiceImpl waiterAuthService;

    private final String tokenUri = "http://localhost:8080/realms/billsplitter/protocol/openid-connect/token";
    private final String clientId = "billsplitter-api";
    private final String clientSecret = "secret";

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(waiterAuthService, "tokenUri", tokenUri);
        ReflectionTestUtils.setField(waiterAuthService, "clientId", clientId);
        ReflectionTestUtils.setField(waiterAuthService, "clientSecret", clientSecret);
        ReflectionTestUtils.setField(waiterAuthService, "logoutUri", "http://localhost:8080/realms/billsplitter/protocol/openid-connect/logout");
        mockRestClientChain();
    }

    @Test
    @DisplayName("Successful login returns tokens and waiter data")
    void loginSuccess() {
        KeycloakTokenResponse tokenResponse = new KeycloakTokenResponse("access", "refresh", 300L, "Bearer", "openid");
        when(responseSpec.body(KeycloakTokenResponse.class)).thenReturn(tokenResponse);

        Jwt jwt = Jwt.withTokenValue("access")
                .header("alg", "RS256")
                .subject("kc-id-1")
                .claim("preferred_username", "waiter1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        when(jwtDecoder.decode("access")).thenReturn(jwt);

        UUID waiterId = UUID.randomUUID();
        Waiter waiter = Waiter.builder()
                .username("waiter1")
                .keycloakId("kc-id-1")
                .role(WaiterRole.WAITER)
                .active(true)
                .build();
        waiter.setId(waiterId);
        when(waiterRepository.findByKeycloakId("kc-id-1")).thenReturn(Optional.of(waiter));

        WaiterDto waiterDto = WaiterDto.builder()
                .id(waiterId)
                .username(waiter.getUsername())
                .keycloakId(waiter.getKeycloakId())
                .role(waiter.getRole())
                .active(waiter.getActive())
                .build();
        when(waiterMapper.toDto(waiter)).thenReturn(waiterDto);

        WaiterLoginRequest request = new WaiterLoginRequest("waiter1", "password");

        WaiterAuthResponse response = waiterAuthService.login(request);

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.waiter()).isEqualTo(waiterDto);

        verify(waiterRepository, never()).save(any());
    }

    @Test
    @DisplayName("Missing keycloakId is stored after successful login")
    void loginUpdatesMissingKeycloakId() {
        KeycloakTokenResponse tokenResponse = new KeycloakTokenResponse("access2", "refresh2", 300L, "Bearer", "openid");
        when(responseSpec.body(KeycloakTokenResponse.class)).thenReturn(tokenResponse);

        Jwt jwt = Jwt.withTokenValue("access2")
                .header("alg", "RS256")
                .subject("kc-id-2")
                .claim("preferred_username", "waiter2")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        when(jwtDecoder.decode("access2")).thenReturn(jwt);

        UUID waiterId = UUID.randomUUID();
        Waiter waiter = Waiter.builder()
                .username("waiter2")
                .keycloakId(null)
                .role(WaiterRole.WAITER)
                .active(true)
                .build();
        waiter.setId(waiterId);

        when(waiterRepository.findByKeycloakId("kc-id-2")).thenReturn(Optional.empty());
        when(waiterRepository.findByUsername("waiter2")).thenReturn(Optional.of(waiter));
        when(waiterRepository.save(any(Waiter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WaiterDto waiterDto = WaiterDto.builder()
                .id(waiterId)
                .username(waiter.getUsername())
                .keycloakId("kc-id-2")
                .role(waiter.getRole())
                .active(waiter.getActive())
                .build();
        when(waiterMapper.toDto(any(Waiter.class))).thenReturn(waiterDto);

        WaiterAuthResponse response = waiterAuthService.login(new WaiterLoginRequest("waiter2", "password"));

        assertThat(response.waiter().keycloakId()).isEqualTo("kc-id-2");
        verify(waiterRepository).save(waiterCaptor.capture());
        assertThat(waiterCaptor.getValue().getKeycloakId()).isEqualTo("kc-id-2");
    }

    @Test
    @DisplayName("Keycloak rejection results in UnauthorizedException")
    void loginFailsWhenKeycloakRejects() {
        when(responseSpec.body(KeycloakTokenResponse.class))
                .thenThrow(new RestClientResponseException("bad", 401, "Unauthorized", null, null, null));

        assertThatThrownBy(() -> waiterAuthService.login(new WaiterLoginRequest("u", "p")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid credentials");

        verify(jwtDecoder, never()).decode(anyString());
    }

    @Test
    @DisplayName("Bad access token decoding results in UnauthorizedException")
    void loginFailsOnBadToken() {
        when(responseSpec.body(KeycloakTokenResponse.class))
                .thenReturn(new KeycloakTokenResponse("bad-token", "r", 0L, "Bearer", "openid"));
        when(jwtDecoder.decode("bad-token")).thenThrow(new JwtException("fail"));

        assertThatThrownBy(() -> waiterAuthService.login(new WaiterLoginRequest("u", "p")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Unable to decode access token");
    }

    @Test
    @DisplayName("Successful refresh token returns new tokens and waiter data")
    void refreshTokenSuccess() {
        KeycloakTokenResponse tokenResponse = new KeycloakTokenResponse("new-access", "new-refresh", 600L, "Bearer", "openid");
        when(responseSpec.body(KeycloakTokenResponse.class)).thenReturn(tokenResponse);

        Jwt jwt = Jwt.withTokenValue("new-access")
                .header("alg", "RS256")
                .subject("kc-id-3")
                .claim("preferred_username", "waiter3")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        when(jwtDecoder.decode("new-access")).thenReturn(jwt);

        UUID waiterId = UUID.randomUUID();
        Waiter waiter = Waiter.builder()
                .username("waiter3")
                .keycloakId("kc-id-3")
                .role(WaiterRole.WAITER)
                .active(true)
                .build();
        waiter.setId(waiterId);
        when(waiterRepository.findByKeycloakId("kc-id-3")).thenReturn(Optional.of(waiter));

        WaiterDto waiterDto = WaiterDto.builder()
                .id(waiterId)
                .username(waiter.getUsername())
                .keycloakId(waiter.getKeycloakId())
                .role(waiter.getRole())
                .active(waiter.getActive())
                .build();
        when(waiterMapper.toDto(waiter)).thenReturn(waiterDto);

        RefreshTokenRequest request = new RefreshTokenRequest("old-refresh-token");
        WaiterAuthResponse response = waiterAuthService.refreshToken(request);

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        assertThat(response.expiresIn()).isEqualTo(600L);
        assertThat(response.waiter()).isEqualTo(waiterDto);
    }

    @Test
    @DisplayName("Invalid refresh token throws UnauthorizedException")
    void refreshTokenFailsOnInvalidToken() {
        when(responseSpec.body(KeycloakTokenResponse.class))
                .thenThrow(new RestClientResponseException("bad", 400, "Bad Request", null, null, null));

        assertThatThrownBy(() -> waiterAuthService.refreshToken(new RefreshTokenRequest("invalid-refresh")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    @DisplayName("Refresh token for non-existent waiter throws UnauthorizedException")
    void refreshTokenFailsForNonExistentWaiter() {
        KeycloakTokenResponse tokenResponse = new KeycloakTokenResponse("new-access", "new-refresh", 600L, "Bearer", "openid");
        when(responseSpec.body(KeycloakTokenResponse.class)).thenReturn(tokenResponse);

        Jwt jwt = Jwt.withTokenValue("new-access")
                .header("alg", "RS256")
                .subject("kc-id-unknown")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        when(jwtDecoder.decode("new-access")).thenReturn(jwt);
        when(waiterRepository.findByKeycloakId("kc-id-unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> waiterAuthService.refreshToken(new RefreshTokenRequest("refresh-token")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Waiter is not registered in the system");
    }

    @Test
    @DisplayName("Successful logout completes without errors")
    void logoutSuccess() {
        when(responseSpec.toBodilessEntity()).thenReturn(null);

        LogoutRequest request = new LogoutRequest("refresh-token");
        waiterAuthService.logout(request);

        verify(restClient).post();
    }

    @Test
    @DisplayName("Logout with invalid token throws UnauthorizedException")
    void logoutFailsOnInvalidToken() {
        when(responseSpec.toBodilessEntity())
                .thenThrow(new RestClientResponseException("bad", 400, "Bad Request", null, null, null));

        assertThatThrownBy(() -> waiterAuthService.logout(new LogoutRequest("invalid-refresh")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Failed to logout from Keycloak");
    }

    private void mockRestClientChain() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(MultiValueMap.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }
}
