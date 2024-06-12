package fc.coupon.core.service;

import fc.coupon.core.repository.redis.RedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AsyncCouponIssueService {
    private final RedisRepository redisRepository;

    /**
     * 1. Sorted Set에 요청을 추가 (NX 옵션 + ZADD score = time stamp)
     *     - ZADD의 응답 값 기반 발급 검증
     * 2. 현재 요청의 순서 조회 (ZRANK)
     * 3. 조회 결과를 선착순 조건과 비교
     * 4. 쿠폰 발급 Queue에 적재
     */
    public void issue(long couponId, long userId) {
        final String key = "issue:request:sorted-set:%s".formatted(couponId);
        redisRepository.zAdd(key, String.valueOf(userId), System.currentTimeMillis());
    }

}
