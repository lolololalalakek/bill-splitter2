package uz.billsplitter2.demo.service;

import uz.billsplitter2.demo.dto.request.CreateWaiterDto;
import uz.billsplitter2.demo.dto.request.UpdateWaiterDto;
import uz.billsplitter2.demo.dto.response.WaiterDto;

import java.util.List;
import java.util.UUID;

public interface WaiterService {

    WaiterDto createWaiter(CreateWaiterDto dto);

    List<WaiterDto> getAllWaiters();

    WaiterDto getWaiterById(UUID id);

    WaiterDto updateWaiter(UUID id, UpdateWaiterDto dto);

    void deactivateWaiter(UUID id);

    List<WaiterDto> getActiveWaiters();
}
