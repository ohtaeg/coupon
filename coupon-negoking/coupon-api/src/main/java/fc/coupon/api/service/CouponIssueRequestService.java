package fc.coupon.api.service;

import fc.coupon.api.controller.dto.CouponIssueRequestDto;
import fc.coupon.core.component.DistributeLockExecutor;
import fc.coupon.core.service.AsyncCouponIssueService;
import fc.coupon.core.service.CouponIssueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static fc.coupon.core.component.DistributeLockExecutor.LOCK_PREFIX;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponIssueRequestService {
    private final CouponIssueService couponIssueService;
    private final DistributeLockExecutor lockExecutor;

    private final AsyncCouponIssueService asyncCouponIssueService;

    public void issueV1(CouponIssueRequestDto requestDto) {
        // this.issueV1WithSynchronized(requestDto);
        this.issueV1WithRedisDistributeLock(requestDto);
        // this.issueV1WithXLock(requestDto);
        log.info("쿠폰 발급 완료. couponId : %s, userId : %s".formatted(requestDto.couponId(), requestDto.userId()));
    }

    /**
     * synchronized는 어플리케이션에 종속되기 때문에
     * 여러 서버로 확장되면 lock을 관리할 수 없어 분산락을 구현해야한다.
     */
    public void issueV1WithSynchronized(CouponIssueRequestDto requestDto) {
        synchronized (this) {
            couponIssueService.issue(requestDto.couponId(), requestDto.userId());
        }
    }

    /**
     * 레디스 분산락
     */
    public void issueV1WithRedisDistributeLock(CouponIssueRequestDto requestDto) {
        final String lockName = LOCK_PREFIX + requestDto.couponId();
        lockExecutor.execute(lockName, 5_000, 5_000,
                () -> couponIssueService.issue(requestDto.couponId(), requestDto.userId())
        );
    }


    /**
     * mysql X LOCK
     */
    public void issueV1WithXLock(CouponIssueRequestDto requestDto) {
        couponIssueService.issueWithXLock(requestDto.couponId(), requestDto.userId());
    }

    /**
     * redis 기반 비동기 쿠폰 발급
     */
    public void asyncIssue(CouponIssueRequestDto requestDto) {
        asyncCouponIssueService.issue(requestDto.couponId(), requestDto.userId());
    }
}
