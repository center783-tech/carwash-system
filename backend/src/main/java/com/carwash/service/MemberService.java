package com.carwash.service;

import com.carwash.entity.Member;

import java.util.List;

public interface MemberService {

    Member register(Member member);

    Member login(String username, String rawPassword);

    Member getById(Long id);

    Member updateProfile(Long id, Member updates);

    void changePassword(Long id, String oldPassword, String newPassword);

    List<Member> findAll();
}
