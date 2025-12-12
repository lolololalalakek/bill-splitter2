package uz.billsplitter2.demo.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.billsplitter2.demo.dto.request.AddGuestDto;
import uz.billsplitter2.demo.dto.response.GuestDto;
import uz.billsplitter2.demo.entity.Guest;
import uz.billsplitter2.demo.entity.Party;
import uz.billsplitter2.demo.enums.PartyStatus;
import uz.billsplitter2.demo.exception.BusinessLogicException;
import uz.billsplitter2.demo.exception.ResourceNotFoundException;
import uz.billsplitter2.demo.exception.UnauthorizedException;
import uz.billsplitter2.demo.mapper.GuestMapper;
import uz.billsplitter2.demo.repository.GuestRepository;
import uz.billsplitter2.demo.repository.PartyRepository;
import uz.billsplitter2.demo.security.SecurityContext;
import uz.billsplitter2.demo.service.GuestService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class GuestServiceImpl implements GuestService {

    private final GuestRepository guestRepository;
    private final PartyRepository partyRepository;
    private final GuestMapper guestMapper;
    private final SecurityContext securityContext;

    // добавление гостя к компании
    @Override
    public GuestDto addGuest(UUID partyId, AddGuestDto dto) {
        Party party = findPartyOrThrow(partyId);
        checkAccess(party);

        if (party.getStatus() == PartyStatus.CLOSED) {
            throw new BusinessLogicException("Cannot add guest to closed party");
        }

        // проверка уникальности имени в рамках компании
        if (guestRepository.findByPartyIdAndName(partyId, dto.name()).isPresent()) {
            throw new BusinessLogicException("Guest with name '" + dto.name() + "' already exists in this party");
        }

        Guest guest = guestMapper.toEntity(dto);
        guest.setParty(party);

        Guest saved = guestRepository.save(guest);
        return guestMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuestDto> getGuestsByParty(UUID partyId) {
        Party party = findPartyOrThrow(partyId);
        checkAccess(party);

        return guestRepository.findAllByPartyId(partyId).stream()
                .map(guestMapper::toDto)
                .toList();
    }

    @Override
    public void removeGuest(UUID partyId, UUID guestId) {
        Party party = findPartyOrThrow(partyId);
        checkAccess(party);

        if (party.getStatus() == PartyStatus.CLOSED) {
            throw new BusinessLogicException("Cannot remove guest from closed party");
        }

        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found with id: " + guestId));

        if (!guest.getParty().getId().equals(partyId)) {
            throw new BusinessLogicException("Guest does not belong to this party");
        }

        guestRepository.delete(guest);
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
