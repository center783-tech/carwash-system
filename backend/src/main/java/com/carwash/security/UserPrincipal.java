package com.carwash.security;

import com.carwash.constant.SecurityConstants;
import com.carwash.entity.Admin;
import com.carwash.entity.Member;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 統一包裝「會員」或「後台管理員」兩種使用者，供 Spring Security 認證使用。
 * role 會被轉成 Spring Security 的權限字串，例如 ROLE_MEMBER / ROLE_ADMIN / ROLE_STAFF。
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String role;   // MEMBER / ADMIN / STAFF
    private final Integer status; // 1:啟用 0:停用

    private UserPrincipal(Long id, String username, String password, String role, Integer status) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.status = status;
    }

    public static UserPrincipal fromMember(Member member) {
        return new UserPrincipal(member.getId(), member.getUsername(), member.getPassword(),
                "MEMBER", member.getStatus());
    }

    public static UserPrincipal fromAdmin(Admin admin) {
        return new UserPrincipal(admin.getId(), admin.getUsername(), admin.getPassword(),
                admin.getRole().name(), admin.getStatus());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(SecurityConstants.ROLE_PREFIX + role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != null && status == 1;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status != null && status == 1;
    }
}
