package uz.billsplitter2.demo.integration.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uz.billsplitter2.demo.controller.BillController;
import uz.billsplitter2.demo.dto.request.BillRequestDto;
import uz.billsplitter2.demo.dto.response.BillResponseDto;
import uz.billsplitter2.demo.dto.response.ParticipantShareDto;
import uz.billsplitter2.demo.exception.ValidationException;
import uz.billsplitter2.demo.security.KeycloakJwtConverter;
import uz.billsplitter2.demo.service.BillCalculationService;
import uz.billsplitter2.demo.service.BillManagementService;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BillController.class)
@Import(KeycloakJwtConverter.class)
class BillControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    BillCalculationService billCalculationService;

    @MockitoBean
    BillManagementService billManagementService;

    @Test
    @DisplayName("calculate endpoint returns computed shares for valid request")
    @WithMockUser(roles = "WAITER")
    void calculateReturnsResult() throws Exception {
        BillResponseDto stubResponse = BillResponseDto.builder()
                .itemsTotal(new BigDecimal("95.00"))
                .serviceFeeApplied(new BigDecimal("9.50"))
                .totalToPay(new BigDecimal("104.50"))
                .shares(List.of(
                        new ParticipantShareDto("Alice", new BigDecimal("35.00"), new BigDecimal("3.50"), new BigDecimal("38.50"), new BigDecimal("36.84")),
                        new ParticipantShareDto("Bob", new BigDecimal("40.00"), new BigDecimal("4.00"), new BigDecimal("44.00"), new BigDecimal("42.11"))
                ))
                .build();
        Mockito.when(billCalculationService.split(any(BillRequestDto.class))).thenReturn(stubResponse);

        String body = """
                {
                  "items": [
                    {"name": "Pasta", "price": 50, "participants": ["Alice", "Bob"]}
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/bills/calculate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemsTotal").value(95.0))
                .andExpect(jsonPath("$.shares[0].name").value("Alice"))
                .andExpect(jsonPath("$.shares[1].total").value(44.0));

        Mockito.verify(billCalculationService).split(any(BillRequestDto.class));
    }

    @Test
    @DisplayName("calculate endpoint requires authentication")
    void calculateRequiresAuth() throws Exception {
        String body = """
                {
                  "items": [
                    {"name": "Pasta", "price": 50, "participants": ["Alice"]}
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/bills/calculate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        Mockito.verifyNoInteractions(billCalculationService);
    }

    @Test
    @DisplayName("calculate endpoint validates input and returns 400 on invalid payload")
    @WithMockUser(roles = "WAITER")
    void calculateValidatesInput() throws Exception {
        String invalidBody = """
                {
                  "items": []
                }
                """;

        mockMvc.perform(post("/api/v1/bills/calculate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(billCalculationService);
    }

    @Test
    @DisplayName("handler wraps ValidationException into error response")
    @WithMockUser(roles = "WAITER")
    void handlesApplicationValidationException() throws Exception {
        Mockito.when(billCalculationService.split(any(BillRequestDto.class)))
                .thenThrow(new ValidationException("Invalid data"));

        String body = """
                {
                  "items": [
                    {"name": "Tea", "price": 10, "participants": ["Solo"]}
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/bills/calculate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid data"));
    }

    @Test
    @DisplayName("handler returns 500 for unexpected errors")
    @WithMockUser(roles = "WAITER")
    void handlesUnexpectedException() throws Exception {
        Mockito.when(billCalculationService.split(any(BillRequestDto.class)))
                .thenThrow(new RuntimeException("boom"));

        String body = """
                {
                  "items": [
                    {"name": "Tea", "price": 10, "participants": ["Solo"]}
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/bills/calculate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @Test
    @DisplayName("ADMIN role can access calculate endpoint")
    @WithMockUser(roles = "ADMIN")
    void adminCanCalculate() throws Exception {
        BillResponseDto stubResponse = BillResponseDto.builder()
                .itemsTotal(new BigDecimal("10.00"))
                .serviceFeeApplied(new BigDecimal("1.00"))
                .totalToPay(new BigDecimal("11.00"))
                .shares(List.of())
                .build();
        Mockito.when(billCalculationService.split(any(BillRequestDto.class))).thenReturn(stubResponse);

        String body = """
                {
                  "items": [
                    {"name": "Coffee", "price": 10, "participants": ["Admin"]}
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/bills/calculate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        Mockito.verify(billCalculationService).split(any(BillRequestDto.class));
    }
}
