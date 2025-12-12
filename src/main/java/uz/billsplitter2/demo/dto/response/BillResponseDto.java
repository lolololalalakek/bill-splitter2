package uz.billsplitter2.demo.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record BillResponseDto(
        BigDecimal itemsTotal,
        BigDecimal serviceFeeApplied,
        BigDecimal totalToPay,
        List<ParticipantShareDto> shares
) {
    public BillResponseDto {
        shares = shares == null ? List.of() : List.copyOf(shares);
    }
}
