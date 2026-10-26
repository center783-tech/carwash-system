package com.carwash.security;

import com.carwash.constant.SecurityConstants;
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
    /**
     * 驗證 JWT 時用：依 token 內的 role 只查對應的表。
     * 兩張表的 username 各自獨立，若只靠 username 查，會員註冊一個和管理員相同的帳號，
     * 就會讓該管理員的 token 被解析成那位會員。
     */
    public UserDetails loadByUsernameAndRole(String username, String role) {
        if (SecurityConstants.ROLE_MEMBER.equals(role)) {
            return memberRepository.findByUsername(username)
                    .map(UserPrincipal::fromMember)
                    .orElseThrow(() -> new UsernameNotFoundException("找不到使用者: " + username));
        }
        return adminRepository.findByUsername(username)
                .map(UserPrincipal::fromAdmin)
                .filter(p -> p.getRole().equals(role))
                .orElseThrow(() -> new UsernameNotFoundException("找不到使用者: " + username));
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return memberRepository.findByUsername(username)
                .map(UserPrincipal::fromMember)
                .or(() -> adminRepository.findByUsername(username).map(UserPrincipal::fromAdmin))
                .orElseThrow(() -> new UsernameNotFoundException("找不到使用者: " + username));
    }
}
