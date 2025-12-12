package uz.billsplitter2.demo.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.billsplitter2.demo.dto.request.BillRequestDto;
import uz.billsplitter2.demo.dto.request.OrderItemRequestDto;
import uz.billsplitter2.demo.dto.response.BillResponseDto;
import uz.billsplitter2.demo.enums.BillStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bill extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @Column(name = "bill_number", unique = true, nullable = false, length = 50)
    private String billNumber;

    @Column(name = "items_total", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal itemsTotal = BigDecimal.ZERO;

    @Column(name = "service_fee_percent", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal serviceFeePercent = BigDecimal.ZERO;

    @Column(name = "service_fee_amount", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal serviceFeeAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BillStatus status = BillStatus.OPEN;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    public void addOrderItem(OrderItem item) {
        orderItems.add(item);
        item.setBill(this);
    }

    public void removeOrderItem(OrderItem item) {
        orderItems.remove(item);
        item.setBill(null);
    }

    public BillRequestDto toBillRequestDto() {
        List<OrderItemRequestDto> items = orderItems.stream()
            .map(item -> new OrderItemRequestDto(
                item.getName(),
                item.getPrice(),
                item.getGuests().stream()
                    .map(Guest::getName)
                    .toList()
            ))
            .toList();
        return new BillRequestDto(items);
    }

    public void applyCalculationResult(BillResponseDto response) {
        this.itemsTotal = response.itemsTotal();
        this.serviceFeeAmount = response.serviceFeeApplied();
        this.totalAmount = response.totalToPay();
    }

    public void close() {
        this.status = BillStatus.CLOSED;
        this.closedAt = OffsetDateTime.now();
    }
}
