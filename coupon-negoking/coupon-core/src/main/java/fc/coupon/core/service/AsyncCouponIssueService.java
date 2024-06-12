package fc.coupon.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fc.coupon.core.component.DistributeLockExecutor;
import fc.coupon.core.exception.CouponIssueException;
import fc.coupon.core.exception.ErrorCode;
import fc.coupon.core.model.Coupon;
import fc.coupon.core.repository.redis.RedisRepository;
import fc.coupon.core.repository.redis.dto.CouponIssueRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static fc.coupon.core.component.DistributeLockExecutor.LOCK_PREFIX;
import static fc.coupon.core.util.CouponRedisUtils.getIssueRequestKey;
import static fc.coupon.core.util.CouponRedisUtils.getIssueRequestQueue;

@Service
@RequiredArgsConstructor
public class AsyncCouponIssueService {
    private final RedisRepository redisRepository;
    private final CouponIssueService couponIssueService;
    private final DistributeLockExecutor lockExecutor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void issue(long couponId, long userId) {
        final Coupon coupon = couponIssueService.findCoupon(couponId);
        if (!coupon.availableIssueDate()) {
            throw new CouponIssueException(ErrorCode.INVALID_COUPON_ISSUE_DATE, "발급 가능한 일자가 아닙니다.");
        }

        final String lockName = LOCK_PREFIX + couponId;
        lockExecutor.execute(lockName, 5_000, 5_000,
                () -> {
                    if (!availableTotalIssueQuantity(couponId, coupon.getTotalQuantity())) {
                        throw new CouponIssueException(ErrorCode.INVALID_COUPON_ISSUE_QUANTITY, "발급 가능한 수량을 초과합니다.");
                    }

                    if (!availableUserIssueQuantity(couponId, userId)) {
                        throw new CouponIssueException(ErrorCode.DUPLICATED_COUPON_ISSUE, "이미 발급 요청이 처리되었습니다.");
                    }

                    this.issueRequest(couponId, userId);
                }
        );
    }

    public boolean availableUserIssueQuantity(long couponId, long userId) {
        final String key = getIssueRequestKey(couponId);
        return !redisRepository.sIsMember(key, String.valueOf(userId));
    }

    public boolean availableTotalIssueQuantity(long couponId, Integer limit) {
        if (Objects.isNull(limit)) {
            return true;
        }

        final String key = getIssueRequestKey(couponId);
        return limit > redisRepository.sCard(key);
    }

    private void issueRequest(long couponId, long userId) {
        final CouponIssueRequestDto issueRequestDto = CouponIssueRequestDto.builder()
                .couponId(couponId)
                .userId(userId)
                .build();

        final String setKey = getIssueRequestKey(couponId);
        final String queueKey = getIssueRequestQueue();

        try {
            String value = objectMapper.writeValueAsString(issueRequestDto);
            redisRepository.sAdd(setKey, String.valueOf(userId));
            redisRepository.rPush(queueKey, value);
        } catch (Exception e) {
            throw new CouponIssueException(ErrorCode.FAIL_COUPON_ISSUE_REQUEST, "input : %s".formatted(issueRequestDto));
        }
    }

}
