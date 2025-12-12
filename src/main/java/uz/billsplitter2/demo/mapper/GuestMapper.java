package uz.billsplitter2.demo.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uz.billsplitter2.demo.dto.request.AddGuestDto;
import uz.billsplitter2.demo.dto.response.GuestDto;
import uz.billsplitter2.demo.entity.Guest;

@Mapper(componentModel = "spring")
public interface GuestMapper {

    Guest toEntity(AddGuestDto dto);

    @Mapping(source = "party.id", target = "partyId")
    GuestDto toDto(Guest entity);
}
