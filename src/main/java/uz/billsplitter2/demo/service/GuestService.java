package uz.billsplitter2.demo.service;

import uz.billsplitter2.demo.dto.request.AddGuestDto;
import uz.billsplitter2.demo.dto.response.GuestDto;

import java.util.List;
import java.util.UUID;

public interface GuestService {

    GuestDto addGuest(UUID partyId, AddGuestDto dto);

    List<GuestDto> getGuestsByParty(UUID partyId);

    void removeGuest(UUID partyId, UUID guestId);
}
