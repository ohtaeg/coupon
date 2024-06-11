package fc.coupon.core.service;

import fc.coupon.core.exception.CouponIssueException;
import fc.coupon.core.exception.ErrorCode;
import fc.coupon.core.model.Coupon;
import fc.coupon.core.model.CouponIssue;
import fc.coupon.core.repository.mysql.CouponIssueJpaRepository;
import fc.coupon.core.repository.mysql.CouponIssueRepository;
import fc.coupon.core.repository.mysql.CouponJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class CouponIssueService {
    private final CouponJpaRepository couponJpaRepository;
    private final CouponIssueJpaRepository couponIssueJpaRepository;
    private final CouponIssueRepository couponIssueRepository;

    /**
     * 이렇게 lock을 걸어도 실제로 원하는 수량을 초과해서 발급한다.
     * 그 이유는 트랜잭션 커밋되기전에 lock을 반납해서이다.
     * 순서를 보자.
     *
     * 트랜잭션 시작
     *   lock 획득
     *     issue()
     *   lock 반납
     * 트랜잭션 종료 (커밋)
     *
     * 순으로 동작하는데
     * 1번 요청 진행중에 2번 요청은 트랜잭션은 시작했지만 lock을 획득하지 못해 대기중일 때
     * 1번 요청이 lock을 반납하고 2번 요청이 lock을 획득하게 된다.
     * 2번은 쿠폰 발급 요청이 되기전 데이터를 읽는다.
     * 그리고나서 1번 요청은 커밋을 lock을 반납하고 하개되므로 동시성 이슈가 발생할 수 있다.
     *
     * 고로 트랜잭션 내에 lock을 거는 행위는 주의해야한다.
     *
     * lock 획득
     *   트랜잭션 시작
     *     issue()
     *   트랜잭션 종료 (커밋)
     * lock 반납
     */
//    @Transactional
//    public void issue(long couponId, long userId) {
//        synchronized (this) {
//            Coupon coupon = this.findCoupon(couponId);
//            coupon.issue();
//            this.saveCouponIssue(couponId, userId);
//        }
//    }

    @Transactional
    public void issue(long couponId, long userId) {
        Coupon coupon = this.findCoupon(couponId);
        coupon.issue();
        this.saveCouponIssue(couponId, userId);
    }

    @Transactional(readOnly = true)
    public Coupon findCoupon(long couponId) {
        return couponJpaRepository.findById(couponId).orElseThrow(() -> new CouponIssueException(ErrorCode.COUPON_NOT_EXIST, "쿠폰이 존재하지 않습니다. %s".formatted(couponId)));
    }

    @Transactional
    public CouponIssue saveCouponIssue(final long couponId, final long userId) {
        this.checkAlreadyIssuance(couponId, userId);

        CouponIssue couponIssue = CouponIssue.builder()
                .couponId(couponId)
                .userId(userId)
                .build();

        return this.couponIssueJpaRepository.save(couponIssue);
    }

    private void checkAlreadyIssuance(long couponId, long userId) {
        final CouponIssue couponIssue = couponIssueRepository.findCouponIssue(couponId, userId);
        if (couponIssue != null) {
            throw new CouponIssueException(ErrorCode.DUPLICATED_COUPON_ISSUE, "쿠폰 중복 발급 couponId : %s, userId : %s".formatted(couponId, userId));
        }
    }
}
