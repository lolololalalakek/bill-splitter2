package uz.billsplitter2.demo.dto.keycloak;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record KeycloakCredentialRepresentation(
        String type,
        String value,
        Boolean temporary
) {
    public static KeycloakCredentialRepresentation password(String password, boolean temporary) {
        return KeycloakCredentialRepresentation.builder()
                .type("password")
                .value(password)
                .temporary(temporary)
                .build();
    }
}
