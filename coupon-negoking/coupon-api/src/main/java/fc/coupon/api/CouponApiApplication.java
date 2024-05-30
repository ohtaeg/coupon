package fc.coupon.api;

import fc.coupon.core.CoreConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import(CoreConfiguration.class)
@SpringBootApplication
public class CouponApiApplication {

	public static void main(String[] args) {
		System.setProperty("spring.config.name", "application-core,application-api");
		SpringApplication.run(CouponApiApplication.class, args);
	}

}
