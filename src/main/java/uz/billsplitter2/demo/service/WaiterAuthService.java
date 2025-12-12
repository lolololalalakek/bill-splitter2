package uz.billsplitter2.demo.service;

import uz.billsplitter2.demo.dto.request.LogoutRequest;
import uz.billsplitter2.demo.dto.request.RefreshTokenRequest;
import uz.billsplitter2.demo.dto.request.WaiterLoginRequest;
import uz.billsplitter2.demo.dto.response.WaiterAuthResponse;

public interface WaiterAuthService {

    WaiterAuthResponse login(WaiterLoginRequest request);

    WaiterAuthResponse refreshToken(RefreshTokenRequest request);

    void logout(LogoutRequest request);
}
