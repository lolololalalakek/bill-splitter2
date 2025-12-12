package uz.billsplitter2.demo.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record OrderItemDto(
        UUID id,
        UUID billId,
        String name,
        BigDecimal price,
        Integer quantity,
        List<GuestDto> guests,
        OffsetDateTime createdAt
) {
}
