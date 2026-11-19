package com.coupon.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coupon.common.Result;
import com.coupon.dto.CouponCreateRequest;
import com.coupon.dto.CouponQueryRequest;
import com.coupon.dto.CouponUpdateRequest;
import com.coupon.service.CouponService;
import com.coupon.vo.CouponVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 优惠券管理接口。
 */
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    /** 创建优惠券 */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CouponCreateRequest request) {
        return Result.ok(couponService.createCoupon(request));
    }

    /** 分页查询优惠券 */
    @GetMapping
    public Result<Page<CouponVO>> page(@Valid CouponQueryRequest request) {
        return Result.ok(couponService.pageCoupons(request));
    }

    /** 查询优惠券详情 */
    @GetMapping("/{id}")
    public Result<CouponVO> detail(@PathVariable Long id) {
        return Result.ok(couponService.getCoupon(id));
    }

    /** 修改优惠券 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CouponUpdateRequest request) {
        couponService.updateCoupon(id, request);
        return Result.ok();
    }

    /** 删除优惠券（逻辑删除） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return Result.ok();
    }
}
