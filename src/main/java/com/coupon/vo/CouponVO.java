package com.coupon.vo;

import com.coupon.entity.Coupon;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券视图对象（对外返回，不含 deleted 等内部字段）。
 */
@Data
public class CouponVO {

    private Long id;
    private String name;
    private BigDecimal amount;
    private Integer totalCount;
    private Integer receivedCount;
    private LocalDateTime validStartTime;
    private LocalDateTime validEndTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static CouponVO from(Coupon coupon) {
        CouponVO vo = new CouponVO();
        vo.setId(coupon.getId());
        vo.setName(coupon.getName());
        vo.setAmount(coupon.getAmount());
        vo.setTotalCount(coupon.getTotalCount());
        vo.setReceivedCount(coupon.getReceivedCount());
        vo.setValidStartTime(coupon.getValidStartTime());
        vo.setValidEndTime(coupon.getValidEndTime());
        vo.setCreateTime(coupon.getCreateTime());
        vo.setUpdateTime(coupon.getUpdateTime());
        return vo;
    }
}
