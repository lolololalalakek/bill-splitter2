package uz.billsplitter2.demo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.billsplitter2.demo.dto.request.AddOrderItemDto;
import uz.billsplitter2.demo.dto.request.BillRequestDto;
import uz.billsplitter2.demo.dto.request.CreateBillDto;
import uz.billsplitter2.demo.dto.response.BillDto;
import uz.billsplitter2.demo.dto.response.BillResponseDto;
import uz.billsplitter2.demo.dto.response.OrderItemDto;
import uz.billsplitter2.demo.service.BillCalculationService;
import uz.billsplitter2.demo.service.BillManagementService;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/bills")
public class BillController {

    private final BillCalculationService billCalculationService;
    private final BillManagementService billManagementService;

    // stateless расчет счета без сохранения в бд
    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
    public ResponseEntity<BillResponseDto> calculate(@Valid @RequestBody BillRequestDto requestDto) {
        return ResponseEntity.ok(billCalculationService.split(requestDto));
    }

    // создание счета для компании
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
    public ResponseEntity<BillDto> createBill(@Valid @RequestBody CreateBillDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(billManagementService.createBill(dto));
    }

    // получение счета или списка счетов компании
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
    public ResponseEntity<List<BillDto>> getBillsByParty(@RequestParam UUID partyId) {
        return ResponseEntity.ok(billManagementService.getBillsByParty(partyId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
    public ResponseEntity<BillDto> getBill(@PathVariable UUID id) {
        return ResponseEntity.ok(billManagementService.getBillById(id));
    }

    // получение позиций счета
    @GetMapping("/{id}/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
    public ResponseEntity<List<OrderItemDto>> getOrderItems(@PathVariable UUID id) {
        return ResponseEntity.ok(billManagementService.getOrderItemsByBill(id));
    }

    // добавление позиции в счет
    @PostMapping("/{id}/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
    public ResponseEntity<OrderItemDto> addItem(
            @PathVariable UUID id,
            @Valid @RequestBody AddOrderItemDto dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(billManagementService.addOrderItem(id, dto));
    }

    // удаление позиции из счета
    @DeleteMapping("/{id}/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
    public ResponseEntity<Void> removeItem(@PathVariable UUID id, @PathVariable UUID itemId) {
        billManagementService.removeOrderItem(id, itemId);
        return ResponseEntity.noContent().build();
    }

    // расчет итоговых сумм для сохраненного счета
    @PostMapping("/{id}/calculate")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
    public ResponseEntity<BillResponseDto> calculateBill(@PathVariable UUID id) {
        return ResponseEntity.ok(billManagementService.calculateBill(id));
    }

    // закрытие счета
    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
    public ResponseEntity<BillDto> closeBill(@PathVariable UUID id) {
        return ResponseEntity.ok(billManagementService.closeBill(id));
    }
}
