package uz.billsplitter2.demo.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}
