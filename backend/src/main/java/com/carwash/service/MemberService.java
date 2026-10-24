package com.carwash.service;

import com.carwash.entity.Member;

import java.util.List;

public interface MemberService {

    Member register(Member member);

    Member login(String username, String rawPassword);

    Member getById(Long id);

    Member updateProfile(Long id, Member updates);

    /** 寄送修改密碼用的 6 位數驗證碼到會員註冊的 Email */
    void sendPasswordChangeCode(Long id);

    void changePassword(Long id, String oldPassword, String newPassword, String verificationCode);

    List<Member> findAll();
}
