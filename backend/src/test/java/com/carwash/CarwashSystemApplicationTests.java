package com.carwash;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:carwash;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver"
})
class CarwashSystemApplicationTests {

	@Test
	void contextLoads() {
	}

}
