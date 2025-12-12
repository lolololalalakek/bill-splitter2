package uz.billsplitter2.demo.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import uz.billsplitter2.demo.dto.request.CreateWaiterDto;
import uz.billsplitter2.demo.dto.request.UpdateWaiterDto;
import uz.billsplitter2.demo.dto.response.WaiterDto;
import uz.billsplitter2.demo.entity.Waiter;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface WaiterMapper {

    @Mapping(target = "keycloakId", ignore = true)
    @Mapping(target = "active", constant = "true")
    Waiter toEntity(CreateWaiterDto dto);

    WaiterDto toDto(Waiter entity);

    void updateEntity(UpdateWaiterDto dto, @MappingTarget Waiter entity);
}
