package uz.billsplitter2.demo.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.billsplitter2.demo.dto.response.AnalyticsDto;
import uz.billsplitter2.demo.dto.response.BillDto;
import uz.billsplitter2.demo.dto.response.BillHistoryDto;
import uz.billsplitter2.demo.entity.Bill;
import uz.billsplitter2.demo.enums.BillStatus;
import uz.billsplitter2.demo.exception.ResourceNotFoundException;
import uz.billsplitter2.demo.exception.UnauthorizedException;
import uz.billsplitter2.demo.mapper.BillMapper;
import uz.billsplitter2.demo.repository.BillRepository;
import uz.billsplitter2.demo.security.SecurityContext;
import uz.billsplitter2.demo.service.OrderHistoryService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderHistoryServiceImpl implements OrderHistoryService {

    private final BillRepository billRepository;
    private final BillMapper billMapper;
    private final SecurityContext securityContext;

    // получение закрытых счетов с фильтрацией по датам и официанту
    @Override
    public List<BillHistoryDto> getClosedBills(OffsetDateTime startDate, OffsetDateTime endDate, UUID waiterId) {
        UUID effectiveWaiterId = waiterId;

        // обычный официант видит только свои счета
        if (!securityContext.isAdmin()) {
            effectiveWaiterId = securityContext.getCurrentWaiterId();
        }

        return billRepository.findClosedBillsWithFilters(BillStatus.CLOSED, startDate, endDate, effectiveWaiterId)
                .stream()
                .map(billMapper::toHistoryDto)
                .toList();
    }

    // получение детальной информации о счете
    @Override
    public BillDto getBillDetails(UUID billId) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with id: " + billId));

        if (!securityContext.isAdmin() && !bill.getParty().getWaiter().getId().equals(securityContext.getCurrentWaiterId())) {
            throw new UnauthorizedException("You don't have access to this bill");
        }

        return billMapper.toDto(bill);
    }

    // получение аналитики по закрытым счетам (только админ)
    @Override
    public AnalyticsDto getAnalytics(OffsetDateTime startDate, OffsetDateTime endDate) {
        if (!securityContext.isAdmin()) {
            throw new UnauthorizedException("Only admins can view analytics");
        }

        List<Bill> closedBills = billRepository.findClosedBillsWithFilters(BillStatus.CLOSED, startDate, endDate, null);

        long totalBills = closedBills.size();
        BigDecimal totalRevenue = closedBills.stream()
                .map(Bill::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalServiceFees = closedBills.stream()
                .map(Bill::getServiceFeeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageBillAmount = totalBills > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalBills), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new AnalyticsDto(
                totalBills,
                totalRevenue,
                totalServiceFees,
                averageBillAmount,
                startDate,
                endDate
        );
    }

    // поиск счета по номеру
    @Override
    public BillDto getBillByNumber(String billNumber) {
        Bill bill = billRepository.findByBillNumber(billNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with number: " + billNumber));

        if (!securityContext.isAdmin() && !bill.getParty().getWaiter().getId().equals(securityContext.getCurrentWaiterId())) {
            throw new UnauthorizedException("You don't have access to this bill");
        }

        return billMapper.toDto(bill);
    }

    // получение всех счетов по статусу (для админа)
    @Override
    public List<BillDto> getAllBillsByStatus(BillStatus status) {
        if (!securityContext.isAdmin()) {
            throw new UnauthorizedException("Only admins can view all bills by status");
        }

        return billRepository.findAllByStatus(status).stream()
                .map(billMapper::toDto)
                .toList();
    }
}
