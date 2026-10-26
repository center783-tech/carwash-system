package com.carwash.security;

import com.carwash.constant.SecurityConstants;
import org.springframework.security.access.AccessDeniedException;

/**
 * 資源擁有者檢查。SecurityConfig 只能擋「角色」，擋不了「會員 A 帶會員 B 的 id」，
 * 這類 IDOR 必須在 controller 比對登入者與資料擁有者。
 * 注意 ADMIN 與 MEMBER 的 id 是各自的自增序列，數字可能相同，所以一定要連 role 一起比對。
 */
public final class AccessGuard {

    private AccessGuard() {
    }

    public static boolean isStaff(UserPrincipal principal) {
        return principal != null
                && (SecurityConstants.ROLE_ADMIN.equals(principal.getRole())
                || SecurityConstants.ROLE_STAFF.equals(principal.getRole()));
    }

    /** 只允許會員本人 */
    public static void requireSelf(Long memberId, UserPrincipal principal) {
        if (!isSelf(memberId, principal)) {
            throw new AccessDeniedException("無權限存取此資源");
        }
    }

    /** 會員本人，或後台 ADMIN / STAFF */
    public static void requireSelfOrStaff(Long memberId, UserPrincipal principal) {
        if (!isSelf(memberId, principal) && !isStaff(principal)) {
            throw new AccessDeniedException("無權限存取此資源");
        }
    }

    private static boolean isSelf(Long memberId, UserPrincipal principal) {
        return principal != null
                && SecurityConstants.ROLE_MEMBER.equals(principal.getRole())
                && principal.getId() != null
                && principal.getId().equals(memberId);
    }
}
