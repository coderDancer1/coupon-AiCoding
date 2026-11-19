package com.coupon.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建优惠券请求参数。
 */
@Data
public class CouponCreateRequest {

    /** 优惠券名称 */
    @NotBlank(message = "优惠券名称不能为空")
    @Size(max = 64, message = "优惠券名称长度不能超过64个字符")
    private String name;

    /** 优惠金额(元)，不能小于0 */
    @NotNull(message = "优惠金额不能为空")
    @DecimalMin(value = "0.00", message = "优惠金额不能小于0")
    private BigDecimal amount;

    /** 发行总量，必须大于0 */
    @NotNull(message = "发行总量不能为空")
    @Positive(message = "发行总量必须大于0")
    private Integer totalCount;

    /** 有效期开始时间 */
    @NotNull(message = "有效期开始时间不能为空")
    private LocalDateTime validStartTime;

    /** 有效期结束时间 */
    @NotNull(message = "有效期结束时间不能为空")
    private LocalDateTime validEndTime;
}
