package uz.billsplitter2.demo.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uz.billsplitter2.demo.dto.response.PartyDto;
import uz.billsplitter2.demo.entity.Party;

@Mapper(componentModel = "spring", uses = {GuestMapper.class})
public interface PartyMapper {

    @Mapping(source = "table.id", target = "tableId")
    @Mapping(source = "table.tableNumber", target = "tableNumber")
    @Mapping(source = "waiter.id", target = "waiterId")
    @Mapping(source = "waiter.username", target = "waiterUsername")
    PartyDto toDto(Party entity);
}
