package uz.billsplitter2.demo.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import uz.billsplitter2.demo.dto.keycloak.KeycloakCredentialRepresentation;
import uz.billsplitter2.demo.dto.keycloak.KeycloakUserRepresentation;
import uz.billsplitter2.demo.dto.response.KeycloakTokenResponse;
import uz.billsplitter2.demo.exception.KeycloakException;
import uz.billsplitter2.demo.service.KeycloakAdminService;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminServiceImpl implements KeycloakAdminService {

    private final RestClient restClient;

    @Value("${keycloak.admin.realm:billsplitter}")
    private String realm;

    @Value("${keycloak.admin.server-url:http://localhost:8080}")
    private String serverUrl;

    @Value("${keycloak.admin.client-id:admin-cli}")
    private String adminClientId;

    @Value("${keycloak.admin.username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin.password:admin}")
    private String adminPassword;

    @Override
    public String createUser(KeycloakUserRepresentation user) {
        String accessToken = getAdminAccessToken();
        String url = String.format("%s/admin/realms/%s/users", serverUrl, realm);

        try {
            restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(user)
                    .retrieve()
                    .toBodilessEntity();

            // Get the created user ID from Location header or by searching
            return getUserIdByUsername(user.username(), accessToken);
        } catch (RestClientResponseException e) {
            log.error("Failed to create user in Keycloak: {}", e.getResponseBodyAsString());
            throw new KeycloakException("Failed to create user in Keycloak: " + e.getMessage());
        }
    }

    @Override
    public void updateUser(String userId, KeycloakUserRepresentation user) {
        String accessToken = getAdminAccessToken();
        String url = String.format("%s/admin/realms/%s/users/%s", serverUrl, realm, userId);

        try {
            restClient.put()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(user)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.error("Failed to update user in Keycloak: {}", e.getResponseBodyAsString());
            throw new KeycloakException("Failed to update user in Keycloak: " + e.getMessage());
        }
    }

    @Override
    public void deleteUser(String userId) {
        String accessToken = getAdminAccessToken();
        String url = String.format("%s/admin/realms/%s/users/%s", serverUrl, realm, userId);

        try {
            restClient.delete()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.error("Failed to delete user from Keycloak: {}", e.getResponseBodyAsString());
            throw new KeycloakException("Failed to delete user from Keycloak: " + e.getMessage());
        }
    }

    @Override
    public void setUserEnabled(String userId, boolean enabled) {
        String accessToken = getAdminAccessToken();
        KeycloakUserRepresentation update = KeycloakUserRepresentation.builder()
                .enabled(enabled)
                .build();

        String url = String.format("%s/admin/realms/%s/users/%s", serverUrl, realm, userId);

        try {
            restClient.put()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(update)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.error("Failed to update user enabled status in Keycloak: {}", e.getResponseBodyAsString());
            throw new KeycloakException("Failed to update user enabled status: " + e.getMessage());
        }
    }

    @Override
    public void resetPassword(String userId, String newPassword, boolean temporary) {
        String accessToken = getAdminAccessToken();
        String url = String.format("%s/admin/realms/%s/users/%s/reset-password", serverUrl, realm, userId);

        KeycloakCredentialRepresentation credential = KeycloakCredentialRepresentation.password(newPassword, temporary);

        try {
            restClient.put()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(credential)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.error("Failed to reset password in Keycloak: {}", e.getResponseBodyAsString());
            throw new KeycloakException("Failed to reset password: " + e.getMessage());
        }
    }

    @Override
    public void assignRealmRole(String userId, String roleName) {
        String accessToken = getAdminAccessToken();

        // Get role representation
        String getRoleUrl = String.format("%s/admin/realms/%s/roles/%s", serverUrl, realm, roleName);
        Map<String, Object> role;

        try {
            role = restClient.get()
                    .uri(getRoleUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException e) {
            log.error("Failed to get role from Keycloak: {}", e.getResponseBodyAsString());
            throw new KeycloakException("Failed to get role: " + e.getMessage());
        }

        // Assign role to user
        String assignRoleUrl = String.format("%s/admin/realms/%s/users/%s/role-mappings/realm", serverUrl, realm, userId);

        try {
            restClient.post()
                    .uri(assignRoleUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(role))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.error("Failed to assign role in Keycloak: {}", e.getResponseBodyAsString());
            throw new KeycloakException("Failed to assign role: " + e.getMessage());
        }
    }

    @Override
    public void removeRealmRole(String userId, String roleName) {
        String accessToken = getAdminAccessToken();

        // Get role representation
        String getRoleUrl = String.format("%s/admin/realms/%s/roles/%s", serverUrl, realm, roleName);
        Map<String, Object> role;

        try {
            role = restClient.get()
                    .uri(getRoleUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException e) {
            log.error("Failed to get role from Keycloak: {}", e.getResponseBodyAsString());
            throw new KeycloakException("Failed to get role: " + e.getMessage());
        }

        // Remove role from user
        String removeRoleUrl = String.format("%s/admin/realms/%s/users/%s/role-mappings/realm", serverUrl, realm, userId);

        try {
            restClient.method(HttpMethod.DELETE)
                    .uri(removeRoleUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(role))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.error("Failed to remove role in Keycloak: {}", e.getResponseBodyAsString());
            throw new KeycloakException("Failed to remove role: " + e.getMessage());
        }
    }

    private String getAdminAccessToken() {
        String tokenUrl = String.format("%s/realms/master/protocol/openid-connect/token", serverUrl);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", adminClientId);
        form.add("username", adminUsername);
        form.add("password", adminPassword);

        try {
            KeycloakTokenResponse response = restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KeycloakTokenResponse.class);

            if (response == null || response.accessToken() == null) {
                throw new KeycloakException("Failed to obtain admin access token");
            }

            return response.accessToken();
        } catch (RestClientResponseException e) {
            log.error("Failed to get admin access token: {}", e.getResponseBodyAsString());
            throw new KeycloakException("Failed to authenticate as admin: " + e.getMessage());
        }
    }

    private String getUserIdByUsername(String username, String accessToken) {
        String url = String.format("%s/admin/realms/%s/users?username=%s&exact=true", serverUrl, realm, username);

        try {
            List<Map<String, Object>> users = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(List.class);

            if (users == null || users.isEmpty()) {
                throw new KeycloakException("User not found after creation: " + username);
            }

            return (String) users.get(0).get("id");
        } catch (RestClientResponseException e) {
            log.error("Failed to get user ID from Keycloak: {}", e.getResponseBodyAsString());
            throw new KeycloakException("Failed to get user ID: " + e.getMessage());
        }
    }
}
