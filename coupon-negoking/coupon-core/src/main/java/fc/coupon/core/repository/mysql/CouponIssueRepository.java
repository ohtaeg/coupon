package fc.coupon.core.repository.mysql;

import com.querydsl.jpa.JPQLQueryFactory;
import fc.coupon.core.model.CouponIssue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import static fc.coupon.core.model.QCouponIssue.couponIssue;

@RequiredArgsConstructor
@Repository
public class CouponIssueRepository {

    private final JPQLQueryFactory queryFactory;

    public CouponIssue findCouponIssue(long id, long userId) {
        return queryFactory.selectFrom(couponIssue)
                .where(couponIssue.couponId.eq(id))
                .where(couponIssue.userId.eq(userId))
                .fetchFirst();
    }
}
