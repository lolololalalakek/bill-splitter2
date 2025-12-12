package uz.billsplitter2.demo.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.billsplitter2.demo.dto.request.CreateTableDto;
import uz.billsplitter2.demo.dto.request.UpdateTableDto;
import uz.billsplitter2.demo.dto.response.TableDto;
import uz.billsplitter2.demo.entity.RestaurantTable;
import uz.billsplitter2.demo.enums.TableStatus;
import uz.billsplitter2.demo.exception.BusinessLogicException;
import uz.billsplitter2.demo.exception.ResourceNotFoundException;
import uz.billsplitter2.demo.exception.ValidationException;
import uz.billsplitter2.demo.mapper.TableMapper;
import uz.billsplitter2.demo.repository.RestaurantTableRepository;
import uz.billsplitter2.demo.service.TableService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TableServiceImpl implements TableService {

    private final RestaurantTableRepository tableRepository;
    private final TableMapper tableMapper;

    // создание нового стола
    @Override
    public TableDto createTable(CreateTableDto dto) {
        if (tableRepository.findByTableNumber(dto.tableNumber()).isPresent()) {
            throw new ValidationException("Table with number " + dto.tableNumber() + " already exists");
        }

        RestaurantTable table = tableMapper.toEntity(dto);
        table.setStatus(TableStatus.AVAILABLE);

        RestaurantTable saved = tableRepository.save(table);
        return tableMapper.toDto(saved);
    }

    // получение списка всех столов
    @Override
    @Transactional(readOnly = true)
    public List<TableDto> getAllTables() {
        return tableRepository.findAll().stream()
                .map(tableMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TableDto getTableById(UUID id) {
        RestaurantTable table = findTableOrThrow(id);
        return tableMapper.toDto(table);
    }

    // обновление данных стола
    @Override
    public TableDto updateTable(UUID id, UpdateTableDto dto) {
        RestaurantTable table = findTableOrThrow(id);
        tableMapper.updateEntity(dto, table);
        RestaurantTable updated = tableRepository.save(table);
        return tableMapper.toDto(updated);
    }

    // удаление стола
    @Override
    public void deleteTable(UUID id) {
        RestaurantTable table = findTableOrThrow(id);

        if (table.getStatus() == TableStatus.OCCUPIED) {
            throw new BusinessLogicException("Cannot delete occupied table");
        }

        tableRepository.delete(table);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableDto> getTablesByStatus(TableStatus status) {
        return tableRepository.findAllByStatus(status).stream()
                .map(tableMapper::toDto)
                .toList();
    }

    private RestaurantTable findTableOrThrow(UUID id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found with id: " + id));
    }
}
