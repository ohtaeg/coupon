package fc.coupon.core.util;

public class CouponRedisUtils {
    public static String getIssueRequestKey(long couponId) {
        return "issue:request:%s".formatted(couponId);
    }

    public static String getIssueRequestQueue() {
        return "issue:request:queue";
    }
}
