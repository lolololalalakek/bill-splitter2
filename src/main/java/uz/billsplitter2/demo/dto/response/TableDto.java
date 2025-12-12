package uz.billsplitter2.demo.dto.response;

import lombok.Builder;
import uz.billsplitter2.demo.enums.TableStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record TableDto(
        UUID id,
        String tableNumber,
        Integer capacity,
        TableStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
