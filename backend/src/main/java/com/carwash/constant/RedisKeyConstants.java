package com.carwash.constant;

/**
 * Redis key 的前綴與組合規則。統一集中管理，避免不同模組各自拼字串造成 key 衝突或打錯字。
 * 命名規則：carwash:{模組}:{用途}:{識別碼}
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    private static final String APP_PREFIX = "carwash:";

    // ------- 服務項目：瀏覽頻率高、變動少，適合快取 -------
    public static final String SERVICE_ACTIVE_LIST = APP_PREFIX + "service:active-list";
    public static final String SERVICE_CATEGORY_LIST_PREFIX = APP_PREFIX + "service:category:";

    public static String serviceCategoryListKey(String category) {
        return SERVICE_CATEGORY_LIST_PREFIX + category;
    }

    // ------- 員工當日可服務時段：預約流程會高頻查詢，適合快取並在預約建立/取消時主動清除 -------
    public static final String EMPLOYEE_SCHEDULE_PREFIX = APP_PREFIX + "employee:schedule:";

    public static String employeeScheduleKey(Long employeeId, String date) {
        return EMPLOYEE_SCHEDULE_PREFIX + employeeId + ":" + date;
    }

    // ------- 優惠券：上架中的優惠券清單 -------
    public static final String COUPON_ACTIVE_LIST = APP_PREFIX + "coupon:active-list";

    // ------- 登入失敗次數（防暴力破解用，搭配帳號鎖定機制） -------
    public static final String LOGIN_FAIL_COUNT_PREFIX = APP_PREFIX + "login:fail-count:";

    public static String loginFailCountKey(String username) {
        return LOGIN_FAIL_COUNT_PREFIX + username;
    }

    // ------- 修改密碼的 Email 驗證碼：驗證碼本體、重寄冷卻、驗證錯誤次數 -------
    public static final String PASSWORD_CODE_PREFIX = APP_PREFIX + "member:pwd-code:";
    public static final String PASSWORD_CODE_COOLDOWN_PREFIX = APP_PREFIX + "member:pwd-code-cooldown:";
    public static final String PASSWORD_CODE_FAIL_PREFIX = APP_PREFIX + "member:pwd-code-fail:";

    public static String passwordCodeKey(Long memberId) {
        return PASSWORD_CODE_PREFIX + memberId;
    }

    public static String passwordCodeCooldownKey(Long memberId) {
        return PASSWORD_CODE_COOLDOWN_PREFIX + memberId;
    }

    public static String passwordCodeFailKey(Long memberId) {
        return PASSWORD_CODE_FAIL_PREFIX + memberId;
    }

    // ------- 忘記帳號 / 忘記密碼的 Email 連結 token：key 只存 token 的 SHA-256，Redis 外洩也無法直接拿來用 -------
    public static final String RECOVER_USERNAME_TOKEN_PREFIX = APP_PREFIX + "member:recover-username:";
    public static final String RESET_PASSWORD_TOKEN_PREFIX = APP_PREFIX + "member:reset-password:";
    public static final String RECOVER_COOLDOWN_PREFIX = APP_PREFIX + "member:recover-cooldown:";

    public static String recoverUsernameTokenKey(String tokenHash) {
        return RECOVER_USERNAME_TOKEN_PREFIX + tokenHash;
    }

    public static String resetPasswordTokenKey(String tokenHash) {
        return RESET_PASSWORD_TOKEN_PREFIX + tokenHash;
    }

    /** 同一個 Email、同一種用途的重寄冷卻 */
    public static String recoverCooldownKey(String type, String emailHash) {
        return RECOVER_COOLDOWN_PREFIX + type + ":" + emailHash;
    }

    // ------- 常用 TTL（秒） -------
    public static final long SERVICE_LIST_TTL_SECONDS = 600;       // 10 分鐘
    public static final long EMPLOYEE_SCHEDULE_TTL_SECONDS = 300;  // 5 分鐘
    public static final long LOGIN_FAIL_COUNT_TTL_SECONDS = 900;   // 15 分鐘
    public static final int LOGIN_MAX_FAILS = 5;                   // 時間窗內失敗達此次數即鎖定
    public static final long PASSWORD_CODE_TTL_SECONDS = 300;      // 驗證碼 5 分鐘有效
    public static final long PASSWORD_CODE_COOLDOWN_SECONDS = 60;  // 同一會員 60 秒內不可重寄
    public static final int PASSWORD_CODE_MAX_FAILS = 5;           // 驗證碼錯誤超過次數即作廢
    public static final long RECOVER_TOKEN_TTL_SECONDS = 900;      // 找回連結 15 分鐘有效
    public static final long RECOVER_COOLDOWN_SECONDS = 60;        // 同一 Email 60 秒內不可重寄
}
