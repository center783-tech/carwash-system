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

    // ------- 常用 TTL（秒） -------
    public static final long SERVICE_LIST_TTL_SECONDS = 600;       // 10 分鐘
    public static final long EMPLOYEE_SCHEDULE_TTL_SECONDS = 300;  // 5 分鐘
    public static final long LOGIN_FAIL_COUNT_TTL_SECONDS = 900;   // 15 分鐘
}
