package com.coupon.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 分页查询优惠券请求参数。
 */
@Data
public class CouponQueryRequest {

    /** 页码，从1开始 */
    @Min(value = 1, message = "页码必须大于等于1")
    private Integer page = 1;

    /** 每页大小 */
    @Min(value = 1, message = "每页大小必须大于等于1")
    @Max(value = 100, message = "每页大小不能超过100")
    private Integer size = 10;

    /** 优惠券名称（模糊匹配，可选） */
    private String name;
}
