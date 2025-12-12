package uz.billsplitter2.demo.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Builder;
import uz.billsplitter2.demo.enums.TableStatus;

@Builder
public record UpdateTableDto(
        @Min(value = 1, message = "Capacity must be at least 1")
        Integer capacity,

        TableStatus status
) {
}
