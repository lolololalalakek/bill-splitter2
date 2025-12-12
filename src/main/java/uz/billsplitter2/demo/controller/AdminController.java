package uz.billsplitter2.demo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.billsplitter2.demo.dto.request.CreateWaiterDto;
import uz.billsplitter2.demo.dto.request.UpdateWaiterDto;
import uz.billsplitter2.demo.dto.response.WaiterDto;
import uz.billsplitter2.demo.service.WaiterService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final WaiterService waiterService;

    // создание нового официанта (только админ)
    @PostMapping("/waiters")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WaiterDto> createWaiter(@Valid @RequestBody CreateWaiterDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(waiterService.createWaiter(dto));
    }

    // получение списка всех официантов (только админ)
    @GetMapping("/waiters")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<WaiterDto>> getAllWaiters() {
        return ResponseEntity.ok(waiterService.getAllWaiters());
    }

    // получение списка активных официантов (только админ)
    @GetMapping("/waiters/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<WaiterDto>> getActiveWaiters() {
        return ResponseEntity.ok(waiterService.getActiveWaiters());
    }

    @GetMapping("/waiters/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WaiterDto> getWaiterById(@PathVariable UUID id) {
        return ResponseEntity.ok(waiterService.getWaiterById(id));
    }

    // обновление данных официанта (только админ)
    @PutMapping("/waiters/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WaiterDto> updateWaiter(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWaiterDto dto
    ) {
        return ResponseEntity.ok(waiterService.updateWaiter(id, dto));
    }

    // деактивация официанта (только админ)
    @DeleteMapping("/waiters/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateWaiter(@PathVariable UUID id) {
        waiterService.deactivateWaiter(id);
        return ResponseEntity.noContent().build();
    }
}
