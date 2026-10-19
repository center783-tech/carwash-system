package com.carwash.constant;

/**
 * 跨模組共用的常數，避免 magic number / magic string 散落在各個 Service、Controller 裡。
 */
public final class CommonConstants {

    private CommonConstants() {
        // 工具類別，不允許實例化
    }

    // ------- 分頁預設值 -------
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    // ------- 日期時間格式 -------
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = "HH:mm";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    // ------- 營業時間（預約時段選擇用，對應前端 CreateReservation.vue 的 el-time-select） -------
    public static final String BUSINESS_START_TIME = "09:00";
    public static final String BUSINESS_END_TIME = "20:00";
    public static final int TIME_SLOT_STEP_MINUTES = 30;

    // ------- 優惠券百分比折扣的基準值（PERCENTAGE 類型時，discountValue 是以 100 為基準的折扣率） -------
    public static final int PERCENTAGE_BASE = 100;
}
