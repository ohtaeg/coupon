package fc.coupon.api.controller;

import fc.coupon.api.controller.dto.CouponIssueRequestDto;
import fc.coupon.api.controller.dto.CouponIssueResponse;
import fc.coupon.api.service.CouponIssueRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class CouponIssueController {
    private final CouponIssueRequestService couponIssueRequestService;

    @PostMapping("/v1/issue")
    public CouponIssueResponse issue(@RequestBody CouponIssueRequestDto body) {
        this.couponIssueRequestService.issueV1(body);
        return new CouponIssueResponse(true, null);
    }
}
