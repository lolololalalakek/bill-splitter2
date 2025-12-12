package uz.billsplitter2.demo.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Value("${keycloak.client-id:billsplitter-api}")
    private String clientId;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new JwtAuthenticationToken(jwt, authorities);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Set<String> roles = new HashSet<>();

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            Object realmRoles = realmAccess.get("roles");
            if (realmRoles instanceof Collection<?> collection) {
                collection.stream()
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .forEach(roles::add);
            }
        }

        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null && clientId != null && resourceAccess.containsKey(clientId)) {
            Object clientAccess = resourceAccess.get(clientId);
            if (clientAccess instanceof Map<?, ?> clientMap && clientMap.containsKey("roles")) {
                Object clientRoles = clientMap.get("roles");
                if (clientRoles instanceof Collection<?> collection) {
                    collection.stream()
                            .filter(Objects::nonNull)
                            .map(Object::toString)
                            .forEach(roles::add);
                }
            }
        }

        return roles.stream()
                .map(role -> role.toUpperCase().startsWith("ROLE_") ? role.toUpperCase() : "ROLE_" + role.toUpperCase())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }
}
