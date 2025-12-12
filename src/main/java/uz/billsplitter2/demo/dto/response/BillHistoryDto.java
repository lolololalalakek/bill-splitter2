package uz.billsplitter2.demo.dto.response;

import lombok.Builder;
import uz.billsplitter2.demo.enums.BillStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record BillHistoryDto(
        UUID id,
        String billNumber,
        UUID partyId,
        UUID tableId,
        String tableNumber,
        UUID waiterId,
        String waiterUsername,
        String waiterFullName,
        BigDecimal itemsTotal,
        BigDecimal serviceFeePercent,
        BigDecimal serviceFeeAmount,
        BigDecimal totalAmount,
        BillStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime closedAt
) {
}
