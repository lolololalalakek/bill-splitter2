package uz.billsplitter2.demo.dto.response;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ParticipantShareDto(
        String name,
        BigDecimal itemsTotal,
        BigDecimal serviceFeeShare,
        BigDecimal total,
        BigDecimal percentage
) {
}
