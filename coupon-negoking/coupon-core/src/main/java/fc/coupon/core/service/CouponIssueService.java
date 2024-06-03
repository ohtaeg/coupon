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
