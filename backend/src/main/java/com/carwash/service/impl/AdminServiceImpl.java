package com.carwash.service.impl;

import com.carwash.entity.Admin;
import com.carwash.exception.BusinessException;
import com.carwash.repository.AdminRepository;
import com.carwash.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Admin login(String username, String rawPassword) {
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("帳號或密碼錯誤"));
        if (!passwordEncoder.matches(rawPassword, admin.getPassword())) {
            throw new BusinessException("帳號或密碼錯誤");
        }
        if (admin.getStatus() == 0) {
            throw new BusinessException("此帳號已被停用");
        }
        return admin;
    }
}
