package uz.billsplitter2.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.billsplitter2.demo.dto.response.AnalyticsDto;
import uz.billsplitter2.demo.dto.response.BillDto;
import uz.billsplitter2.demo.dto.response.BillHistoryDto;
import uz.billsplitter2.demo.enums.BillStatus;
import uz.billsplitter2.demo.service.OrderHistoryService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
public class OrderHistoryController {

    private final OrderHistoryService orderHistoryService;

    @GetMapping("/bills")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
    public ResponseEntity<List<BillHistoryDto>> getClosedBills(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @RequestParam(required = false) UUID waiterId
    ) {
        return ResponseEntity.ok(orderHistoryService.getClosedBills(startDate, endDate, waiterId));
    }

    @GetMapping("/bills/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
    public ResponseEntity<BillDto> getBillDetails(@PathVariable UUID id) {
        return ResponseEntity.ok(orderHistoryService.getBillDetails(id));
    }

    // поиск счета по номеру
    @GetMapping("/bills/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
    public ResponseEntity<BillDto> getBillByNumber(@RequestParam String billNumber) {
        return ResponseEntity.ok(orderHistoryService.getBillByNumber(billNumber));
    }

    // получение всех счетов по статусу (для админа)
    @GetMapping("/bills/by-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BillDto>> getBillsByStatus(@RequestParam BillStatus status) {
        return ResponseEntity.ok(orderHistoryService.getAllBillsByStatus(status));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AnalyticsDto> getAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate
    ) {
        return ResponseEntity.ok(orderHistoryService.getAnalytics(startDate, endDate));
    }
}
