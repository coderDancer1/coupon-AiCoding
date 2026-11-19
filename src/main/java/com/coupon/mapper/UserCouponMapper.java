package com.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coupon.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户领取记录 Mapper。
 */
@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {
}
