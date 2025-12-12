package uz.billsplitter2.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record AddGuestDto(
        @NotBlank(message = "Guest name is required")
        @Size(max = 100, message = "Guest name must not exceed 100 characters")
        String name
) {
}
