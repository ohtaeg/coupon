package fc.coupon.core.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class DistributeLockExecutor {

    private final RedissonClient redissonClient;

    /**
     *
     * @param lockName - lock key
     * @param wait - millisecond, 락 획득 대기 시간
     * @param release- millisecond, 락 획득 후 소유 시간
     * @param runnable
     */
    public void execute(final String lockName, final long wait, final long release, Runnable runnable) {
        RLock lock = redissonClient.getLock(lockName);

        // 락 획득 시도
        try {
            // 락 획득 결과
            final boolean isGetLock = lock.tryLock(wait, release, TimeUnit.MILLISECONDS);
            // 락 획득 실패시, 락없이 로직을 진행하는 경우 동시성 이슈 발생할 수 있기에 예외처리
            if (!isGetLock) {
                throw new IllegalStateException("[" + lockName+ "] lock 획득 실패");
            }
            runnable.run();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            // 락 반환
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
