package com.coupon.controller;

import com.coupon.common.Result;
import com.coupon.service.CouponReceiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户领取优惠券接口。
 */
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponReceiveController {

    private final CouponReceiveService couponReceiveService;

    /** 用户领取优惠券 */
    @PostMapping("/{couponId}/receive")
    public Result<Long> receive(@PathVariable Long couponId,
                                @RequestParam(required = false) Long userId) {
        return Result.ok("领取成功", couponReceiveService.receiveCoupon(couponId, userId));
    }
}
