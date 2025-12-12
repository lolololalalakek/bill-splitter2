package uz.billsplitter2.demo.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uz.billsplitter2.demo.dto.response.OrderItemDto;
import uz.billsplitter2.demo.entity.OrderItem;

@Mapper(componentModel = "spring", uses = {GuestMapper.class})
public interface OrderItemMapper {

    @Mapping(source = "bill.id", target = "billId")
    OrderItemDto toDto(OrderItem entity);
}
