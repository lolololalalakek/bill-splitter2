package uz.billsplitter2.demo.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import uz.billsplitter2.demo.dto.request.LogoutRequest;
import uz.billsplitter2.demo.dto.request.RefreshTokenRequest;
import uz.billsplitter2.demo.dto.request.WaiterLoginRequest;
import uz.billsplitter2.demo.dto.response.KeycloakTokenResponse;
import uz.billsplitter2.demo.dto.response.WaiterAuthResponse;
import uz.billsplitter2.demo.entity.Waiter;
import uz.billsplitter2.demo.exception.UnauthorizedException;
import uz.billsplitter2.demo.mapper.WaiterMapper;
import uz.billsplitter2.demo.repository.WaiterRepository;
import uz.billsplitter2.demo.service.WaiterAuthService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class WaiterAuthServiceImpl implements WaiterAuthService {

    private final RestClient restClient;
    private final JwtDecoder jwtDecoder;
    private final WaiterRepository waiterRepository;
    private final WaiterMapper waiterMapper;

    @Value("${keycloak.token-uri:${spring.security.oauth2.resourceserver.jwt.issuer-uri}/protocol/openid-connect/token}")
    private String tokenUri;

    @Value("${keycloak.client-id:billsplitter-api}")
    private String clientId;

    @Value("${keycloak.client-secret:}")
    private String clientSecret;

    @Value("${keycloak.logout-uri:${spring.security.oauth2.resourceserver.jwt.issuer-uri}/protocol/openid-connect/logout}")
    private String logoutUri;

    @Override
    public WaiterAuthResponse login(WaiterLoginRequest request) {
        KeycloakTokenResponse tokenResponse = requestToken(request.username(), request.password());
        Jwt jwt = decodeToken(tokenResponse.accessToken());
        Waiter waiter = resolveWaiter(jwt, request.username());

        return WaiterAuthResponse.builder()
                .accessToken(tokenResponse.accessToken())
                .refreshToken(tokenResponse.refreshToken())
                .expiresIn(tokenResponse.expiresIn())
                .tokenType(tokenResponse.tokenType())
                .waiter(waiterMapper.toDto(waiter))
                .build();
    }

    private KeycloakTokenResponse requestToken(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", clientId);
        if (clientSecret != null && !clientSecret.isBlank()) {
            form.add("client_secret", clientSecret);
        }
        form.add("username", username);
        form.add("password", password);

        try {
            KeycloakTokenResponse body = restClient.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KeycloakTokenResponse.class);

            if (body != null) {
                return body;
            }
            throw new UnauthorizedException("Failed to obtain token from Keycloak");
        } catch (RestClientResponseException ex) {
            throw new UnauthorizedException("Invalid credentials");
        }
    }

    private Jwt decodeToken(String accessToken) {
        try {
            return jwtDecoder.decode(accessToken);
        } catch (JwtException ex) {
            throw new UnauthorizedException("Unable to decode access token");
        }
    }

    private Waiter resolveWaiter(Jwt jwt, String fallbackUsername) {
        String keycloakId = jwt.getSubject();
        String usernameClaim = jwt.getClaimAsString("preferred_username");

        return waiterRepository.findByKeycloakId(keycloakId)
                .or(() -> Optional.ofNullable(usernameClaim).flatMap(waiterRepository::findByUsername))
                .or(() -> waiterRepository.findByUsername(fallbackUsername))
                .map(waiter -> {
                    if (waiter.getKeycloakId() == null || waiter.getKeycloakId().isBlank()) {
                        waiter.setKeycloakId(keycloakId);
                        return waiterRepository.save(waiter);
                    }
                    return waiter;
                })
                .orElseThrow(() -> new UnauthorizedException("Waiter is not registered in the system"));
    }

    @Override
    public WaiterAuthResponse refreshToken(RefreshTokenRequest request) {
        KeycloakTokenResponse tokenResponse = refreshKeycloakToken(request.refreshToken());
        Jwt jwt = decodeToken(tokenResponse.accessToken());
        Waiter waiter = resolveWaiterByKeycloakId(jwt.getSubject());

        return WaiterAuthResponse.builder()
                .accessToken(tokenResponse.accessToken())
                .refreshToken(tokenResponse.refreshToken())
                .expiresIn(tokenResponse.expiresIn())
                .tokenType(tokenResponse.tokenType())
                .waiter(waiterMapper.toDto(waiter))
                .build();
    }

    @Override
    public void logout(LogoutRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        if (clientSecret != null && !clientSecret.isBlank()) {
            form.add("client_secret", clientSecret);
        }
        form.add("refresh_token", request.refreshToken());

        try {
            restClient.post()
                    .uri(logoutUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw new UnauthorizedException("Failed to logout from Keycloak");
        }
    }

    private KeycloakTokenResponse refreshKeycloakToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", clientId);
        if (clientSecret != null && !clientSecret.isBlank()) {
            form.add("client_secret", clientSecret);
        }
        form.add("refresh_token", refreshToken);

        try {
            KeycloakTokenResponse body = restClient.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KeycloakTokenResponse.class);

            if (body != null) {
                return body;
            }
            throw new UnauthorizedException("Failed to refresh token from Keycloak");
        } catch (RestClientResponseException ex) {
            throw new UnauthorizedException("Invalid refresh token");
        }
    }

    private Waiter resolveWaiterByKeycloakId(String keycloakId) {
        return waiterRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UnauthorizedException("Waiter is not registered in the system"));
    }
}
