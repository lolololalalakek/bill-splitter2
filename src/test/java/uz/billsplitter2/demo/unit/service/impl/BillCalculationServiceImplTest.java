package uz.billsplitter2.demo.unit.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uz.billsplitter2.demo.dto.request.BillRequestDto;
import uz.billsplitter2.demo.dto.request.OrderItemRequestDto;
import uz.billsplitter2.demo.dto.response.BillResponseDto;
import uz.billsplitter2.demo.dto.response.ParticipantShareDto;
import uz.billsplitter2.demo.exception.ValidationException;
import uz.billsplitter2.demo.service.impl.BillCalculationServiceImpl;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class BillCalculationServiceImplTest {

    BillCalculationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BillCalculationServiceImpl();
        ReflectionTestUtils.setField(service, "serviceFeePercent", new BigDecimal("10"));
    }

    @Test
    void splitsByOrderItemsWithServiceFee() {
        BillRequestDto request = new BillRequestDto(
                List.of(
                        new OrderItemRequestDto("Pasta", new BigDecimal("25"), List.of("Alice")),
                        new OrderItemRequestDto("Steak", new BigDecimal("30"), List.of("Bob")),
                        new OrderItemRequestDto("Shared dessert", new BigDecimal("40"), List.of("Alice", "Bob", "Carol", "Dave"))
                )
        );

        BillResponseDto response = service.split(request);

        assertThat(response.itemsTotal()).isEqualByComparingTo("95.00");
        assertThat(response.serviceFeeApplied()).isEqualByComparingTo("9.50");
        assertThat(response.totalToPay()).isEqualByComparingTo("104.50");

        assertThat(response.shares())
                .extracting(ParticipantShareDto::name, ParticipantShareDto::itemsTotal, ParticipantShareDto::serviceFeeShare, ParticipantShareDto::total, ParticipantShareDto::percentage)
                .containsExactly(
                        tuple("Alice", new BigDecimal("35.00"), new BigDecimal("3.50"), new BigDecimal("38.50"), new BigDecimal("36.84")),
                        tuple("Bob", new BigDecimal("40.00"), new BigDecimal("4.00"), new BigDecimal("44.00"), new BigDecimal("42.11")),
                        tuple("Carol", new BigDecimal("10.00"), new BigDecimal("1.00"), new BigDecimal("11.00"), new BigDecimal("10.53")),
                        tuple("Dave", new BigDecimal("10.00"), new BigDecimal("1.00"), new BigDecimal("11.00"), new BigDecimal("10.53"))
                );
    }

    @Test
    void distributesRemaindersDeterministically() {
        ReflectionTestUtils.setField(service, "serviceFeePercent", BigDecimal.ZERO);

        BillRequestDto request = new BillRequestDto(
                List.of(
                        new OrderItemRequestDto("Pizza", new BigDecimal("100"), List.of("A", "B", "C"))
                )
        );

        BillResponseDto response = service.split(request);

        assertThat(response.itemsTotal()).isEqualByComparingTo("100.00");
        assertThat(response.serviceFeeApplied()).isEqualByComparingTo("0.00");
        assertThat(response.totalToPay()).isEqualByComparingTo("100.00");

        assertThat(response.shares())
                .extracting(ParticipantShareDto::name, ParticipantShareDto::total, ParticipantShareDto::percentage)
                .containsExactly(
                        tuple("A", new BigDecimal("33.34"), new BigDecimal("33.34")),
                        tuple("B", new BigDecimal("33.33"), new BigDecimal("33.33")),
                        tuple("C", new BigDecimal("33.33"), new BigDecimal("33.33"))
                );
    }

    @Test
    void validatesNegativeServiceFee() {
        ReflectionTestUtils.setField(service, "serviceFeePercent", new BigDecimal("-1"));

        BillRequestDto request = new BillRequestDto(
                List.of(new OrderItemRequestDto("Tea", new BigDecimal("10"), List.of("Solo")))
        );

        assertThatThrownBy(() -> service.split(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Service fee percent");
    }

    @Test
    void validatesEmptyParticipants() {
        BillRequestDto request = new BillRequestDto(
                List.of(new OrderItemRequestDto("Tea", new BigDecimal("10"), List.of()))
        );

        assertThatThrownBy(() -> service.split(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("order item");
    }

    @Test
    void distributesServiceFeeProportionallyToItemsTotal() {
        BillRequestDto request = new BillRequestDto(
                List.of(
                        new OrderItemRequestDto("Soup", new BigDecimal("20"), List.of("Ann")),
                        new OrderItemRequestDto("Juice", new BigDecimal("10"), List.of("Ben"))
                )
        );

        BillResponseDto response = service.split(request);

        assertThat(response.serviceFeeApplied()).isEqualByComparingTo("3.00");
        assertThat(response.shares())
                .extracting(ParticipantShareDto::name, ParticipantShareDto::serviceFeeShare)
                .containsExactly(
                        tuple("Ann", new BigDecimal("2.00")),
                        tuple("Ben", new BigDecimal("1.00"))
                );
    }

    @Test
    void throwsWhenNoItems() {
        BillRequestDto request = new BillRequestDto(List.of());

        assertThatThrownBy(() -> service.split(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("At least one order item");
    }

    @Test
    void distributesServiceFeeRemainderDeterministically() {
        BillRequestDto request = new BillRequestDto(
                List.of(
                        new OrderItemRequestDto("Shared dish", new BigDecimal("1.00"), List.of("A", "B", "C"))
                )
        );

        BillResponseDto response = service.split(request);

        assertThat(response.serviceFeeApplied()).isEqualByComparingTo("0.10");
        assertThat(response.shares())
                .extracting(ParticipantShareDto::name, ParticipantShareDto::total)
                .containsExactly(
                        tuple("A", new BigDecimal("0.38")),
                        tuple("B", new BigDecimal("0.36")),
                        tuple("C", new BigDecimal("0.36"))
                );
    }

    @Test
    void handlesZeroServiceFee() {
        ReflectionTestUtils.setField(service, "serviceFeePercent", BigDecimal.ZERO);

        BillRequestDto request = new BillRequestDto(
                List.of(new OrderItemRequestDto("Coffee", new BigDecimal("5.00"), List.of("Alice")))
        );

        BillResponseDto response = service.split(request);

        assertThat(response.itemsTotal()).isEqualByComparingTo("5.00");
        assertThat(response.serviceFeeApplied()).isEqualByComparingTo("0.00");
        assertThat(response.totalToPay()).isEqualByComparingTo("5.00");
    }
}
