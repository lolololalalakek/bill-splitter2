package uz.billsplitter2.demo.service;

import uz.billsplitter2.demo.dto.request.CreatePartyDto;
import uz.billsplitter2.demo.dto.response.PartyDto;

import java.util.List;
import java.util.UUID;

public interface PartyService {

    PartyDto createParty(CreatePartyDto dto);

    PartyDto getPartyById(UUID id);

    List<PartyDto> getMyParties();

    List<PartyDto> getAllActiveParties();

    PartyDto closeParty(UUID id);

    List<PartyDto> getAllPartiesByWaiter(UUID waiterId);
}
