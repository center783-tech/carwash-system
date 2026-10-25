package com.carwash.controller;

import com.carwash.constant.DiscountType;
import com.carwash.controller.dto.CouponBatchIssueRequest;
import com.carwash.controller.dto.CouponBatchIssueResponse;
import com.carwash.controller.dto.CouponRequest;
import com.carwash.controller.dto.CouponResponse;
import com.carwash.controller.dto.MemberCouponResponse;
import com.carwash.entity.Coupon;
import com.carwash.response.ApiResponse;
import com.carwash.entity.MemberCoupon;
import com.carwash.service.CouponService;
import com.carwash.service.MailService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "優惠券", description = "優惠券建立、發放給會員、會員優惠券查詢")
@RestController
@RequestMapping("/api/coupons")
@Slf4j
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final MailService mailService;

    @PostMapping
    public ApiResponse<CouponResponse> create(@Valid @RequestBody CouponRequest req) {
        Coupon coupon = Coupon.builder()
                .code(req.getCode())
                .discountType(req.getDiscountType())
                .discountValue(req.getDiscountValue())
                .minAmount(req.getMinAmount())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .usageLimit(req.getUsageLimit())
                .build();
        return ApiResponse.success("優惠券建立成功", CouponResponse.from(couponService.create(coupon)));
    }

    @GetMapping
    public ApiResponse<List<CouponResponse>> listActive() {
        return ApiResponse.success(couponService.listActive().stream()
                .map(CouponResponse::from).collect(Collectors.toList()));
    }

    @PostMapping("/{couponId}/issue")
    public ApiResponse<MemberCouponResponse> issueToMember(@PathVariable Long couponId, @RequestParam Long memberId) {
        MemberCoupon memberCoupon = couponService.issueToMember(memberId, couponId);
        MemberCouponResponse response = MemberCouponResponse.from(memberCoupon);
        response.setEmailSent(notifyMember(memberCoupon));
        return ApiResponse.success("發放成功", response);
    }

    /** 後台勾選多位會員一次發放；某位會員寄信失敗不影響其他人，結果回報成功與寄信失敗人數 */
    @PostMapping("/{couponId}/issue-batch")
    public ApiResponse<CouponBatchIssueResponse> issueToMembers(
            @PathVariable Long couponId, @Valid @RequestBody CouponBatchIssueRequest req) {
        int issued = 0;
        int emailFailed = 0;
        for (Long memberId : req.getMemberIds().stream().distinct().toList()) {
            MemberCoupon memberCoupon = couponService.issueToMember(memberId, couponId);
            issued++;
            if (!notifyMember(memberCoupon)) emailFailed++;
        }
        return ApiResponse.success("發放完成", new CouponBatchIssueResponse(issued, emailFailed));
    }

    /** 寄通知信給會員；寄信失敗不影響已完成的發放，只回傳是否成功讓後台提示 */
    private boolean notifyMember(MemberCoupon memberCoupon) {
        var member = memberCoupon.getMember();
        var coupon = memberCoupon.getCoupon();
        String name = member.getRealName() != null && !member.getRealName().isBlank()
                ? member.getRealName() : member.getUsername();
        String discount = coupon.getDiscountType() == DiscountType.FIXED
                ? "折抵 NT$" + coupon.getDiscountValue().stripTrailingZeros().toPlainString()
                : coupon.getDiscountValue().stripTrailingZeros().toPlainString() + "% 折扣";
        boolean hasThreshold = coupon.getMinAmount() != null && coupon.getMinAmount().signum() > 0;
        String threshold = hasThreshold
                ? "最低消費 NT$" + coupon.getMinAmount().stripTrailingZeros().toPlainString() : "無最低消費";
        try {
            mailService.sendEmail(member.getEmail(), "【洗車預約系統】您收到一張優惠券",
                    name + " 您好，\n\n我們為您發放了一張優惠券：\n\n"
                            + "優惠券代碼：" + coupon.getCode() + "\n"
                            + "優惠內容：" + discount + "\n"
                            + "使用門檻：" + threshold + "\n"
                            + "有效期限：" + coupon.getEndDate() + "\n\n"
                            + "登入後在「我的優惠券」即可查看，預約時選擇使用。");
            return true;
        } catch (MailException e) {
            log.warn("Coupon notification mail failed. memberId={}, couponId={}", member.getId(), coupon.getId(), e);
            return false;
        }
    }

    /** 會員到店出示優惠券：開始 10 分鐘倒數，時間到自動變成已使用 */
    @PostMapping("/member-coupons/{memberCouponId}/redeem")
    public ApiResponse<MemberCouponResponse> redeem(@PathVariable Long memberCouponId, @RequestParam Long memberId) {
        return ApiResponse.success(MemberCouponResponse.from(couponService.startRedeem(memberCouponId, memberId)));
    }

    @GetMapping("/member/{memberId}")
    public ApiResponse<List<MemberCouponResponse>> listMemberCoupons(@PathVariable Long memberId) {
        return ApiResponse.success(couponService.listMemberCoupons(memberId).stream()
                .map(MemberCouponResponse::from).collect(Collectors.toList()));
    }
}
