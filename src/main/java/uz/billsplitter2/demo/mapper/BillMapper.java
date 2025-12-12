package uz.billsplitter2.demo.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uz.billsplitter2.demo.dto.response.BillDto;
import uz.billsplitter2.demo.dto.response.BillHistoryDto;
import uz.billsplitter2.demo.entity.Bill;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class})
public interface BillMapper {

    @Mapping(source = "party.id", target = "partyId")
    BillDto toDto(Bill entity);

    @Mapping(source = "party.id", target = "partyId")
    @Mapping(source = "party.table.id", target = "tableId")
    @Mapping(source = "party.table.tableNumber", target = "tableNumber")
    @Mapping(source = "party.waiter.id", target = "waiterId")
    @Mapping(source = "party.waiter.username", target = "waiterUsername")
    @Mapping(source = "party.waiter.fullName", target = "waiterFullName")
    BillHistoryDto toHistoryDto(Bill entity);
}
