package com.coupon.service;

/**
 * 用户领取优惠券业务接口。
 */
public interface CouponReceiveService {

    /**
     * 用户领取优惠券，返回领取记录ID。
     *
     * @param couponId 优惠券ID
     * @param userId   用户ID
     * @return 领取记录ID
     */
    Long receiveCoupon(Long couponId, Long userId);
}
