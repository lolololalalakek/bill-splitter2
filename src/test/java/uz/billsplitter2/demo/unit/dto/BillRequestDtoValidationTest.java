package uz.billsplitter2.demo.unit.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uz.billsplitter2.demo.dto.request.BillRequestDto;
import uz.billsplitter2.demo.dto.request.OrderItemRequestDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BillRequestDtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    @DisplayName("valid payload passes validation")
    void validPayload() {
        BillRequestDto dto = new BillRequestDto(
            List.of(new OrderItemRequestDto("Salad", new BigDecimal("20"), List.of("Alice", "Bob")))
        );

        Set<ConstraintViolation<BillRequestDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("invalid payload reports violations")
    void invalidPayload() {
        BillRequestDto dto = new BillRequestDto(
            List.of(new OrderItemRequestDto(" ", new BigDecimal("-1"), List.of(" ", "")))
        );

        Set<ConstraintViolation<BillRequestDto>> violations = validator.validate(dto);

        assertThat(violations)
            .hasSizeGreaterThanOrEqualTo(1)
            .anyMatch(v -> v.getPropertyPath().toString().contains("items"));
    }

    @Test
    @DisplayName("null items collection fails validation")
    void nullItems() {
        BillRequestDto dto = new BillRequestDto(null);

        Set<ConstraintViolation<BillRequestDto>> violations = validator.validate(dto);

        assertThat(violations)
            .anyMatch(v -> v.getPropertyPath().toString().equals("items"));
    }
}
