package uz.billsplitter2.demo.dto.response;

import lombok.Builder;
import uz.billsplitter2.demo.enums.PartyStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record PartyDto(
        UUID id,
        UUID tableId,
        String tableNumber,
        UUID waiterId,
        String waiterUsername,
        PartyStatus status,
        List<GuestDto> guests,
        OffsetDateTime createdAt,
        OffsetDateTime closedAt
) {
}
