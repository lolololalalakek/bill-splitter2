package uz.billsplitter2.demo.unit.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import uz.billsplitter2.demo.service.impl.TableServiceImpl;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TableServiceImplTest {

    @Mock
    RestaurantTableRepository tableRepository;

    @Mock
    TableMapper tableMapper;

    @InjectMocks
    TableServiceImpl tableService;

    RestaurantTable table;
    CreateTableDto createDto;
    TableDto tableDto;

    @BeforeEach
    void setUp() {
        UUID tableId = UUID.randomUUID();
        table = RestaurantTable.builder()
                .tableNumber("T1")
                .capacity(4)
                .status(TableStatus.AVAILABLE)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(table, "id", tableId);

        createDto = new CreateTableDto("T1", 4);

        tableDto = new TableDto(
                tableId,
                "T1",
                4,
                TableStatus.AVAILABLE,
                null,
                null
        );
    }

    @Test
    void createTable_Success() {
        when(tableRepository.findByTableNumber("T1")).thenReturn(Optional.empty());
        when(tableMapper.toEntity(createDto)).thenReturn(table);
        when(tableRepository.save(any(RestaurantTable.class))).thenReturn(table);
        when(tableMapper.toDto(table)).thenReturn(tableDto);

        TableDto result = tableService.createTable(createDto);

        assertThat(result).isNotNull();
        assertThat(result.tableNumber()).isEqualTo("T1");
        verify(tableRepository).save(any(RestaurantTable.class));
    }

    @Test
    void createTable_DuplicateTableNumber_ThrowsValidationException() {
        when(tableRepository.findByTableNumber("T1")).thenReturn(Optional.of(table));

        assertThatThrownBy(() -> tableService.createTable(createDto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already exists");

        verify(tableRepository, never()).save(any());
    }

    @Test
    void getTableById_Success() {
        when(tableRepository.findById(table.getId())).thenReturn(Optional.of(table));
        when(tableMapper.toDto(table)).thenReturn(tableDto);

        TableDto result = tableService.getTableById(table.getId());

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(table.getId());
    }

    @Test
    void getTableById_NotFound_ThrowsResourceNotFoundException() {
        UUID nonExistentId = UUID.randomUUID();
        when(tableRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tableService.getTableById(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void updateTable_Success() {
        UpdateTableDto updateDto = new UpdateTableDto(6, TableStatus.RESERVED);
        when(tableRepository.findById(table.getId())).thenReturn(Optional.of(table));
        when(tableRepository.save(table)).thenReturn(table);
        when(tableMapper.toDto(table)).thenReturn(tableDto);

        TableDto result = tableService.updateTable(table.getId(), updateDto);

        assertThat(result).isNotNull();
        verify(tableMapper).updateEntity(updateDto, table);
        verify(tableRepository).save(table);
    }

    @Test
    void deleteTable_Success() {
        when(tableRepository.findById(table.getId())).thenReturn(Optional.of(table));

        tableService.deleteTable(table.getId());

        verify(tableRepository).delete(table);
    }

    @Test
    void deleteTable_OccupiedTable_ThrowsBusinessLogicException() {
        table.setStatus(TableStatus.OCCUPIED);
        when(tableRepository.findById(table.getId())).thenReturn(Optional.of(table));

        assertThatThrownBy(() -> tableService.deleteTable(table.getId()))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("occupied");

        verify(tableRepository, never()).delete(any());
    }
}
