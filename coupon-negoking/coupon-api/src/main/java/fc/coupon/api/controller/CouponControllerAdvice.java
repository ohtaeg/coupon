package fc.coupon.api.controller;

import fc.coupon.api.controller.dto.CouponIssueResponse;
import fc.coupon.core.exception.CouponIssueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CouponControllerAdvice {

    @ExceptionHandler(CouponIssueException.class)
    public CouponIssueResponse couponIssueExceptionHandler(CouponIssueException e) {
        return new CouponIssueResponse(false, e.getErrorCode().message);
    }
}
