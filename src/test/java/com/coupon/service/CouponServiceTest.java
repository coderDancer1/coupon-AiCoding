package com.coupon.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coupon.dto.CouponCreateRequest;
import com.coupon.dto.CouponQueryRequest;
import com.coupon.dto.CouponUpdateRequest;
import com.coupon.entity.Coupon;
import com.coupon.exception.BusinessException;
import com.coupon.mapper.CouponMapper;
import com.coupon.vo.CouponVO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 优惠券 CRUD 业务测试（真实 MySQL，@Transactional 回滚隔离）。
 */
@SpringBootTest
@Transactional
class CouponServiceTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private Validator validator;

    private CouponCreateRequest buildCreateRequest() {
        CouponCreateRequest req = new CouponCreateRequest();
        req.setName("测试优惠券");
        req.setAmount(new BigDecimal("10.50"));
        req.setTotalCount(100);
        req.setValidStartTime(LocalDateTime.now().minusDays(1));
        req.setValidEndTime(LocalDateTime.now().plusDays(30));
        return req;
    }

    private CouponUpdateRequest buildUpdateRequest() {
        CouponUpdateRequest req = new CouponUpdateRequest();
        req.setName("测试优惠券");
        req.setAmount(new BigDecimal("10.50"));
        req.setTotalCount(100);
        req.setValidStartTime(LocalDateTime.now().minusDays(1));
        req.setValidEndTime(LocalDateTime.now().plusDays(30));
        return req;
    }

    @Test
    @DisplayName("创建优惠券成功")
    void createCouponSuccess() {
        Long id = couponService.createCoupon(buildCreateRequest());
        assertNotNull(id);

        Coupon coupon = couponMapper.selectById(id);
        assertNotNull(coupon);
        assertEquals(0, coupon.getReceivedCount());
        assertEquals(0, coupon.getAmount().compareTo(new BigDecimal("10.50")));
    }

    @Test
    @DisplayName("创建优惠券参数非法")
    void createCouponInvalidParams() {
        CouponCreateRequest req = buildCreateRequest();
        req.setName(" ");
        req.setAmount(new BigDecimal("-1"));
        req.setTotalCount(0);

        Set<ConstraintViolation<CouponCreateRequest>> violations = validator.validate(req);
        Set<String> messages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertTrue(messages.contains("优惠券名称不能为空"));
        assertTrue(messages.contains("优惠金额不能小于0"));
        assertTrue(messages.contains("发行总量必须大于0"));
    }

    @Test
    @DisplayName("分页查询正常")
    void pageCoupons() {
        for (int i = 0; i < 3; i++) {
            CouponCreateRequest req = buildCreateRequest();
            req.setName("分页测试券" + i);
            couponService.createCoupon(req);
        }

        CouponQueryRequest query = new CouponQueryRequest();
        query.setPage(1);
        query.setSize(2);

        Page<CouponVO> page = couponService.pageCoupons(query);
        assertEquals(2, page.getRecords().size());
        assertTrue(page.getTotal() >= 3);
    }

    @Test
    @DisplayName("查询不存在的优惠券")
    void getNonExistentCoupon() {
        assertThrows(BusinessException.class, () -> couponService.getCoupon(999999L));
    }

    @Test
    @DisplayName("修改优惠券成功")
    void updateCouponSuccess() {
        Long id = couponService.createCoupon(buildCreateRequest());

        CouponUpdateRequest update = buildUpdateRequest();
        update.setName("改名后的券");
        update.setTotalCount(50);
        couponService.updateCoupon(id, update);

        Coupon coupon = couponMapper.selectById(id);
        assertEquals("改名后的券", coupon.getName());
        assertEquals(50, coupon.getTotalCount());
    }

    @Test
    @DisplayName("发行总量小于已领取数量时修改失败")
    void updateCouponTotalCountBelowReceivedFails() {
        Long id = couponService.createCoupon(buildCreateRequest());

        Coupon coupon = couponMapper.selectById(id);
        coupon.setReceivedCount(80);
        couponMapper.updateById(coupon);

        CouponUpdateRequest update = buildUpdateRequest();
        update.setTotalCount(50);
        assertThrows(BusinessException.class, () -> couponService.updateCoupon(id, update));
    }

    @Test
    @DisplayName("删除优惠券后正常查询不到")
    void deleteCouponThenNotFound() {
        Long id = couponService.createCoupon(buildCreateRequest());
        couponService.deleteCoupon(id);

        assertNull(couponMapper.selectById(id));
        assertThrows(BusinessException.class, () -> couponService.getCoupon(id));
    }

    @Test
    @DisplayName("有效期开始时间晚于结束时间时创建失败")
    void createCouponInvalidTimeRange() {
        CouponCreateRequest req = buildCreateRequest();
        req.setValidStartTime(LocalDateTime.now().plusDays(1));
        req.setValidEndTime(LocalDateTime.now());
        assertThrows(BusinessException.class, () -> couponService.createCoupon(req));
    }
}
