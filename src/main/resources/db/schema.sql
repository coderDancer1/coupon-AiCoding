-- =============================================================
-- 优惠券管理系统数据库建表脚本
-- 数据库：coupon_db（需先手动创建）
-- 执行方式：mysql -uroot -p coupon_db < schema.sql
-- =============================================================

-- 优惠券主表
CREATE TABLE IF NOT EXISTS `coupon` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name`             VARCHAR(64)   NOT NULL                COMMENT '优惠券名称',
  `amount`           DECIMAL(10,2) NOT NULL                COMMENT '优惠金额(元)',
  `total_count`      INT           NOT NULL                COMMENT '发行总量',
  `received_count`   INT           NOT NULL DEFAULT 0      COMMENT '已领取数量',
  `valid_start_time` DATETIME      NOT NULL                COMMENT '有效期开始时间',
  `valid_end_time`   DATETIME      NOT NULL                COMMENT '有效期结束时间',
  `deleted`          TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0-未删除 1-已删除',
  `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_valid_time` (`valid_start_time`, `valid_end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='优惠券主表';

-- 用户领取记录表
CREATE TABLE IF NOT EXISTS `user_coupon` (
  `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`      BIGINT   NOT NULL                COMMENT '用户ID',
  `coupon_id`    BIGINT   NOT NULL                COMMENT '优惠券ID',
  `status`       TINYINT  NOT NULL DEFAULT 0      COMMENT '状态(预留): 0-未使用',
  `receive_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_coupon` (`user_id`, `coupon_id`),
  KEY `idx_coupon_id` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户领取记录表';
