# coupon
이벤트 선착순 쿠폰 발급 시스템 개발을 위한 레포

## Setting
```shell
# mysql, redis 세팅
$ cd coupon-negoking
$ docker-compose up -d

# 부하테스트 locust 세팅
# worker가 1개인 경우 부하를 생성하면서 부하를 가하다보니 API 서버에서는 부하를 더 받을 수 있는데 cpu를 거의 다 사용하여 제대로 된 테스트가 안될 수 있음
$ cd loadtest
$ docker-compose up -d --scale worker=3
```

## Trouble Shooting
현재 이슈는 쿠폰이 500개만 발급이 되어야하지만 실제로 500개를 초과하는 동시성 이슈가 발생. 

동시성을 어떻게 해결할 수 있을까?

### 1. synchronized
```java
public void issueV1(CouponIssueRequestDto requestDto) {
    synchronized (this) {
        couponIssueService.issue(requestDto.couponId(), requestDto.userId());
    }
}
```

- before
  - 1000명의 유저로 부하테스트시 평균 RPS 1500
- synchronized after
  - 1000명의 유저로 부하테스트시 평균 RPS 200~300
  
  ![img.png](images/img.png)  


- 안정성은 확보했지만 해당 키워드는 어플리케이션에 종속적이기 때문에 여러 서버로 확장되면 lock을 관리할 수 없다.
- 분산락을 구현해보자.

<br>

### 2. 분산락
현재 스택에서 구현할 수 있는 분산락은 다음과 같다.
1. redis
2. mysql record lock


### 2.1 레디스 분산 락
```java
public void issueV1(CouponIssueRequestDto requestDto) {
    final String lockName = LOCK_PREFIX + requestDto.couponId();
    lockExecutor.execute(lockName, 10_000, 10_000,
        () -> couponIssueService.issue(requestDto.couponId(), requestDto.userId())
    );
}
```
![img_3.png](images/img_3.png)

데이터 수와 요청수가 많지 않아 synchronized 부하 테스트와 RPS는 큰 차이가 없다.

### 2.2 Mysql Record Lock
```java
public interface CouponJpaRepository extends JpaRepository<Coupon, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.id = :id")
    Optional<Coupon> findByIdWithLock(long id);
}

```

조회 쿼리에서 `for update` 키워드 사용
> SELECT * FROM coupon WHERE id = 1 FOR UPDATE;

쿠폰 id가 1인 record에 X Lock이 걸림, X LOCK은 중첩해서 걸 수 없다.

![img_2.png](images/img_2.png)


데이터와 요청수가 적다면 X Lock이 성능이 더 좋을 수 있지만 데이터 수가 많고 요청수가 많아질수록 redis 의 성능이 우수할 것으로 예상