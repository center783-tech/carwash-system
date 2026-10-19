package com.carwash.constant;

/**
 * JWT 認證相關的固定字串，讓 JwtAuthenticationFilter、前端串接文件都能引用同一個來源。
 */
public final class SecurityConstants {

    private SecurityConstants() {
    }

    public static final String AUTH_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    /** JWT claims 裡自訂欄位的 key 名稱，要跟 JwtTokenProvider 產生/解析 token 時使用的 key 一致 */
    public static final String CLAIM_USER_ID = "id";
    public static final String CLAIM_ROLE = "role";

    // ------- Spring Security 角色前綴（hasRole("ADMIN") 實際比對的是 ROLE_ADMIN） -------
    public static final String ROLE_PREFIX = "ROLE_";
    public static final String ROLE_MEMBER = "MEMBER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_STAFF = "STAFF";
}
