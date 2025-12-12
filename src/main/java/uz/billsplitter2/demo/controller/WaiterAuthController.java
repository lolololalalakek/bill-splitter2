package uz.billsplitter2.demo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.billsplitter2.demo.dto.request.LogoutRequest;
import uz.billsplitter2.demo.dto.request.RefreshTokenRequest;
import uz.billsplitter2.demo.dto.request.WaiterLoginRequest;
import uz.billsplitter2.demo.dto.response.WaiterAuthResponse;
import uz.billsplitter2.demo.dto.response.WaiterDto;
import uz.billsplitter2.demo.mapper.WaiterMapper;
import uz.billsplitter2.demo.security.SecurityContext;
import uz.billsplitter2.demo.service.WaiterAuthService;

@RestController
@RequestMapping("/api/v1/auth/waiters")
@RequiredArgsConstructor
public class WaiterAuthController {

    private final WaiterAuthService waiterAuthService;
    private final SecurityContext securityContext;
    private final WaiterMapper waiterMapper;

    @PostMapping("/login")
    public ResponseEntity<WaiterAuthResponse> login(@Valid @RequestBody WaiterLoginRequest request) {
        return ResponseEntity.ok(waiterAuthService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<WaiterAuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(waiterAuthService.refreshToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        waiterAuthService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('WAITER')")
    public ResponseEntity<WaiterDto> me() {
        return ResponseEntity.ok(waiterMapper.toDto(securityContext.getCurrentWaiter()));
    }
}
