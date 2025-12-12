package uz.billsplitter2.demo.service;

import uz.billsplitter2.demo.dto.request.CreateTableDto;
import uz.billsplitter2.demo.dto.request.UpdateTableDto;
import uz.billsplitter2.demo.dto.response.TableDto;
import uz.billsplitter2.demo.enums.TableStatus;

import java.util.List;
import java.util.UUID;

public interface TableService {

    TableDto createTable(CreateTableDto dto);

    List<TableDto> getAllTables();

    TableDto getTableById(UUID id);

    TableDto updateTable(UUID id, UpdateTableDto dto);

    void deleteTable(UUID id);

    List<TableDto> getTablesByStatus(TableStatus status);
}
