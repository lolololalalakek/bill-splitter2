package uz.billsplitter2.demo.service;

import uz.billsplitter2.demo.dto.request.BillRequestDto;
import uz.billsplitter2.demo.dto.response.BillResponseDto;

public interface BillCalculationService {
    BillResponseDto split(BillRequestDto requestDto);
}
