
CREATE TABLE `coupon`.`coupon`
(
    `id`                   BIGINT(20) NOT NULL AUTO_INCREMENT,
    `title`                VARCHAR(255) NOT NULL COMMENT '쿠폰명',
    `coupon_type`          VARCHAR(255) NOT NULL COMMENT '쿠폰 타입 (선착순 쿠폰, ..)',
    # 쿠폰 성격에 따라 최대 수량이 필요없을 수 있기 때문에 nullable
    `total_quantity`       INT NULL COMMENT '쿠폰 발급 최대 수량',
    `issued_quantity`      INT          NOT NULL COMMENT '발급된 쿠폰 수량',
    `discount_amount`      INT          NOT NULL COMMENT '할인 금액',
    `min_available_amount` INT          NOT NULL COMMENT '최소 사용 금액',
    `date_issue_start`     datetime(6) NOT NULL COMMENT '발급 시작 일시',
    `date_issue_end`       datetime(6) NOT NULL COMMENT '발급 종료 일시',
    `date_created`         datetime(6) NOT NULL COMMENT '생성 일시',
    `date_updated`         datetime(6) NOT NULL COMMENT '수정 일시',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
    COMMENT '쿠폰 정책';

CREATE TABLE `coupon`.`coupon_issue`
(
    `id`           BIGINT(20) NOT NULL AUTO_INCREMENT,
    `coupon_id`    BIGINT(20) NOT NULL COMMENT '쿠폰 ID',
    `user_id`      BIGINT(20) NOT NULL COMMENT '유저 ID',
    `date_issued`  datetime(6) NOT NULL COMMENT '발급 일시',
    `date_used`    datetime(6) NULL COMMENT '사용 일시',
    `date_created` datetime(6) NOT NULL COMMENT '생성 일시',
    `date_updated` datetime(6) NOT NULL COMMENT '수정 일시',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
    COMMENT '쿠폰 발급 내역';

INSERT INTO coupon.coupon
(date_updated, date_created, coupon_type, date_issue_end, date_issue_start, discount_amount, issued_quantity, min_available_amount, title, total_quantity)
VALUES ('2024-06-04 21:33:15.000000', '2024-06-04 21:33:15.000000', 'FIRST_COME_FIRST_SERVED', '2024-12-30 21:35:24.000000', '2024-04-02 21:35:03.000000', 100000, 0, 110000, 'test', 500)
