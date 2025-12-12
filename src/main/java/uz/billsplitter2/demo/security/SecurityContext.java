package uz.billsplitter2.demo.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import uz.billsplitter2.demo.entity.Waiter;
import uz.billsplitter2.demo.exception.ResourceNotFoundException;
import uz.billsplitter2.demo.exception.UnauthorizedException;
import uz.billsplitter2.demo.repository.WaiterRepository;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SecurityContext {

    private final WaiterRepository waiterRepository;

    public Waiter getCurrentWaiter() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            throw new UnauthorizedException("Invalid authentication principal");
        }

        String keycloakId = jwt.getSubject();
        return waiterRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("Waiter not found for keycloak ID: " + keycloakId));
    }

    public UUID getCurrentWaiterId() {
        return getCurrentWaiter().getId();
    }

    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }

    public boolean isWaiter() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_WAITER"));
    }

    public String getCurrentUsername() {
        return getCurrentWaiter().getUsername();
    }
}
