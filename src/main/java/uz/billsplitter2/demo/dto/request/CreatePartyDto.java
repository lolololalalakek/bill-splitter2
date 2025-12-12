package uz.billsplitter2.demo.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record CreatePartyDto(
        @NotNull(message = "Table ID is required")
        UUID tableId
) {
}
