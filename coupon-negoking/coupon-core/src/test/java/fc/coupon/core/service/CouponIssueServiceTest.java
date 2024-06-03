package fc.coupon.core.service;

import fc.coupon.core.TestConfig;
import fc.coupon.core.exception.CouponIssueException;
import fc.coupon.core.exception.ErrorCode;
import fc.coupon.core.model.Coupon;
import fc.coupon.core.model.CouponIssue;
import fc.coupon.core.model.CouponType;
import fc.coupon.core.repository.mysql.CouponIssueJpaRepository;
import fc.coupon.core.repository.mysql.CouponIssueRepository;
import fc.coupon.core.repository.mysql.CouponJpaRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static fc.coupon.core.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;

class CouponIssueServiceTest extends TestConfig {

    @Autowired
    CouponIssueService service;

    @Autowired
    CouponIssueJpaRepository couponIssueJpaRepository;

    @Autowired
    CouponIssueRepository couponIssueRepository;

    @Autowired
    CouponJpaRepository couponJpaRepository;

    @BeforeEach
    void setUp() {
        this.couponJpaRepository.deleteAllInBatch();
        this.couponIssueJpaRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("쿠폰 발급 내역이 존재하면 예외를 반환한다.")
    void saveCouponIssue_fail_alreadyIssuedCoupon() {
        final CouponIssue couponIssue = CouponIssue.builder().couponId(1L).userId(1L).build();
        this.couponIssueJpaRepository.save(couponIssue);

        final CouponIssueException couponIssueException = assertThrows(CouponIssueException.class, () -> service.saveCouponIssue(couponIssue.getCouponId(), couponIssue.getUserId()));

        Assertions.assertEquals(couponIssueException.getErrorCode(), DUPLICATED_COUPON_ISSUE);
    }

    @Test
    @DisplayName("쿠폰 발급 내역이 존재하지 않는 경우 쿠폰 발급을 성공한다.")
    void saveCouponIssue_fail_existCouponIssue() {
        long couponId = 1L;
        long userId = 1L;

        final CouponIssue actual = this.service.saveCouponIssue(couponId, userId);

        Assertions.assertTrue(this.couponIssueJpaRepository.findById(actual.getId()).isPresent());
        Assertions.assertEquals(actual.getId(), couponId);
    }

    @Test
    @DisplayName("발급 수량, 기한, 중복 발급 문제가 없는 정상적인 쿠폰을 발급한다.")
    void issue_success() {
        // given
        long userId = 1L;
        Coupon coupon = Coupon.builder().couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(100)
                .issuedQuantity(0)
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(1))
                .build();
        this.couponJpaRepository.save(coupon);

        // when
        this.service.issue(coupon.getId(), userId);

        // then
        Coupon result = this.couponJpaRepository.findById(coupon.getId()).get();
        CouponIssue issueResult = this.couponIssueRepository.findCouponIssue(coupon.getId(), userId);
        assertAll(
                () -> Assertions.assertEquals(result.getIssuedQuantity(), 1),
                () -> Assertions.assertNotNull(issueResult)
        );
    }

    @Test
    @DisplayName("발급 수량에 문제가 있다면 예외를 반환한다.")
    void issue_fail_quantity() {
        // given
        long userId = 1L;
        Coupon coupon = Coupon.builder().couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(100)
                .issuedQuantity(100)
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(1))
                .build();
        this.couponJpaRepository.save(coupon);

        // when
        CouponIssueException actual = assertThrows(CouponIssueException.class, () -> this.service.issue(coupon.getId(), userId));

        // then
        assertEquals(actual.getErrorCode(), ErrorCode.INVALID_COUPON_ISSUE_QUANTITY);
    }

    @Test
    @DisplayName("발급 기한에 문제가 있다면 예외를 반환한다")
    void issue_fail_dateIssue() {
        // given
        long userId = 1;
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(100)
                .issuedQuantity(0)
                .dateIssueStart(LocalDateTime.now().minusDays(2))
                .dateIssueEnd(LocalDateTime.now().minusDays(1))
                .build();
        couponJpaRepository.save(coupon);
        // when & then
        CouponIssueException exception = Assertions.assertThrows(CouponIssueException.class, () -> {
            this.service.issue(coupon.getId(), userId);
        });
        Assertions.assertEquals(exception.getErrorCode(), INVALID_COUPON_ISSUE_DATE);
    }

    @Test
    @DisplayName("중복 발급 검증에 문제가 있다면 예외를 반환한다")
    void issue_4() {
        // given
        long userId = 1;
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(100)
                .issuedQuantity(0)
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(1))
                .build();
        couponJpaRepository.save(coupon);

        CouponIssue couponIssue = CouponIssue.builder()
                .couponId(coupon.getId())
                .userId(userId)
                .build();
        couponIssueJpaRepository.save(couponIssue);

        // when & then
        CouponIssueException exception = Assertions.assertThrows(CouponIssueException.class, () -> {
            service.issue(coupon.getId(), userId);
        });
        Assertions.assertEquals(exception.getErrorCode(), DUPLICATED_COUPON_ISSUE);
    }

    @Test
    @DisplayName("쿠폰이 존재하지 않는다면 예외를 반환한다")
    void issue_5() {
        // given
        long userId = 1;
        long couponId = 1;

        // when & then
        CouponIssueException exception = Assertions.assertThrows(CouponIssueException.class, () -> {
            service.issue(couponId, userId);
        });
        Assertions.assertEquals(exception.getErrorCode(), COUPON_NOT_EXIST);
    }
}