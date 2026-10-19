package com.carwash.security;

import com.carwash.repository.AdminRepository;
import com.carwash.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;
    private final AdminRepository adminRepository;

    /**
     * username 在會員跟管理員兩個表裡是各自獨立的命名空間，
     * 先嘗試當會員登入，找不到的話再嘗試當後台管理員登入。
     */
    @Override
    public UserDetails loadUserByUsername(String username) {
        return memberRepository.findByUsername(username)
                .map(UserPrincipal::fromMember)
                .or(() -> adminRepository.findByUsername(username).map(UserPrincipal::fromAdmin))
                .orElseThrow(() -> new UsernameNotFoundException("找不到使用者: " + username));
    }
}
