package uz.billsplitter2.demo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.billsplitter2.demo.dto.request.CreateTableDto;
import uz.billsplitter2.demo.dto.request.UpdateTableDto;
import uz.billsplitter2.demo.dto.response.TableDto;
import uz.billsplitter2.demo.enums.TableStatus;
import uz.billsplitter2.demo.service.TableService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tables")
@RequiredArgsConstructor
public class TableController {

    private final TableService tableService;

    // создание нового стола (только админ)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TableDto> createTable(@Valid @RequestBody CreateTableDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tableService.createTable(dto));
    }

    // получение списка столов с фильтрацией по статусу
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
    public ResponseEntity<List<TableDto>> getAllTables(
            @RequestParam(required = false) TableStatus status
    ) {
        if (status != null) {
            return ResponseEntity.ok(tableService.getTablesByStatus(status));
        }
        return ResponseEntity.ok(tableService.getAllTables());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
    public ResponseEntity<TableDto> getTableById(@PathVariable UUID id) {
        return ResponseEntity.ok(tableService.getTableById(id));
    }

    // обновление данных стола (только админ)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TableDto> updateTable(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTableDto dto
    ) {
        return ResponseEntity.ok(tableService.updateTable(id, dto));
    }

    // удаление стола (только админ)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTable(@PathVariable UUID id) {
        tableService.deleteTable(id);
        return ResponseEntity.noContent().build();
    }
}
