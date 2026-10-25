package com.carwash.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CouponBatchIssueResponse {

    /** 成功發放的人數 */
    private int issued;

    /** 已發放但通知信寄送失敗的人數 */
    private int emailFailed;
}
