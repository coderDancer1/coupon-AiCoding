package com.coupon.service.impl;

import com.coupon.common.ResultCode;
import com.coupon.entity.Coupon;
import com.coupon.entity.UserCoupon;
import com.coupon.exception.BusinessException;
import com.coupon.mapper.CouponMapper;
import com.coupon.mapper.UserCouponMapper;
import com.coupon.service.CouponReceiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户领取优惠券业务实现。
 */
@Service
@RequiredArgsConstructor
public class CouponReceiveServiceImpl implements CouponReceiveService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long receiveCoupon(Long couponId, Long userId) {
        if (couponId == null || couponId <= 0) {
            throw new BusinessException("优惠券ID不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户ID不能为空");
        }

        // 1. 原子扣减库存（条件更新，防止超发）
        int affected = couponMapper.deductStock(couponId);
        if (affected == 0) {
            throw diagnoseFailReason(couponId);
        }

        // 2. 插入领取记录（唯一索引 uk_user_coupon 防止重复领取）
        UserCoupon record = new UserCoupon();
        record.setUserId(userId);
        record.setCouponId(couponId);
        record.setStatus(0);
        try {
            userCouponMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 唯一键冲突说明已领取过；抛出异常触发事务回滚，库存扣减随之回滚
            throw new BusinessException("您已领取过该优惠券");
        }

        return record.getId();
    }

    /**
     * 扣减失败时诊断具体原因（仅用于错误提示，不参与库存判断）。
     */
    private BusinessException diagnoseFailReason(Long couponId) {
        Coupon coupon = couponMapper.selectByIdIncludeDeleted(couponId);
        if (coupon == null) {
            return new BusinessException(ResultCode.NOT_FOUND.getCode(), "优惠券不存在");
        }
        if (coupon.getDeleted() != null && coupon.getDeleted() == 1) {
            return new BusinessException("优惠券已删除");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getValidStartTime())) {
            return new BusinessException("优惠券尚未生效");
        }
        if (now.isAfter(coupon.getValidEndTime())) {
            return new BusinessException("优惠券已过期");
        }
        return new BusinessException("库存不足，优惠券已领完");
    }
}
