package com.sayless;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Pinned to disabled mode so an exported SAYLESS_AI_ENABLED=true can never make this test call Gemini.
@SpringBootTest(properties = {"sayless.ai.enabled=false", "sayless.ai.api-key="})
class SaylessBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
