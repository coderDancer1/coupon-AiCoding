package com.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coupon.common.ResultCode;
import com.coupon.dto.CouponCreateRequest;
import com.coupon.dto.CouponQueryRequest;
import com.coupon.dto.CouponUpdateRequest;
import com.coupon.entity.Coupon;
import com.coupon.exception.BusinessException;
import com.coupon.mapper.CouponMapper;
import com.coupon.service.CouponService;
import com.coupon.vo.CouponVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * 优惠券管理业务实现。
 */
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class CouponServiceImpl implements CouponService {

    private final CouponMapper couponMapper;

    @Override
    public Long createCoupon(CouponCreateRequest request) {
        validateTimeRange(request.getValidStartTime(), request.getValidEndTime());

        Coupon coupon = new Coupon();
        coupon.setName(request.getName());
        coupon.setAmount(request.getAmount());
        coupon.setTotalCount(request.getTotalCount());
        coupon.setReceivedCount(0);
        coupon.setValidStartTime(request.getValidStartTime());
        coupon.setValidEndTime(request.getValidEndTime());
        couponMapper.insert(coupon);
        return coupon.getId();
    }

    @Override
    public Page<CouponVO> pageCoupons(CouponQueryRequest request) {
        Page<Coupon> page = new Page<>(request.getPage(), request.getSize());

        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(request.getName()), Coupon::getName, request.getName());
        wrapper.orderByDesc(Coupon::getId);

        Page<Coupon> couponPage = couponMapper.selectPage(page, wrapper);

        Page<CouponVO> voPage = new Page<>(couponPage.getCurrent(), couponPage.getSize(), couponPage.getTotal());
        voPage.setRecords(couponPage.getRecords().stream().map(CouponVO::from).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public CouponVO getCoupon(Long id) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "优惠券不存在");
        }
        return CouponVO.from(coupon);
    }

    @Override
    public void updateCoupon(Long id, CouponUpdateRequest request) {
        validateTimeRange(request.getValidStartTime(), request.getValidEndTime());

        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "优惠券不存在");
        }

        // 数据库层并发保护：仅当 received_count <= totalCount 时更新成功，
        // 避免并发领取与修改发行总量同时发生时出现 received_count > total_count
        int affected = couponMapper.updateCouponConditional(
                id, request.getName(), request.getAmount(), request.getTotalCount(),
                request.getValidStartTime(), request.getValidEndTime());
        if (affected == 0) {
            throw new BusinessException("发行总量不能小于已领取数量");
        }
    }

    @Override
    public void deleteCoupon(Long id) {
        int affected = couponMapper.deleteById(id);
        if (affected == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "优惠券不存在");
        }
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && !start.isBefore(end)) {
            throw new BusinessException("有效期开始时间必须早于结束时间");
        }
    }
}
