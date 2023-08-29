package eu.merloteducation.organisationsorchestrator;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.merloteducation.organisationsorchestrator.config.JwtAuthConverter;
import eu.merloteducation.organisationsorchestrator.config.JwtAuthConverterProperties;
import eu.merloteducation.organisationsorchestrator.config.WebSecurityConfig;
import eu.merloteducation.organisationsorchestrator.controller.OrganizationQueryController;
import eu.merloteducation.organisationsorchestrator.controller.ParticipantShapeController;
import eu.merloteducation.organisationsorchestrator.models.dto.MerlotParticipantDto;
import eu.merloteducation.organisationsorchestrator.models.gxfscatalog.*;
import eu.merloteducation.organisationsorchestrator.service.GXFSCatalogRestService;
import eu.merloteducation.organisationsorchestrator.service.GXFSWizardRestService;
import eu.merloteducation.organisationsorchestrator.service.KeycloakAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ParticipantShapeController.class, WebSecurityConfig.class})
@AutoConfigureMockMvc()
class ParticipantShapeControllerTests {

    @MockBean
    private GXFSWizardRestService gxfsWizardRestService;

    @Autowired
    private JwtAuthConverter jwtAuthConverter;

    @MockBean
    private JwtAuthConverterProperties jwtAuthConverterProperties;

    @MockBean
    private KeycloakAuthService keycloakAuthService;

    @Autowired
    private MockMvc mvc;


    @BeforeEach
    public void beforeEach()  {
        lenient().when(gxfsWizardRestService.getMerlotParticipantShape()).thenReturn("shape");
    }

    @Test
    void getParticipantShapeUnauthenticated() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/shapes/merlotParticipant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getParticipantShapeAuthenticated() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/shapes/merlotParticipant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_OrgLegRep_20")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
