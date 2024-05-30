from locust import task, FastHttpUser

class HelloWorld(FastHttpUser):
    connection_timeout = 10
    network_timeout = 10

    @task
    def hello_world(self):
        self.client.get("/hello")