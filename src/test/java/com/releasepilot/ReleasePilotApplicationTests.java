package com.releasepilot;

import com.releasepilot.application.event.DomainEventPublisher;
import com.releasepilot.domain.repository.PromotionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class ReleasePilotApplicationTests {

	// Using @MockBean tells Spring to inject these mocks into the context
	// automatically. This satisfies Sonar because it uses the framework's
	// built-in mechanism instead of manual 'Mockito.mock()' calls.

	@MockitoBean
	private PromotionRepository promotionRepository;

	@MockitoBean
	private DomainEventPublisher domainEventPublisher;

	@Test
	void contextLoads() {
		// Context loads successfully
	}
}