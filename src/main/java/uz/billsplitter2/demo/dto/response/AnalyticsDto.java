package uz.billsplitter2.demo.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
public record AnalyticsDto(
        Long totalClosedBills,
        BigDecimal totalRevenue,
        BigDecimal totalServiceFees,
        BigDecimal averageBillAmount,
        OffsetDateTime periodStart,
        OffsetDateTime periodEnd
) {
}
