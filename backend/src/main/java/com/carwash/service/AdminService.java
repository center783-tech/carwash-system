package com.carwash.service;

import com.carwash.entity.Admin;

public interface AdminService {

    Admin login(String username, String rawPassword);
}
