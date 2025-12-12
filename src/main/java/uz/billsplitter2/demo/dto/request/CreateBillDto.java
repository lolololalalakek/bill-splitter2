package uz.billsplitter2.demo.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record CreateBillDto(
        @NotNull(message = "Party ID is required")
        UUID partyId
) {
}
