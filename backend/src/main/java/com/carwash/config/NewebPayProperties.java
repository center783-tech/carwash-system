package com.carwash.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 藍新金流設定，對應 application.yml 的 newebpay.*（金鑰請放 .env，不要進版控） */
@Data
@Component
@ConfigurationProperties(prefix = "newebpay")
public class NewebPayProperties {

    private String merchantId;
    private String hashKey;
    private String hashIv;

    /** MPG 付款頁；測試 https://ccore.newebpay.com/MPG/mpg_gateway，正式 https://core.newebpay.com/MPG/mpg_gateway */
    private String gatewayUrl;

    /** 交易查詢 API；測試 https://ccore.newebpay.com/API/QueryTradeInfo，正式 https://core.newebpay.com/API/QueryTradeInfo */
    private String queryUrl;

    /** 使用者付款完成後瀏覽器被導回的後端網址（必須是藍新可連到的公開網址） */
    private String returnUrl;

    /** 藍新付款結果通知的後端網址（必須是藍新可連到的公開網址） */
    private String notifyUrl;
}
