package uz.billsplitter2.demo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.billsplitter2.demo.dto.request.CreatePartyDto;
import uz.billsplitter2.demo.dto.response.PartyDto;
import uz.billsplitter2.demo.security.SecurityContext;
import uz.billsplitter2.demo.service.PartyService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/parties")
@RequiredArgsConstructor
public class PartyController {

    private final PartyService partyService;
    private final SecurityContext securityContext;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
    public ResponseEntity<PartyDto> createParty(@Valid @RequestBody CreatePartyDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partyService.createParty(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
    public ResponseEntity<List<PartyDto>> getParties(@RequestParam(required = false) UUID waiterId) {
        if (waiterId != null) {
            return ResponseEntity.ok(partyService.getAllPartiesByWaiter(waiterId));
        }
        if (securityContext.isAdmin()) {
            return ResponseEntity.ok(partyService.getAllActiveParties());
        }
        return ResponseEntity.ok(partyService.getMyParties());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
    public ResponseEntity<PartyDto> getPartyById(@PathVariable UUID id) {
        return ResponseEntity.ok(partyService.getPartyById(id));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
    public ResponseEntity<PartyDto> closeParty(@PathVariable UUID id) {
        return ResponseEntity.ok(partyService.closeParty(id));
    }
}
