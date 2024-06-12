package fc.coupon.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fc.coupon.core.TestConfig;
import fc.coupon.core.exception.CouponIssueException;
import fc.coupon.core.exception.ErrorCode;
import fc.coupon.core.model.Coupon;
import fc.coupon.core.model.CouponType;
import fc.coupon.core.repository.mysql.CouponJpaRepository;
import fc.coupon.core.repository.redis.dto.CouponIssueRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.stream.IntStream;

import static fc.coupon.core.util.CouponRedisUtils.getIssueRequestKey;
import static fc.coupon.core.util.CouponRedisUtils.getIssueRequestQueue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AsyncCouponIssueServiceTest extends TestConfig {
    @Autowired
    AsyncCouponIssueService asyncCouponIssueService;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @Autowired
    CouponJpaRepository couponJpaRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        Collection<String> redisKeys = redisTemplate.keys("*");
        redisTemplate.delete(redisKeys);
    }

    @Test
    @DisplayName("쿠폰 수량 검증 - 발급 가능 수량이 존재하면 true를 반환한다.")
    void availableTotalIssueQuantity_isTrue() {
        // given
        int limit = 10;
        long couponId = 1;

        // when
        boolean actual = asyncCouponIssueService.availableTotalIssueQuantity(couponId, limit);

        // then
        assertThat(actual).isTrue();
    }

    @Test
    @DisplayName("쿠폰 수량 검증 - 발급 가능 수량이 모두 소진되면 false를 반환한다.")
    void availableTotalIssueQuantity_isFalse() {
        // given
        int limit = 10;
        long couponId = 1;
        IntStream.range(0, limit).forEach(userId -> redisTemplate.opsForSet().add(getIssueRequestKey(couponId), String.valueOf(userId)));

        // when
        boolean actual = asyncCouponIssueService.availableTotalIssueQuantity(couponId, limit);

        // then
        assertThat(actual).isFalse();
    }

    @Test
    @DisplayName("쿠폰 중복 발급 검증 - 발급된 내역에 유저가 존재하지 않으면 true를 반환한다.")
    void availableUserIssueQuantity_isTrue() {
        // given
        long couponId = 1;
        long userId = 1;

        // when
        boolean actual = asyncCouponIssueService.availableUserIssueQuantity(couponId, userId);

        // then
        assertThat(actual).isTrue();
    }

    @Test
    @DisplayName("쿠폰 중복 발급 검증 - 발급된 내역에 유저가 존재하는 경우 false를 반환한다.")
    void availableUserIssueQuantity_isFalse() {
        // given
        long couponId = 1;
        long userId = 1;
        redisTemplate.opsForSet().add(getIssueRequestKey(couponId), String.valueOf(userId));

        // when
        boolean actual = asyncCouponIssueService.availableUserIssueQuantity(couponId, userId);

        // then
        assertThat(actual).isFalse();
    }

    @Test
    @DisplayName("쿠폰 발급 - 쿠폰이 존재하지 않는다면 예외를 발생한다.")
    void issue_notExistCoupon() {
        // given
        long couponId = 1;
        long userId = 1;

        // when
        final CouponIssueException actual = assertThrows(CouponIssueException.class, () -> asyncCouponIssueService.issue(couponId, userId));

        // then
        assertThat(actual.getErrorCode()).isEqualTo(ErrorCode.COUPON_NOT_EXIST);
    }

    @Test
    @DisplayName("쿠폰 발급 - 발급 가능 수량이 존재하지 않으면 예외를 발생한다.")
    void issue_notAvailableQuantity() {
        // given
        final Coupon coupon = Coupon.builder().couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(1))
                .build();
        couponJpaRepository.save(coupon);

        IntStream.range(0, coupon.getTotalQuantity()).forEach(userId -> redisTemplate.opsForSet().add(getIssueRequestKey(coupon.getId()), String.valueOf(userId)));

        // when
        final CouponIssueException actual = assertThrows(CouponIssueException.class, () -> asyncCouponIssueService.issue(coupon.getId(), 99));

        // then
        assertThat(actual.getErrorCode()).isEqualTo(ErrorCode.INVALID_COUPON_ISSUE_QUANTITY);
    }

    @Test
    @DisplayName("쿠폰 발급 - 쿠폰이 이미 발급된 유저라면 예외를 발생한다.")
    void issue_alreadyIssued() {
        // given
        long userId = 1;
        final Coupon coupon = Coupon.builder().couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(1))
                .build();
        couponJpaRepository.save(coupon);
        redisTemplate.opsForSet().add(getIssueRequestKey(coupon.getId()), String.valueOf(userId));

        // when
        final CouponIssueException actual = assertThrows(CouponIssueException.class, () -> asyncCouponIssueService.issue(coupon.getId(), userId));

        // then
        assertThat(actual.getErrorCode()).isEqualTo(ErrorCode.DUPLICATED_COUPON_ISSUE);
    }

    @Test
    @DisplayName("쿠폰 발급 - 쿠폰 발급 기한이 유효하지 않다면 예외를 발생한다.")
    void issue_invalidDate() {
        // given
        long userId = 1;
        final Coupon coupon = Coupon.builder().couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(LocalDateTime.now().plusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(2))
                .build();
        couponJpaRepository.save(coupon);
        redisTemplate.opsForSet().add(getIssueRequestKey(coupon.getId()), String.valueOf(userId));

        // when
        final CouponIssueException actual = assertThrows(CouponIssueException.class, () -> asyncCouponIssueService.issue(coupon.getId(), userId));

        // then
        assertThat(actual.getErrorCode()).isEqualTo(ErrorCode.INVALID_COUPON_ISSUE_DATE);
    }

    @Test
    @DisplayName("쿠폰 발급 - 쿠폰 발급 요청을 성공한다.")
    void issue_success() {
        // given
        long userId = 1;
        final Coupon coupon = Coupon.builder().couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(2))
                .build();
        couponJpaRepository.save(coupon);

        // when
        asyncCouponIssueService.issue(coupon.getId(), userId);
        Boolean actual = redisTemplate.opsForSet().isMember(getIssueRequestKey(coupon.getId()), String.valueOf(userId));

        // then
        assertThat(actual).isTrue();
    }

    @Test
    @DisplayName("쿠폰 발급 - 쿠폰 발급 요청을 성공하면 큐에 적재를 성공한다.")
    void issue_queue() throws JsonProcessingException {
        // given
        long userId = 1;
        final Coupon coupon = Coupon.builder().couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(2))
                .build();
        couponJpaRepository.save(coupon);
        final String expected = objectMapper.writeValueAsString(CouponIssueRequestDto.builder()
                .couponId(coupon.getId())
                .userId(userId)
                .build());

        // when
        asyncCouponIssueService.issue(coupon.getId(), userId);
        final String actual = redisTemplate.opsForList().leftPop(getIssueRequestQueue());

        // then
        assertThat(actual).isEqualTo(expected);
    }
}