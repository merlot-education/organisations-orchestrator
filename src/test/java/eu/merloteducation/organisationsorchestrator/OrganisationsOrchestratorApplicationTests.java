package eu.merloteducation.organisationsorchestrator;

import eu.merloteducation.organisationsorchestrator.config.InitialDataLoader;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class OrganisationsOrchestratorApplicationTests {

	@MockBean
	private InitialDataLoader initialDataLoader;

	@Test
	void contextLoads() {
		// no explicit assertions necessary, just be sure we reach this point
		assertTrue(true);
	}

}
