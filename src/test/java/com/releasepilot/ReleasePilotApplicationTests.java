package com.releasepilot;

import com.releasepilot.application.event.PromotionEventPublisher;
import com.releasepilot.domain.repository.PromotionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class ReleasePilotApplicationTests extends AbstractIntegrationTest {

	@Autowired
	private DataSource dataSource;

	@MockitoBean
	private PromotionRepository promotionRepository;

	@MockitoBean
	private PromotionEventPublisher promotionEventPublisher;

	@Test
	void contextLoads() {
		assertNotNull(dataSource);
	}
}
