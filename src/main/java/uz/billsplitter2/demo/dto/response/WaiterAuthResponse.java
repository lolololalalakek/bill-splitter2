package uz.billsplitter2.demo.dto.response;

import lombok.Builder;

@Builder
public record WaiterAuthResponse(
        String accessToken,
        String refreshToken,
        Long expiresIn,
        String tokenType,
        WaiterDto waiter
) {
}
