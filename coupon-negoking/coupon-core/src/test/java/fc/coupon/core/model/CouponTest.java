package fc.coupon.core.model;

import fc.coupon.core.exception.CouponIssueException;
import fc.coupon.core.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CouponTest {

    @Test
    @DisplayName("쿠폰 발급 최대 수량이 초과되지 않은 경우 쿠폰 발급을 성공한다.")
    void availableIssueQuantity_success() {
        Coupon coupon = Coupon.builder()
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(2))
                .totalQuantity(100)
                .issuedQuantity(99)
                .build();

        coupon.issue();

        assertEquals(coupon.getIssuedQuantity(), 100);
    }

    @Test
    @DisplayName("쿠폰 발급 최대 수량이 null인 경우 쿠폰 발급을 성공한다.")
    void availableIssueQuantity_success_null() {
        Coupon coupon = Coupon.builder()
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(2))
                .totalQuantity(null)
                .issuedQuantity(99)
                .build();

        coupon.issue();

        assertEquals(coupon.getIssuedQuantity(), 100);
    }

    @Test
    @DisplayName("쿠폰 발급 최대 수량이 초과된 경우 쿠폰 발급을 실패한다.")
    void availableIssueQuantity_fail() {
        Coupon coupon = Coupon.builder()
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(2))
                .totalQuantity(100)
                .issuedQuantity(100)
                .build();

        CouponIssueException actual = assertThrows(CouponIssueException.class, coupon::issue);

        assertEquals(actual.getErrorCode(), ErrorCode.INVALID_COUPON_ISSUE_QUANTITY);
    }

    @Test
    @DisplayName("쿠폰 발급 기간에 해당되지 않으면 쿠폰 발급을 실패한다.")
    void availableIssueDate_fail_notYet() {
        Coupon coupon = Coupon.builder()
                .dateIssueStart(LocalDateTime.now().plusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(2))
                .totalQuantity(null)
                .issuedQuantity(100)
                .build();

        CouponIssueException actual = assertThrows(CouponIssueException.class, coupon::issue);

        assertEquals(actual.getErrorCode(), ErrorCode.INVALID_COUPON_ISSUE_DATE);
    }

    @Test
    @DisplayName("쿠폰 발급 기간에 해당되면 쿠폰 발급을 성공한다.")
    void availableIssueDate_success() {
        Coupon coupon = Coupon.builder()
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(2))
                .totalQuantity(null)
                .issuedQuantity(99)
                .build();

        coupon.issue();

        assertEquals(coupon.getIssuedQuantity(), 100);
    }
}