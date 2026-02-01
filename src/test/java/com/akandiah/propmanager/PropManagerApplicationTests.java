package com.akandiah.propmanager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.akandiah.propmanager.config.TestSecurityConfig;

@SpringBootTest
@Import(TestSecurityConfig.class)
class PropManagerApplicationTests {

	@Test
	void contextLoads() {
	}

}
