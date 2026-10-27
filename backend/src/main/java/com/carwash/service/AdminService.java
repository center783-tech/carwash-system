package com.carwash.service;

import com.carwash.constant.AdminRole;
import com.carwash.entity.Admin;

import java.util.List;

public interface AdminService {

    Admin login(String username, String rawPassword);

    List<Admin> listAccounts();

    Admin createAccount(String username, String rawPassword, AdminRole role);

    /** 啟用 / 停用帳號；不能停用自己，避免唯一的管理員把自己鎖在外面 */
    Admin updateStatus(Long id, Integer status, Long operatorId);
}
