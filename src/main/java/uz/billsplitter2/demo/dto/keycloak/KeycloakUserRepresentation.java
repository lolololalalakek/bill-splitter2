package uz.billsplitter2.demo.dto.keycloak;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record KeycloakUserRepresentation(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        Boolean enabled,
        Boolean emailVerified,
        List<KeycloakCredentialRepresentation> credentials,
        List<String> realmRoles,
        Map<String, List<String>> clientRoles
) {
}
