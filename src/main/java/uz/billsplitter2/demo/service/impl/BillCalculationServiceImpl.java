package uz.billsplitter2.demo.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uz.billsplitter2.demo.dto.request.BillRequestDto;
import uz.billsplitter2.demo.dto.request.OrderItemRequestDto;
import uz.billsplitter2.demo.dto.response.BillResponseDto;
import uz.billsplitter2.demo.dto.response.ParticipantShareDto;
import uz.billsplitter2.demo.exception.ValidationException;
import uz.billsplitter2.demo.service.BillCalculationService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BillCalculationServiceImpl implements BillCalculationService {

    // количество знаков после запятой для денег (копейки)
    private static final int MONEY_SCALE = 2;

    // дополнительная точность для промежуточных расчетов
    private static final int EXTRA_SCALE = 4;

    // константа 100 для расчета процентов
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    // одна копейка для распределения остатка
    private static final BigDecimal ONE_CENT = new BigDecimal("0.01");

    @Value("${app.bill.service-fee-percent}")
    BigDecimal serviceFeePercent;

    // основной метод расчета счета с разделением между участниками
    @Override
    public BillResponseDto split(BillRequestDto dto) {

        BigDecimal normalizedServiceFee = defaultIfNull(serviceFeePercent, BigDecimal.ZERO);

        if (normalizedServiceFee.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Service fee percent cannot be negative");
        }

        ensureValid(dto);

        BigDecimal itemsTotal = dto.items().stream()
                .map(OrderItemRequestDto::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        LinkedHashMap<String, ParticipantPortion> portions = allocateItemTotals(dto.items());

        BigDecimal serviceFeeApplied = percentageOf(itemsTotal, normalizedServiceFee)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        if (serviceFeeApplied.signum() != 0) {
            distributeServiceFee(portions, itemsTotal, serviceFeeApplied);
        }

        BigDecimal totalToPay = itemsTotal.add(serviceFeeApplied).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        List<ParticipantShareDto> shares = portions.values().stream()
                .map(portion -> portion.toDto(totalToPay))
                .toList();

        return new BillResponseDto(itemsTotal, serviceFeeApplied, totalToPay, shares);
    }

    // проверка валидности данных запроса
    private void ensureValid(BillRequestDto dto) {
        if (dto.items().isEmpty()) {
            throw new ValidationException("At least one order item is required");
        }

        boolean hasInvalidItem = dto.items().stream().anyMatch(item ->
                item.price() == null
                        || item.price().compareTo(BigDecimal.ZERO) <= 0
                        || item.participants() == null
                        || item.participants().isEmpty()
        );
        if (hasInvalidItem) {
            throw new ValidationException("Each order item must have price and participants");
        }

        boolean hasBlankNames = dto.items().stream()
                .flatMap(item -> item.participants().stream())
                .anyMatch(name -> name == null || name.isBlank());
        if (hasBlankNames) {
            throw new ValidationException("Participant name cannot be blank");
        }

        boolean hasEmptyItemName = dto.items().stream()
                .map(OrderItemRequestDto::name)
                .anyMatch(name -> name == null || name.isBlank());
        if (hasEmptyItemName) {
            throw new ValidationException("Item name cannot be blank");
        }
    }

    // распределение сумм позиций между участниками
    private LinkedHashMap<String, ParticipantPortion> allocateItemTotals(List<OrderItemRequestDto> items) {
        LinkedHashMap<String, ParticipantPortion> totals = new LinkedHashMap<>();

        for (OrderItemRequestDto item : items) {
            List<ItemShare> shares = splitAmount(item.price(), item.participants());
            for (ItemShare share : shares) {
                ParticipantPortion portion = totals.computeIfAbsent(
                        share.name(),
                        ParticipantPortion::new
                );
                portion.addItemsShare(share.amount());
            }
        }

        if (totals.isEmpty()) {
            throw new ValidationException("No participants found for items");
        }

        return totals;
    }

    // разделение суммы позиции на равные части с распределением остатка
    private List<ItemShare> splitAmount(BigDecimal amount, List<String> participants) {
        BigDecimal normalizedAmount = amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        int count = participants.size();
        BigDecimal rawShare = normalizedAmount.divide(BigDecimal.valueOf(count), MONEY_SCALE + EXTRA_SCALE, RoundingMode.HALF_UP);

        List<ItemShare> shares = new ArrayList<>(count);
        BigDecimal roundedSum = BigDecimal.ZERO;

        for (int i = 0; i < count; i++) {
            BigDecimal rounded = rawShare.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            BigDecimal fractionalPart = rawShare.remainder(BigDecimal.ONE);
            shares.add(new ItemShare(i, participants.get(i), rounded, fractionalPart));
            roundedSum = roundedSum.add(rounded);
        }

        BigDecimal remainder = normalizedAmount.subtract(roundedSum).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (remainder.signum() != 0) {
            distributeRemainder(shares, remainder);
        }

        return shares;
    }

    // распределение сервисного сбора пропорционально сумме позиций
    private void distributeServiceFee(Map<String, ParticipantPortion> portions, BigDecimal itemsTotal, BigDecimal serviceFee) {
        List<FeeShare> shares = new ArrayList<>(portions.size());
        BigDecimal roundedSum = BigDecimal.ZERO;
        int order = 0;

        for (ParticipantPortion portion : portions.values()) {
            BigDecimal rawShare = serviceFee.multiply(portion.itemsTotal()).divide(itemsTotal, MONEY_SCALE + EXTRA_SCALE, RoundingMode.HALF_UP);
            BigDecimal rounded = rawShare.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            BigDecimal fractional = rawShare.remainder(BigDecimal.ONE);
            shares.add(new FeeShare(order++, portion, rounded, fractional));
            roundedSum = roundedSum.add(rounded);
        }

        BigDecimal remainder = serviceFee.subtract(roundedSum).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (remainder.signum() != 0 && !shares.isEmpty()) {
            distributeRemainder(shares, remainder);
        }

        shares.forEach(FeeShare::apply);
    }

    private BigDecimal percentageOf(BigDecimal base, BigDecimal percent) {
        if (percent.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return base.multiply(percent).divide(ONE_HUNDRED, MONEY_SCALE + EXTRA_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultIfNull(BigDecimal value, BigDecimal fallback) {
        return Objects.requireNonNullElse(value, fallback);
    }

    private void distributeRemainder(List<? extends AdjustableShare> shares, BigDecimal remainder) {
        int centsToDistribute = remainder.movePointRight(MONEY_SCALE).intValue();
        Comparator<AdjustableShare> comparator = Comparator.comparing(AdjustableShare::fractionalPart).reversed()
                .thenComparingInt(AdjustableShare::order);
        shares.sort(comparator);

        BigDecimal step = ONE_CENT.multiply(BigDecimal.valueOf(remainder.signum()));
        int iterations = Math.abs(centsToDistribute);
        for (int i = 0; i < iterations; i++) {
            shares.get(i % shares.size()).bump(step);
        }
    }

    private interface AdjustableShare {
        BigDecimal fractionalPart();

        int order();

        void bump(BigDecimal delta);
    }

    private static final class ItemShare implements AdjustableShare {
        private final int order;
        private final String name;
        private BigDecimal amount;
        private final BigDecimal fractionalPart;

        private ItemShare(int order, String name, BigDecimal amount, BigDecimal fractionalPart) {
            this.order = order;
            this.name = name;
            this.amount = amount;
            this.fractionalPart = fractionalPart;
        }

        String name() {
            return name;
        }

        BigDecimal amount() {
            return amount;
        }

        @Override
        public BigDecimal fractionalPart() {
            return fractionalPart;
        }

        @Override
        public int order() {
            return order;
        }

        @Override
        public void bump(BigDecimal delta) {
            amount = amount.add(delta);
        }
    }

    private static final class FeeShare implements AdjustableShare {
        private final int order;
        private final ParticipantPortion portion;
        private BigDecimal amount;
        private final BigDecimal fractionalPart;

        private FeeShare(int order, ParticipantPortion portion, BigDecimal amount, BigDecimal fractionalPart) {
            this.order = order;
            this.portion = portion;
            this.amount = amount;
            this.fractionalPart = fractionalPart;
        }

        @Override
        public BigDecimal fractionalPart() {
            return fractionalPart;
        }

        @Override
        public int order() {
            return order;
        }

        @Override
        public void bump(BigDecimal delta) {
            amount = amount.add(delta);
        }

        void apply() {
            portion.addServiceFee(amount);
        }
    }

    private static final class ParticipantPortion {
        private final String name;
        private BigDecimal itemsTotal = BigDecimal.ZERO;
        private BigDecimal serviceFee = BigDecimal.ZERO;

        private ParticipantPortion(String name) {
            this.name = name;
        }

        void addItemsShare(BigDecimal value) {
            itemsTotal = itemsTotal.add(value);
        }

        void addServiceFee(BigDecimal value) {
            serviceFee = serviceFee.add(value);
        }

        BigDecimal itemsTotal() {
            return itemsTotal;
        }

        ParticipantShareDto toDto(BigDecimal totalToPay) {
            BigDecimal total = itemsTotal.add(serviceFee).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            BigDecimal percentage = totalToPay.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : total.multiply(ONE_HUNDRED).divide(totalToPay, MONEY_SCALE, RoundingMode.HALF_UP);
            return new ParticipantShareDto(
                    name,
                    itemsTotal.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                    serviceFee.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                    total,
                    percentage
            );
        }
    }
}
