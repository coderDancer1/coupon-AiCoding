package com.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户领取记录实体，对应表 user_coupon。
 */
@Data
@TableName("user_coupon")
public class UserCoupon {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 优惠券ID */
    private Long couponId;

    /** 状态(预留): 0-未使用 */
    private Integer status;

    /** 领取时间 */
    private LocalDateTime receiveTime;
}
