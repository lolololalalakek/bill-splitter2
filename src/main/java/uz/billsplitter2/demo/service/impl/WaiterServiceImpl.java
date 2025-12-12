package uz.billsplitter2.demo.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.billsplitter2.demo.dto.request.CreateWaiterDto;
import uz.billsplitter2.demo.dto.request.UpdateWaiterDto;
import uz.billsplitter2.demo.dto.response.WaiterDto;
import uz.billsplitter2.demo.entity.Waiter;
import uz.billsplitter2.demo.exception.ResourceNotFoundException;
import uz.billsplitter2.demo.exception.ValidationException;
import uz.billsplitter2.demo.mapper.WaiterMapper;
import uz.billsplitter2.demo.repository.WaiterRepository;
import uz.billsplitter2.demo.service.WaiterService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class WaiterServiceImpl implements WaiterService {

    private final WaiterRepository waiterRepository;
    private final WaiterMapper waiterMapper;

    // создание нового официанта
    @Override
    public WaiterDto createWaiter(CreateWaiterDto dto) {
        if (waiterRepository.findByUsername(dto.username()).isPresent()) {
            throw new ValidationException("Username already exists: " + dto.username());
        }

        Waiter waiter = waiterMapper.toEntity(dto);
        waiter.setKeycloakId("KEYCLOAK_ID_PLACEHOLDER");

        Waiter saved = waiterRepository.save(waiter);
        return waiterMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WaiterDto> getAllWaiters() {
        return waiterRepository.findAll().stream()
                .map(waiterMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WaiterDto getWaiterById(UUID id) {
        Waiter waiter = findWaiterOrThrow(id);
        return waiterMapper.toDto(waiter);
    }

    // обновление данных официанта
    @Override
    public WaiterDto updateWaiter(UUID id, UpdateWaiterDto dto) {
        Waiter waiter = findWaiterOrThrow(id);
        waiterMapper.updateEntity(dto, waiter);
        Waiter updated = waiterRepository.save(waiter);
        return waiterMapper.toDto(updated);
    }

    // деактивация официанта
    @Override
    public void deactivateWaiter(UUID id) {
        Waiter waiter = findWaiterOrThrow(id);
        waiter.setActive(false);
        waiterRepository.save(waiter);
    }

    // получение списка активных официантов
    @Override
    @Transactional(readOnly = true)
    public List<WaiterDto> getActiveWaiters() {
        return waiterRepository.findAllByActive(true).stream()
                .map(waiterMapper::toDto)
                .toList();
    }

    private Waiter findWaiterOrThrow(UUID id) {
        return waiterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Waiter not found with id: " + id));
    }
}
