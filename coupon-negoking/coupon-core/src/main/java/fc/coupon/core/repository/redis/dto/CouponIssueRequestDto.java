package fc.coupon.core.repository.redis.dto;

import lombok.Builder;

@Builder
public record CouponIssueRequestDto(
        long couponId,
        long userId
) {
}
