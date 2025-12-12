package uz.billsplitter2.demo.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import uz.billsplitter2.demo.dto.request.CreateTableDto;
import uz.billsplitter2.demo.dto.request.UpdateTableDto;
import uz.billsplitter2.demo.dto.response.TableDto;
import uz.billsplitter2.demo.entity.RestaurantTable;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TableMapper {

    RestaurantTable toEntity(CreateTableDto dto);

    TableDto toDto(RestaurantTable entity);

    void updateEntity(UpdateTableDto dto, @MappingTarget RestaurantTable entity);
}
