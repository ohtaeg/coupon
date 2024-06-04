package fc.coupon.api.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
public record CouponIssueResponse(boolean isSuccess, String comment) {
}
