import random
from locust import task, FastHttpUser

class CouponIssueV1(FastHttpUser):
    connection_timeout = 10
    network_timeout = 10

    @task
    def issue(self):
        payload = {
            "userId" : random.randint(1, 10_000_000),
            "couponId" : 3,
        }
        with self.rest("POST", "/v1/issue-async", json=payload):
            pass