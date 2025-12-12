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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.billsplitter2.demo.dto.request.AddGuestDto;
import uz.billsplitter2.demo.dto.response.GuestDto;
import uz.billsplitter2.demo.service.GuestService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/parties/{partyId}/guests")
@RequiredArgsConstructor
public class GuestController {

    private final GuestService guestService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
    public ResponseEntity<GuestDto> addGuest(
            @PathVariable UUID partyId,
            @Valid @RequestBody AddGuestDto dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(guestService.addGuest(partyId, dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
    public ResponseEntity<List<GuestDto>> getGuests(@PathVariable UUID partyId) {
        return ResponseEntity.ok(guestService.getGuestsByParty(partyId));
    }

    @DeleteMapping("/{guestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
    public ResponseEntity<Void> removeGuest(
            @PathVariable UUID partyId,
            @PathVariable UUID guestId
    ) {
        guestService.removeGuest(partyId, guestId);
        return ResponseEntity.noContent().build();
    }
}
