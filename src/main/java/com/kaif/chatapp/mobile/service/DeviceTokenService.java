package com.kaif.chatapp.mobile.service;

import com.kaif.chatapp.mobile.dto.RegisterDeviceRequest;
import com.kaif.chatapp.mobile.model.DeviceToken;
import com.kaif.chatapp.mobile.repository.DeviceTokenRepository;
import com.kaif.chatapp.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;

    @Transactional
    public void registerDevice(User user, RegisterDeviceRequest request) {
        deviceTokenRepository.findByToken(request.getToken()).ifPresentOrElse(existing -> {
            existing.setUserId(user.getId());
            existing.setPlatform(request.getPlatform().toUpperCase());
            deviceTokenRepository.save(existing);
        }, () -> deviceTokenRepository.save(DeviceToken.builder()
                .userId(user.getId())
                .token(request.getToken())
                .platform(request.getPlatform().toUpperCase())
                .createdAt(LocalDateTime.now())
                .build()));
    }

    @Transactional
    public void unregisterDevice(User user, String token) {
        deviceTokenRepository.findByToken(token)
                .filter(t -> t.getUserId().equals(user.getId()))
                .ifPresent(deviceTokenRepository::delete);
    }
}
