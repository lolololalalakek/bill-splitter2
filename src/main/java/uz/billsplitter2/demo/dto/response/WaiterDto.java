package uz.billsplitter2.demo.dto.response;

import lombok.Builder;
import uz.billsplitter2.demo.enums.WaiterRole;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record WaiterDto(
        UUID id,
        String keycloakId,
        String username,
        String email,
        String fullName,
        WaiterRole role,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
