package uz.billsplitter2.demo.service;

import uz.billsplitter2.demo.dto.response.AnalyticsDto;
import uz.billsplitter2.demo.dto.response.BillDto;
import uz.billsplitter2.demo.dto.response.BillHistoryDto;
import uz.billsplitter2.demo.enums.BillStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderHistoryService {

    List<BillHistoryDto> getClosedBills(OffsetDateTime startDate, OffsetDateTime endDate, UUID waiterId);

    BillDto getBillDetails(UUID billId);

    AnalyticsDto getAnalytics(OffsetDateTime startDate, OffsetDateTime endDate);

    BillDto getBillByNumber(String billNumber);

    List<BillDto> getAllBillsByStatus(BillStatus status);
}
