package uz.billsplitter2.demo.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.billsplitter2.demo.dto.request.CreatePartyDto;
import uz.billsplitter2.demo.dto.response.PartyDto;
import uz.billsplitter2.demo.entity.Party;
import uz.billsplitter2.demo.entity.RestaurantTable;
import uz.billsplitter2.demo.entity.Waiter;
import uz.billsplitter2.demo.enums.BillStatus;
import uz.billsplitter2.demo.enums.PartyStatus;
import uz.billsplitter2.demo.enums.TableStatus;
import uz.billsplitter2.demo.exception.BusinessLogicException;
import uz.billsplitter2.demo.exception.ResourceNotFoundException;
import uz.billsplitter2.demo.exception.UnauthorizedException;
import uz.billsplitter2.demo.mapper.PartyMapper;
import uz.billsplitter2.demo.repository.BillRepository;
import uz.billsplitter2.demo.repository.PartyRepository;
import uz.billsplitter2.demo.repository.RestaurantTableRepository;
import uz.billsplitter2.demo.security.SecurityContext;
import uz.billsplitter2.demo.service.PartyService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PartyServiceImpl implements PartyService {

    private final PartyRepository partyRepository;
    private final RestaurantTableRepository tableRepository;
    private final BillRepository billRepository;
    private final PartyMapper partyMapper;
    private final SecurityContext securityContext;

    // создание новой компании за столом
    @Override
    public PartyDto createParty(CreatePartyDto dto) {
        Waiter currentWaiter = securityContext.getCurrentWaiter();

        RestaurantTable table = tableRepository.findById(dto.tableId())
                .orElseThrow(() -> new ResourceNotFoundException("Table not found with id: " + dto.tableId()));

        if (table.getStatus() == TableStatus.OCCUPIED) {
            throw new BusinessLogicException("Table is already occupied");
        }

        if (partyRepository.findByTableIdAndStatus(dto.tableId(), PartyStatus.ACTIVE).isPresent()) {
            throw new BusinessLogicException("Table already has an active party");
        }

        Party party = Party.builder()
                .table(table)
                .waiter(currentWaiter)
                .status(PartyStatus.ACTIVE)
                .build();

        table.setStatus(TableStatus.OCCUPIED);

        Party saved = partyRepository.save(party);
        tableRepository.save(table);

        return partyMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PartyDto getPartyById(UUID id) {
        Party party = findPartyOrThrow(id);
        checkAccess(party);
        return partyMapper.toDto(party);
    }

    // получение активных компаний текущего официанта
    @Override
    @Transactional(readOnly = true)
    public List<PartyDto> getMyParties() {
        UUID currentWaiterId = securityContext.getCurrentWaiterId();
        return partyRepository.findAllByWaiterIdAndStatus(currentWaiterId, PartyStatus.ACTIVE).stream()
                .map(partyMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartyDto> getAllActiveParties() {
        if (!securityContext.isAdmin()) {
            throw new UnauthorizedException("Only admins can view all active parties");
        }
        return partyRepository.findAllByStatus(PartyStatus.ACTIVE).stream()
                .map(partyMapper::toDto)
                .toList();
    }

    // закрытие компании и освобождение стола
    @Override
    public PartyDto closeParty(UUID id) {
        Party party = findPartyOrThrow(id);
        checkAccess(party);

        if (party.getStatus() == PartyStatus.CLOSED) {
            throw new BusinessLogicException("Party is already closed");
        }

        boolean hasOpenBill = billRepository.findByPartyIdAndStatus(id, BillStatus.OPEN).isPresent();
        if (hasOpenBill) {
            throw new BusinessLogicException("Cannot close party with open bill");
        }

        party.setStatus(PartyStatus.CLOSED);
        party.setClosedAt(OffsetDateTime.now());

        RestaurantTable table = party.getTable();
        table.setStatus(TableStatus.AVAILABLE);

        partyRepository.save(party);
        tableRepository.save(table);

        return partyMapper.toDto(party);
    }

    // получение всех компаний конкретного официанта (для админа)
    @Override
    @Transactional(readOnly = true)
    public List<PartyDto> getAllPartiesByWaiter(UUID waiterId) {
        if (!securityContext.isAdmin()) {
            throw new UnauthorizedException("Only admins can view parties by waiter");
        }
        return partyRepository.findAllByWaiterId(waiterId).stream()
                .map(partyMapper::toDto)
                .toList();
    }

    private Party findPartyOrThrow(UUID id) {
        return partyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Party not found with id: " + id));
    }

    private void checkAccess(Party party) {
        if (!securityContext.isAdmin() && !party.getWaiter().getId().equals(securityContext.getCurrentWaiterId())) {
            throw new UnauthorizedException("You don't have access to this party");
        }
    }
}
