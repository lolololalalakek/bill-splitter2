package uz.billsplitter2.demo.dto.request;

import jakarta.validation.constraints.NotBlank;

public record WaiterLoginRequest(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {
}
