package com.carwash.service.impl;

import com.carwash.constant.AdminRole;
import com.carwash.entity.Admin;
import com.carwash.exception.BusinessException;
import com.carwash.exception.ResourceNotFoundException;
import com.carwash.repository.AdminRepository;
import com.carwash.service.AdminService;
import com.carwash.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;

    @Override
    public Admin login(String username, String rawPassword) {
        loginAttemptService.checkNotLocked(LoginAttemptService.SCOPE_ADMIN, username);
        Admin admin = adminRepository.findByUsername(username).orElse(null);
        // 帳號不存在也要計次，否則可以靠「有沒有被鎖」來探測帳號是否存在
        if (admin == null || !passwordEncoder.matches(rawPassword, admin.getPassword())) {
            loginAttemptService.recordFailure(LoginAttemptService.SCOPE_ADMIN, username);
            throw new BusinessException("帳號或密碼錯誤");
        }
        if (admin.getStatus() == 0) {
            throw new BusinessException("此帳號已被停用");
        }
        loginAttemptService.reset(LoginAttemptService.SCOPE_ADMIN, username);
        return admin;
    }

    @Override
    public List<Admin> listAccounts() {
        return adminRepository.findAll();
    }

    @Override
    @Transactional
    public Admin createAccount(String username, String rawPassword, AdminRole role) {
        if (adminRepository.findByUsername(username).isPresent()) {
            throw new BusinessException("此帳號已存在");
        }
        return adminRepository.save(Admin.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .build());
    }

    @Override
    @Transactional
    public Admin updateStatus(Long id, Integer status, Long operatorId) {
        if (id.equals(operatorId)) {
            throw new BusinessException("不能變更自己的帳號狀態");
        }
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("帳號", id));
        admin.setStatus(status);
        return admin;
    }
}
