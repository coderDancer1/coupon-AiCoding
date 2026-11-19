package com.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coupon.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券 Mapper。
 */
@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {

    /**
     * 原子扣减库存：仅当库存充足、未删除、且在有效期内时成功。
     * 返回影响行数，1 表示扣减成功，0 表示失败。
     */
    @Update("UPDATE coupon SET received_count = received_count + 1 " +
            "WHERE id = #{couponId} AND received_count < total_count AND deleted = 0 " +
            "AND valid_start_time <= NOW() AND valid_end_time >= NOW()")
    int deductStock(@Param("couponId") Long couponId);

    /**
     * 查询优惠券（含已逻辑删除的记录），用于领取失败时诊断具体原因。
     * 使用原始 SQL，绕过逻辑删除自动过滤。
     */
    @Select("SELECT id, name, amount, total_count, received_count, valid_start_time, valid_end_time, " +
            "deleted, create_time, update_time FROM coupon WHERE id = #{id}")
    Coupon selectByIdIncludeDeleted(@Param("id") Long id);

    /**
     * 修改优惠券（数据库层并发保护）。
     * 仅当 received_count &lt;= totalCount 时更新成功，避免并发领取与修改发行总量
     * 同时发生时出现 received_count &gt; total_count。
     */
    @Update("UPDATE coupon SET name = #{name}, amount = #{amount}, total_count = #{totalCount}, " +
            "valid_start_time = #{validStartTime}, valid_end_time = #{validEndTime} " +
            "WHERE id = #{id} AND deleted = 0 AND received_count <= #{totalCount}")
    int updateCouponConditional(@Param("id") Long id,
                                @Param("name") String name,
                                @Param("amount") BigDecimal amount,
                                @Param("totalCount") Integer totalCount,
                                @Param("validStartTime") LocalDateTime validStartTime,
                                @Param("validEndTime") LocalDateTime validEndTime);
}
