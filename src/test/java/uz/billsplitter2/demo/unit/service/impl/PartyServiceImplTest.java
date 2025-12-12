package uz.billsplitter2.demo.unit.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.billsplitter2.demo.dto.request.CreatePartyDto;
import uz.billsplitter2.demo.dto.response.PartyDto;
import uz.billsplitter2.demo.entity.Party;
import uz.billsplitter2.demo.entity.RestaurantTable;
import uz.billsplitter2.demo.entity.Waiter;
import uz.billsplitter2.demo.enums.BillStatus;
import uz.billsplitter2.demo.enums.PartyStatus;
import uz.billsplitter2.demo.enums.TableStatus;
import uz.billsplitter2.demo.enums.WaiterRole;
import uz.billsplitter2.demo.exception.BusinessLogicException;
import uz.billsplitter2.demo.exception.ResourceNotFoundException;
import uz.billsplitter2.demo.mapper.PartyMapper;
import uz.billsplitter2.demo.repository.BillRepository;
import uz.billsplitter2.demo.repository.PartyRepository;
import uz.billsplitter2.demo.repository.RestaurantTableRepository;
import uz.billsplitter2.demo.security.SecurityContext;
import uz.billsplitter2.demo.service.impl.PartyServiceImpl;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartyServiceImplTest {

    @Mock
    PartyRepository partyRepository;

    @Mock
    RestaurantTableRepository tableRepository;

    @Mock
    BillRepository billRepository;

    @Mock
    PartyMapper partyMapper;

    @Mock
    SecurityContext securityContext;

    @InjectMocks
    PartyServiceImpl partyService;

    Waiter waiter;
    RestaurantTable table;
    Party party;
    PartyDto partyDto;
    CreatePartyDto createDto;

    @BeforeEach
    void setUp() {
        UUID waiterId = UUID.randomUUID();
        UUID tableId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();

        waiter = Waiter.builder()
                .keycloakId("keycloak-123")
                .username("waiter1")
                .role(WaiterRole.WAITER)
                .active(true)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(waiter, "id", waiterId);

        table = RestaurantTable.builder()
                .tableNumber("T1")
                .capacity(4)
                .status(TableStatus.AVAILABLE)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(table, "id", tableId);

        party = Party.builder()
                .table(table)
                .waiter(waiter)
                .status(PartyStatus.ACTIVE)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(party, "id", partyId);

        createDto = new CreatePartyDto(tableId);

        partyDto = new PartyDto(
                partyId,
                tableId,
                "T1",
                waiterId,
                "waiter1",
                PartyStatus.ACTIVE,
                null,
                null,
                null
        );
    }

    @Test
    void createParty_Success() {
        when(securityContext.getCurrentWaiter()).thenReturn(waiter);
        when(tableRepository.findById(table.getId())).thenReturn(Optional.of(table));
        when(partyRepository.findByTableIdAndStatus(table.getId(), PartyStatus.ACTIVE)).thenReturn(Optional.empty());
        when(partyRepository.save(any(Party.class))).thenReturn(party);
        when(partyMapper.toDto(party)).thenReturn(partyDto);

        PartyDto result = partyService.createParty(createDto);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(PartyStatus.ACTIVE);
        verify(tableRepository).save(table);
        assertThat(table.getStatus()).isEqualTo(TableStatus.OCCUPIED);
    }

    @Test
    void createParty_TableNotFound_ThrowsResourceNotFoundException() {
        when(securityContext.getCurrentWaiter()).thenReturn(waiter);
        when(tableRepository.findById(table.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> partyService.createParty(createDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");

        verify(partyRepository, never()).save(any());
    }

    @Test
    void createParty_TableOccupied_ThrowsBusinessLogicException() {
        table.setStatus(TableStatus.OCCUPIED);
        when(securityContext.getCurrentWaiter()).thenReturn(waiter);
        when(tableRepository.findById(table.getId())).thenReturn(Optional.of(table));

        assertThatThrownBy(() -> partyService.createParty(createDto))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("occupied");

        verify(partyRepository, never()).save(any());
    }

    @Test
    void createParty_ActivePartyExists_ThrowsBusinessLogicException() {
        when(securityContext.getCurrentWaiter()).thenReturn(waiter);
        when(tableRepository.findById(table.getId())).thenReturn(Optional.of(table));
        when(partyRepository.findByTableIdAndStatus(table.getId(), PartyStatus.ACTIVE)).thenReturn(Optional.of(party));

        assertThatThrownBy(() -> partyService.createParty(createDto))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("already has");

        verify(partyRepository, never()).save(any());
    }

    @Test
    void closeParty_Success() {
        when(partyRepository.findById(party.getId())).thenReturn(Optional.of(party));
        when(securityContext.isAdmin()).thenReturn(false);
        when(securityContext.getCurrentWaiterId()).thenReturn(waiter.getId());
        when(billRepository.findByPartyIdAndStatus(party.getId(), BillStatus.OPEN)).thenReturn(Optional.empty());
        when(partyMapper.toDto(party)).thenReturn(partyDto);

        PartyDto result = partyService.closeParty(party.getId());

        assertThat(result).isNotNull();
        verify(partyRepository).save(party);
        verify(tableRepository).save(table);
        assertThat(party.getStatus()).isEqualTo(PartyStatus.CLOSED);
        assertThat(table.getStatus()).isEqualTo(TableStatus.AVAILABLE);
    }

    @Test
    void closeParty_WithOpenBill_ThrowsBusinessLogicException() {
        when(partyRepository.findById(party.getId())).thenReturn(Optional.of(party));
        when(securityContext.isAdmin()).thenReturn(false);
        when(securityContext.getCurrentWaiterId()).thenReturn(waiter.getId());
        when(billRepository.findByPartyIdAndStatus(party.getId(), BillStatus.OPEN))
                .thenReturn(Optional.of(org.mockito.Mockito.mock(uz.billsplitter2.demo.entity.Bill.class)));

        assertThatThrownBy(() -> partyService.closeParty(party.getId()))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("open bill");

        verify(partyRepository, never()).save(any());
    }

    @Test
    void closeParty_AlreadyClosed_ThrowsBusinessLogicException() {
        party.setStatus(PartyStatus.CLOSED);
        when(partyRepository.findById(party.getId())).thenReturn(Optional.of(party));
        when(securityContext.isAdmin()).thenReturn(false);
        when(securityContext.getCurrentWaiterId()).thenReturn(waiter.getId());

        assertThatThrownBy(() -> partyService.closeParty(party.getId()))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("already closed");

        verify(billRepository, never()).findByPartyIdAndStatus(any(), any());
    }
}
