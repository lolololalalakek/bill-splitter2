package uz.billsplitter2.demo.dto.response;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record GuestDto(
        UUID id,
        UUID partyId,
        String name,
        OffsetDateTime createdAt
) {
}
