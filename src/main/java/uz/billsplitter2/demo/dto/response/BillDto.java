package uz.billsplitter2.demo.dto.response;

import lombok.Builder;
import uz.billsplitter2.demo.enums.BillStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record BillDto(
        UUID id,
        UUID partyId,
        String billNumber,
        BigDecimal itemsTotal,
        BigDecimal serviceFeePercent,
        BigDecimal serviceFeeAmount,
        BigDecimal totalAmount,
        BillStatus status,
        List<OrderItemDto> orderItems,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime closedAt
) {
}
