package uz.billsplitter2.demo.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.billsplitter2.demo.dto.request.AddOrderItemDto;
import uz.billsplitter2.demo.dto.request.CreateBillDto;
import uz.billsplitter2.demo.dto.response.BillDto;
import uz.billsplitter2.demo.dto.response.BillResponseDto;
import uz.billsplitter2.demo.dto.response.OrderItemDto;
import uz.billsplitter2.demo.entity.Bill;
import uz.billsplitter2.demo.entity.Guest;
import uz.billsplitter2.demo.entity.OrderItem;
import uz.billsplitter2.demo.entity.Party;
import uz.billsplitter2.demo.enums.BillStatus;
import uz.billsplitter2.demo.enums.PartyStatus;
import uz.billsplitter2.demo.exception.BusinessLogicException;
import uz.billsplitter2.demo.exception.ResourceNotFoundException;
import uz.billsplitter2.demo.exception.UnauthorizedException;
import uz.billsplitter2.demo.mapper.BillMapper;
import uz.billsplitter2.demo.mapper.OrderItemMapper;
import uz.billsplitter2.demo.repository.BillRepository;
import uz.billsplitter2.demo.repository.GuestRepository;
import uz.billsplitter2.demo.repository.OrderItemRepository;
import uz.billsplitter2.demo.repository.PartyRepository;
import uz.billsplitter2.demo.security.SecurityContext;
import uz.billsplitter2.demo.service.BillCalculationService;
import uz.billsplitter2.demo.service.BillManagementService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BillManagementServiceImpl implements BillManagementService {

    private final BillRepository billRepository;
    private final PartyRepository partyRepository;
    private final OrderItemRepository orderItemRepository;
    private final GuestRepository guestRepository;
    private final BillMapper billMapper;
    private final OrderItemMapper orderItemMapper;
    private final BillCalculationService calculationService;
    private final SecurityContext securityContext;

    @Value("${app.bill.service-fee-percent}")
    BigDecimal serviceFeePercent;

    // создание нового счета для компании
    @Override
    public BillDto createBill(CreateBillDto dto) {
        Party party = partyRepository.findById(dto.partyId())
                .orElseThrow(() -> new ResourceNotFoundException("Party not found with id: " + dto.partyId()));

        checkAccess(party);

        if (party.getStatus() == PartyStatus.CLOSED) {
            throw new BusinessLogicException("Cannot create bill for closed party");
        }

        if (billRepository.findByPartyIdAndStatus(dto.partyId(), BillStatus.OPEN).isPresent()) {
            throw new BusinessLogicException("Party already has an open bill");
        }

        String billNumber = generateBillNumber();

        Bill bill = Bill.builder()
                .party(party)
                .billNumber(billNumber)
                .serviceFeePercent(serviceFeePercent)
                .itemsTotal(BigDecimal.ZERO)
                .serviceFeeAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .status(BillStatus.OPEN)
                .build();

        Bill saved = billRepository.save(bill);
        return billMapper.toDto(saved);
    }

    // получение счета по id с проверкой доступа
    @Override
    @Transactional(readOnly = true)
    public BillDto getBillById(UUID id) {
        Bill bill = findBillOrThrow(id);
        checkAccess(bill.getParty());
        return billMapper.toDto(bill);
    }

    // добавление позиции в счет с привязкой к гостям
    @Override
    public OrderItemDto addOrderItem(UUID billId, AddOrderItemDto dto) {
        Bill bill = findBillOrThrow(billId);
        checkAccess(bill.getParty());

        if (bill.getStatus() != BillStatus.OPEN) {
            throw new BusinessLogicException("Cannot add items to closed bill");
        }

        // проверка существования всех гостей
        List<Guest> guests = guestRepository.findAllById(dto.guestIds());
        if (guests.size() != dto.guestIds().size()) {
            throw new ResourceNotFoundException("Some guests not found");
        }

        // проверка что все гости принадлежат этой компании
        for (Guest guest : guests) {
            if (!guest.getParty().getId().equals(bill.getParty().getId())) {
                throw new BusinessLogicException("Guest does not belong to this party");
            }
        }

        OrderItem orderItem = OrderItem.builder()
                .bill(bill)
                .name(dto.name())
                .price(dto.price())
                .quantity(dto.quantity())
                .guests(guests)
                .build();

        OrderItem saved = orderItemRepository.save(orderItem);
        return orderItemMapper.toDto(saved);
    }

    // удаление позиции из счета
    @Override
    public void removeOrderItem(UUID billId, UUID itemId) {
        Bill bill = findBillOrThrow(billId);
        checkAccess(bill.getParty());

        if (bill.getStatus() != BillStatus.OPEN) {
            throw new BusinessLogicException("Cannot remove items from closed bill");
        }

        OrderItem orderItem = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found with id: " + itemId));

        if (!orderItem.getBill().getId().equals(billId)) {
            throw new BusinessLogicException("Order item does not belong to this bill");
        }

        orderItemRepository.delete(orderItem);
    }

    // расчет счета без закрытия (предпросмотр)
    @Override
    @Transactional(readOnly = true)
    public BillResponseDto calculateBill(UUID billId) {
        Bill bill = findBillOrThrow(billId);
        checkAccess(bill.getParty());

        if (bill.getStatus() != BillStatus.OPEN) {
            throw new BusinessLogicException("Can only calculate open bills");
        }

        if (bill.getOrderItems().isEmpty()) {
            throw new BusinessLogicException("Cannot calculate bill with no items");
        }

        return calculationService.split(bill.toBillRequestDto());
    }

    // закрытие счета с финальным расчетом
    @Override
    public BillDto closeBill(UUID billId) {
        Bill bill = findBillOrThrow(billId);
        checkAccess(bill.getParty());

        if (bill.getStatus() == BillStatus.CLOSED) {
            throw new BusinessLogicException("Bill is already closed");
        }

        if (bill.getOrderItems().isEmpty()) {
            throw new BusinessLogicException("Cannot close bill with no items");
        }

        BillResponseDto calculation = calculationService.split(bill.toBillRequestDto());
        bill.applyCalculationResult(calculation);
        bill.close();

        Bill saved = billRepository.save(bill);
        return billMapper.toDto(saved);
    }

    // получение всех позиций конкретного счета
    @Override
    @Transactional(readOnly = true)
    public List<OrderItemDto> getOrderItemsByBill(UUID billId) {
        Bill bill = findBillOrThrow(billId);
        checkAccess(bill.getParty());
        return orderItemRepository.findAllByBillId(billId).stream()
                .map(orderItemMapper::toDto)
                .toList();
    }

    // получение всех счетов конкретной компании
    @Override
    @Transactional(readOnly = true)
    public List<BillDto> getBillsByParty(UUID partyId) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ResourceNotFoundException("Party not found with id: " + partyId));
        checkAccess(party);
        return billRepository.findAllByPartyId(partyId).stream()
                .map(billMapper::toDto)
                .toList();
    }

    private Bill findBillOrThrow(UUID id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with id: " + id));
    }

    // проверка доступа к счету (только свой официант или админ)
    private void checkAccess(Party party) {
        if (!securityContext.isAdmin() && !party.getWaiter().getId().equals(securityContext.getCurrentWaiterId())) {
            throw new UnauthorizedException("You don't have access to this bill");
        }
    }

    // генерация уникального номера счета
    private String generateBillNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "BILL-" + timestamp + "-" + random;
    }
}
