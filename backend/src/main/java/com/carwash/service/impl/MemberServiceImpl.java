package com.carwash.service.impl;

import com.carwash.entity.Member;
import com.carwash.exception.BusinessException;
import com.carwash.exception.ResourceNotFoundException;
import com.carwash.repository.MemberRepository;
import com.carwash.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Member register(Member member) {
        if (memberRepository.existsByUsername(member.getUsername())) {
            throw new BusinessException("帳號已被使用");
        }
        if (memberRepository.existsByEmail(member.getEmail())) {
            throw new BusinessException("Email 已被註冊");
        }
        member.setPassword(passwordEncoder.encode(member.getPassword()));
        member.setStatus(1);
        return memberRepository.save(member);
    }

    @Override
    @Transactional(readOnly = true)
    public Member login(String username, String rawPassword) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("帳號或密碼錯誤"));
        if (!passwordEncoder.matches(rawPassword, member.getPassword())) {
            throw new BusinessException("帳號或密碼錯誤");
        }
        if (member.getStatus() == 0) {
            throw new BusinessException("此帳號已被停用");
        }
        return member;
    }

    @Override
    @Transactional(readOnly = true)
    public Member getById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Member", id));
    }

    @Override
    public Member updateProfile(Long id, Member updates) {
        Member member = getById(id);
        member.setRealName(updates.getRealName());
        member.setPhone(updates.getPhone());
        member.setAvatarUrl(updates.getAvatarUrl());
        return memberRepository.save(member);
    }

    @Override
    public void changePassword(Long id, String oldPassword, String newPassword) {
        Member member = getById(id);
        if (!passwordEncoder.matches(oldPassword, member.getPassword())) {
            throw new BusinessException("原密碼不正確");
        }
        member.setPassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Member> findAll() {
        return memberRepository.findAll();
    }
}
