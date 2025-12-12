package uz.billsplitter2.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record OrderItemRequestDto(
        @NotBlank String name,
        @NotNull @Positive BigDecimal price,
        @NotNull @NotEmpty List<@NotBlank String> participants
) {
    public OrderItemRequestDto {
        // удаление дубликатов и trim
        participants = participants == null
                ? List.of()
                : participants.stream()
                    .map(String::trim)
                    .distinct()
                    .toList();
    }
}
