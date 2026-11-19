package com.coupon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coupon.dto.CouponCreateRequest;
import com.coupon.entity.Coupon;
import com.coupon.entity.UserCoupon;
import com.coupon.exception.BusinessException;
import com.coupon.mapper.CouponMapper;
import com.coupon.mapper.UserCouponMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 领取优惠券业务测试（真实 MySQL，非事务回滚，用 @BeforeEach 清理表保证隔离）。
 */
@SpringBootTest
class CouponReceiveServiceTest {

    @Autowired
    private CouponReceiveService couponReceiveService;

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM user_coupon");
        jdbcTemplate.update("DELETE FROM coupon");
    }

    /** 创建一张指定库存、当前有效期的优惠券 */
    private Long createCoupon(int stock) {
        CouponCreateRequest req = new CouponCreateRequest();
        req.setName("领取测试券");
        req.setAmount(new BigDecimal("5.00"));
        req.setTotalCount(stock);
        req.setValidStartTime(LocalDateTime.now().minusDays(1));
        req.setValidEndTime(LocalDateTime.now().plusDays(30));
        return couponService.createCoupon(req);
    }

    private long countRecords(Long couponId) {
        return userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getCouponId, couponId));
    }

    private long countRecords(Long couponId, Long userId) {
        return userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getCouponId, couponId)
                .eq(UserCoupon::getUserId, userId));
    }

    @Test
    @DisplayName("正常领取成功")
    void receiveSuccess() {
        Long couponId = createCoupon(10);
        Long recordId = couponReceiveService.receiveCoupon(couponId, 1001L);
        assertNotNull(recordId);

        assertEquals(1, couponMapper.selectById(couponId).getReceivedCount());
        assertEquals(1, countRecords(couponId));
    }

    @Test
    @DisplayName("库存为0时领取失败")
    void receiveWhenStockZero() {
        Long couponId = createCoupon(0);
        BusinessException e = assertThrows(BusinessException.class,
                () -> couponReceiveService.receiveCoupon(couponId, 1001L));
        assertEquals("库存不足，优惠券已领完", e.getMessage());
    }

    @Test
    @DisplayName("优惠券不存在时领取失败")
    void receiveNonExistentCoupon() {
        assertThrows(BusinessException.class,
                () -> couponReceiveService.receiveCoupon(999999L, 1001L));
    }

    @Test
    @DisplayName("优惠券已删除时领取失败")
    void receiveDeletedCoupon() {
        Long couponId = createCoupon(10);
        couponService.deleteCoupon(couponId);

        BusinessException e = assertThrows(BusinessException.class,
                () -> couponReceiveService.receiveCoupon(couponId, 1001L));
        assertEquals("优惠券已删除", e.getMessage());
    }

    @Test
    @DisplayName("优惠券未生效时领取失败")
    void receiveNotStartedCoupon() {
        CouponCreateRequest req = new CouponCreateRequest();
        req.setName("未生效券");
        req.setAmount(new BigDecimal("5.00"));
        req.setTotalCount(10);
        req.setValidStartTime(LocalDateTime.now().plusDays(1));
        req.setValidEndTime(LocalDateTime.now().plusDays(30));
        Long couponId = couponService.createCoupon(req);

        BusinessException e = assertThrows(BusinessException.class,
                () -> couponReceiveService.receiveCoupon(couponId, 1001L));
        assertEquals("优惠券尚未生效", e.getMessage());
    }

    @Test
    @DisplayName("优惠券已过期时领取失败")
    void receiveExpiredCoupon() {
        CouponCreateRequest req = new CouponCreateRequest();
        req.setName("已过期券");
        req.setAmount(new BigDecimal("5.00"));
        req.setTotalCount(10);
        req.setValidStartTime(LocalDateTime.now().minusDays(30));
        req.setValidEndTime(LocalDateTime.now().minusDays(1));
        Long couponId = couponService.createCoupon(req);

        BusinessException e = assertThrows(BusinessException.class,
                () -> couponReceiveService.receiveCoupon(couponId, 1001L));
        assertEquals("优惠券已过期", e.getMessage());
    }

    @Test
    @DisplayName("同一用户重复领取失败")
    void receiveDuplicateBySameUser() {
        Long couponId = createCoupon(10);
        Long userId = 1001L;

        couponReceiveService.receiveCoupon(couponId, userId); // 第一次成功
        BusinessException e = assertThrows(BusinessException.class,
                () -> couponReceiveService.receiveCoupon(couponId, userId));
        assertEquals("您已领取过该优惠券", e.getMessage());

        // 第二次领取的库存扣减已回滚，库存仍为1
        assertEquals(1, couponMapper.selectById(couponId).getReceivedCount());
        assertEquals(1, countRecords(couponId, userId));
    }

    @Test
    @DisplayName("100个不同用户并发抢最后一张券，只有一个成功")
    void concurrentReceiveLastCouponBy100Users() throws Exception {
        Long couponId = createCoupon(1);
        int threadCount = 100;
        AtomicInteger success = new AtomicInteger(0);

        runConcurrent(threadCount, i -> {
            try {
                couponReceiveService.receiveCoupon(couponId, 2000L + i);
                success.incrementAndGet();
            } catch (BusinessException ignored) {
                // 领取失败（库存不足）
            }
        });

        assertEquals(1, success.get());
        assertEquals(1, couponMapper.selectById(couponId).getReceivedCount());
        assertEquals(1, countRecords(couponId));
    }

    @Test
    @DisplayName("同一用户并发领取同一张券，只有一个成功")
    void concurrentReceiveBySameUser() throws Exception {
        // 库存充足，让所有扣减都能成功，验证唯一索引兜底防重复
        Long couponId = createCoupon(100);
        long userId = 3000L;
        int threadCount = 50;
        AtomicInteger success = new AtomicInteger(0);

        runConcurrent(threadCount, i -> {
            try {
                couponReceiveService.receiveCoupon(couponId, userId);
                success.incrementAndGet();
            } catch (BusinessException ignored) {
            }
        });

        assertEquals(1, success.get());
        assertEquals(1, couponMapper.selectById(couponId).getReceivedCount());
        assertEquals(1, countRecords(couponId, userId));
    }

    @Test
    @DisplayName("领取记录插入失败时，库存更新回滚")
    void insertFailThenStockRollback() {
        Long couponId = createCoupon(10);
        Long userId = 4000L;

        // 将已领取数量设为5，便于观察回滚效果
        jdbcTemplate.update("UPDATE coupon SET received_count = 5 WHERE id = ?", couponId);

        // 预先插入一条领取记录，制造唯一键冲突
        UserCoupon existing = new UserCoupon();
        existing.setUserId(userId);
        existing.setCouponId(couponId);
        existing.setStatus(0);
        userCouponMapper.insert(existing);

        assertThrows(BusinessException.class,
                () -> couponReceiveService.receiveCoupon(couponId, userId));

        // 扣减（5->6）已随事务回滚，仍为5
        assertEquals(5, couponMapper.selectById(couponId).getReceivedCount());
        assertEquals(1, countRecords(couponId, userId));
    }

    /** 并发执行工具：n 个线程同时执行 action */
    private void runConcurrent(int n, IntConsumer action) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(n);

        for (int i = 0; i < n; i++) {
            final int idx = i;
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    action.accept(idx);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        assertTrue(ready.await(10, TimeUnit.SECONDS), "并发线程未就绪");
        start.countDown();
        assertTrue(done.await(60, TimeUnit.SECONDS), "并发执行超时");
        pool.shutdown();
    }
}
