package com.profmojo;

import com.profmojo.integration.BasePostgresContainerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("Application Context Loads Test")
class ProfMojoApplicationTests extends BasePostgresContainerTest {

	@Test
	@DisplayName("Spring Application Context loads cleanly with isolated test environment")
	void contextLoads() {
	}

}
