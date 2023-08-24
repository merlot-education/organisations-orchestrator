package eu.merloteducation.organisationsorchestrator;

import eu.merloteducation.organisationsorchestrator.service.KeycloakAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class OrganisationsOrchestratorApplicationTests {

	@MockBean
	private KeycloakAuthService keycloakAuthService;

	@Test
	void contextLoads() {
	}

}
