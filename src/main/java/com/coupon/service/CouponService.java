package com.coupon.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coupon.dto.CouponCreateRequest;
import com.coupon.dto.CouponQueryRequest;
import com.coupon.dto.CouponUpdateRequest;
import com.coupon.vo.CouponVO;

/**
 * 优惠券管理业务接口。
 */
public interface CouponService {

    /**
     * 创建优惠券，返回新优惠券ID。
     */
    Long createCoupon(CouponCreateRequest request);

    /**
     * 分页查询优惠券，支持按名称模糊筛选。
     */
    Page<CouponVO> pageCoupons(CouponQueryRequest request);

    /**
     * 根据ID查询优惠券详情。
     */
    CouponVO getCoupon(Long id);

    /**
     * 修改优惠券。
     */
    void updateCoupon(Long id, CouponUpdateRequest request);

    /**
     * 逻辑删除优惠券。
     */
    void deleteCoupon(Long id);
}
