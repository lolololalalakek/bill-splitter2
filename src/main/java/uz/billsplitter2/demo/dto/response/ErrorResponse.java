package uz.billsplitter2.demo.dto.response;

import lombok.Builder;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;

@Builder
public record ErrorResponse(
        String code,
        String message,
        int status,
        OffsetDateTime timestamp,
        String path
) {
    public static ErrorResponse of(String code, String message, HttpStatus status, String path) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .status(status.value())
                .timestamp(OffsetDateTime.now())
                .path(path)
                .build();
    }
}
