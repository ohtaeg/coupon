package fc.coupon.api.service;

import fc.coupon.api.controller.dto.CouponIssueRequestDto;
import fc.coupon.core.service.CouponIssueService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CouponIssueRequestService {
    private final Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());
    private final CouponIssueService couponIssueService;

    public void issue(CouponIssueRequestDto requestDto) {
        couponIssueService.issue(requestDto.couponId(), requestDto.userId());
        log.info("쿠폰 발급 완료. couponId : %s, userId : %s".formatted(requestDto.couponId(), requestDto.userId()));

    }
}
