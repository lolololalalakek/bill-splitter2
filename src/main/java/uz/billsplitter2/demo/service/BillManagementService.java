package uz.billsplitter2.demo.service;

import uz.billsplitter2.demo.dto.request.AddOrderItemDto;
import uz.billsplitter2.demo.dto.request.CreateBillDto;
import uz.billsplitter2.demo.dto.response.BillDto;
import uz.billsplitter2.demo.dto.response.BillResponseDto;
import uz.billsplitter2.demo.dto.response.OrderItemDto;

import java.util.List;
import java.util.UUID;

public interface BillManagementService {

    BillDto createBill(CreateBillDto dto);

    BillDto getBillById(UUID id);

    OrderItemDto addOrderItem(UUID billId, AddOrderItemDto dto);

    void removeOrderItem(UUID billId, UUID itemId);

    BillResponseDto calculateBill(UUID billId);

    BillDto closeBill(UUID billId);

    List<OrderItemDto> getOrderItemsByBill(UUID billId);

    List<BillDto> getBillsByParty(UUID partyId);
}
