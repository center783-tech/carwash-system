package com.carwash.constant;

/**
 * 共用的驗證正規表示式，給 Controller 的 DTO 用 @Pattern，或 Service 層做額外檢查時使用。
 */
public final class RegexConstants {

    private RegexConstants() {
    }

    /** 台灣手機號碼：09 開頭共 10 碼 */
    public static final String PHONE_REGEX = "^09\\d{8}$";

    /** 台灣小客車車牌：新式 ABC-1234，舊式如 AB-1234、1234-AB、ABC-123、123-ABC */
    public static final String PLATE_NUMBER_REGEX = "^([A-Z]{2,3}-\\d{3,4}|\\d{3,4}-[A-Z]{2,3})$";

    /** 密碼規則：至少 8 碼，需同時包含英文字母與數字 */
    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,}$";

    /** 優惠券代碼：大寫英數字，4~20 碼 */
    public static final String COUPON_CODE_REGEX = "^[A-Z0-9]{4,20}$";
}
