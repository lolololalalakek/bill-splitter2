package uz.billsplitter2.demo.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.billsplitter2.demo.dto.keycloak.KeycloakCredentialRepresentation;
import uz.billsplitter2.demo.dto.keycloak.KeycloakUserRepresentation;
import uz.billsplitter2.demo.dto.request.CreateWaiterDto;
import uz.billsplitter2.demo.dto.request.UpdateWaiterDto;
import uz.billsplitter2.demo.dto.response.WaiterDto;
import uz.billsplitter2.demo.entity.Waiter;
import uz.billsplitter2.demo.enums.WaiterRole;
import uz.billsplitter2.demo.exception.KeycloakException;
import uz.billsplitter2.demo.exception.ResourceNotFoundException;
import uz.billsplitter2.demo.exception.ValidationException;
import uz.billsplitter2.demo.mapper.WaiterMapper;
import uz.billsplitter2.demo.repository.WaiterRepository;
import uz.billsplitter2.demo.service.KeycloakAdminService;
import uz.billsplitter2.demo.service.WaiterService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class WaiterServiceImpl implements WaiterService {

    private final WaiterRepository waiterRepository;
    private final WaiterMapper waiterMapper;
    private final KeycloakAdminService keycloakAdminService;

    // создание нового официанта
    @Override
    public WaiterDto createWaiter(CreateWaiterDto dto) {
        log.info("Creating waiter with username: {}", dto.username());

        if (waiterRepository.findByUsername(dto.username()).isPresent()) {
            throw new ValidationException("Username already exists: " + dto.username());
        }

        // Create user in Keycloak first
        String keycloakId;
        try {
            KeycloakUserRepresentation keycloakUser = buildKeycloakUser(dto);
            keycloakId = keycloakAdminService.createUser(keycloakUser);
            log.info("Created Keycloak user with ID: {}", keycloakId);

            // Assign role to user
            String roleName = mapWaiterRoleToKeycloakRole(dto.role());
            keycloakAdminService.assignRealmRole(keycloakId, roleName);
            log.info("Assigned role {} to user {}", roleName, keycloakId);

        } catch (KeycloakException e) {
            log.error("Failed to create user in Keycloak: {}", e.getMessage());
            throw new ValidationException("Failed to create user in Keycloak: " + e.getMessage());
        }

        // Create waiter in database
        Waiter waiter = waiterMapper.toEntity(dto);
        waiter.setKeycloakId(keycloakId);

        Waiter saved = waiterRepository.save(waiter);
        log.info("Created waiter in database with ID: {}", saved.getId());

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
        log.info("Updating waiter with ID: {}", id);
        Waiter waiter = findWaiterOrThrow(id);

        // Update user in Keycloak if email or fullName changed
        if (dto.email() != null || dto.fullName() != null) {
            try {
                KeycloakUserRepresentation keycloakUpdate = buildKeycloakUpdateUser(dto);
                keycloakAdminService.updateUser(waiter.getKeycloakId(), keycloakUpdate);
                log.info("Updated Keycloak user: {}", waiter.getKeycloakId());
            } catch (KeycloakException e) {
                log.error("Failed to update user in Keycloak: {}", e.getMessage());
                throw new ValidationException("Failed to update user in Keycloak: " + e.getMessage());
            }
        }

        // Update enabled status in Keycloak if active flag changed
        if (dto.active() != null && !dto.active().equals(waiter.getActive())) {
            try {
                keycloakAdminService.setUserEnabled(waiter.getKeycloakId(), dto.active());
                log.info("Updated Keycloak user enabled status: {}", dto.active());
            } catch (KeycloakException e) {
                log.error("Failed to update user enabled status in Keycloak: {}", e.getMessage());
                throw new ValidationException("Failed to update user status in Keycloak: " + e.getMessage());
            }
        }

        // Update waiter in database
        waiterMapper.updateEntity(dto, waiter);
        Waiter updated = waiterRepository.save(waiter);
        log.info("Updated waiter in database: {}", updated.getId());

        return waiterMapper.toDto(updated);
    }

    // деактивация официанта
    @Override
    public void deactivateWaiter(UUID id) {
        log.info("Deactivating waiter with ID: {}", id);
        Waiter waiter = findWaiterOrThrow(id);

        // Disable user in Keycloak
        try {
            keycloakAdminService.setUserEnabled(waiter.getKeycloakId(), false);
            log.info("Disabled Keycloak user: {}", waiter.getKeycloakId());
        } catch (KeycloakException e) {
            log.error("Failed to disable user in Keycloak: {}", e.getMessage());
            throw new ValidationException("Failed to disable user in Keycloak: " + e.getMessage());
        }

        // Deactivate waiter in database
        waiter.setActive(false);
        waiterRepository.save(waiter);
        log.info("Deactivated waiter in database: {}", id);
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

    private KeycloakUserRepresentation buildKeycloakUser(CreateWaiterDto dto) {
        String[] names = splitFullName(dto.fullName());

        return KeycloakUserRepresentation.builder()
                .username(dto.username())
                .email(dto.email())
                .firstName(names[0])
                .lastName(names[1])
                .enabled(true)
                .emailVerified(true)
                .credentials(List.of(KeycloakCredentialRepresentation.password(dto.password(), false)))
                .build();
    }

    private KeycloakUserRepresentation buildKeycloakUpdateUser(UpdateWaiterDto dto) {
        String[] names = dto.fullName() != null ? splitFullName(dto.fullName()) : new String[]{null, null};

        return KeycloakUserRepresentation.builder()
                .email(dto.email())
                .firstName(names[0])
                .lastName(names[1])
                .build();
    }

    private String[] splitFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new String[]{"", ""};
        }

        String[] parts = fullName.trim().split("\\s+", 2);
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[1] : "";

        return new String[]{firstName, lastName};
    }

    private String mapWaiterRoleToKeycloakRole(WaiterRole role) {
        return switch (role) {
            case ADMIN -> "ADMIN";
            case WAITER -> "WAITER";
        };
    }
}
