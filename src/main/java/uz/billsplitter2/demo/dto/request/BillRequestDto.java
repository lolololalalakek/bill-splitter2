package uz.billsplitter2.demo.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

@Builder
public record BillRequestDto(
        @NotNull @Valid @Size(min = 1, message = "At least one order item required")
        List<OrderItemRequestDto> items
) {
    public BillRequestDto {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
