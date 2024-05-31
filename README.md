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
