package com.releasepilot;

import com.releasepilot.application.event.PromotionEventPublisher;
import com.releasepilot.domain.repository.PromotionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class ReleasePilotApplicationTests {

	@MockitoBean
	private PromotionRepository promotionRepository;

	@MockitoBean
	private PromotionEventPublisher promotionEventPublisher;

	@Test
	void contextLoads() {
		// Context loads successfully
	}
}
