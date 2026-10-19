package com.carwash.constant;

/**
 * 共用的驗證正規表示式，給 Controller 的 DTO 用 @Pattern，或 Service 層做額外檢查時使用。
 */
public final class RegexConstants {

    private RegexConstants() {
    }

    /** 台灣手機號碼：09 開頭共 10 碼 */
    public static final String PHONE_REGEX = "^09\\d{8}$";

    /** 台灣新式車牌：3 碼英數字 + 連字號 + 4 碼數字（例如 ABC-1234），實務上格式變化多，必要時放寬 */
    public static final String PLATE_NUMBER_REGEX = "^[A-Z0-9]{2,4}-[A-Z0-9]{2,4}$";

    /** 密碼規則：至少 8 碼，需同時包含英文字母與數字 */
    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,}$";

    /** 優惠券代碼：大寫英數字，4~20 碼 */
    public static final String COUPON_CODE_REGEX = "^[A-Z0-9]{4,20}$";
}
