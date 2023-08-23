package eu.merloteducation.organisationsorchestrator;

import eu.merloteducation.organisationsorchestrator.config.JwtAuthConverter;
import eu.merloteducation.organisationsorchestrator.config.JwtAuthConverterProperties;
import eu.merloteducation.organisationsorchestrator.config.WebSecurityConfig;
import eu.merloteducation.organisationsorchestrator.controller.OrganizationQueryController;
import eu.merloteducation.organisationsorchestrator.models.dto.MerlotParticipantDto;
import eu.merloteducation.organisationsorchestrator.service.GXFSCatalogRestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
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

@WebMvcTest({OrganizationQueryController.class, WebSecurityConfig.class})
@AutoConfigureMockMvc()
public class OrganizationQueryControllerTests {

    @MockBean
    private GXFSCatalogRestService gxfsCatalogRestService;

    @Autowired
    private JwtAuthConverter jwtAuthConverter;

    @MockBean
    private JwtAuthConverterProperties jwtAuthConverterProperties;

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    public void beforeEach() throws Exception {
        List<MerlotParticipantDto> participants = new ArrayList<>();
        MerlotParticipantDto participantDto = new MerlotParticipantDto();
        participants.add(participantDto);

        Page<MerlotParticipantDto> participantsPage = new PageImpl<>(participants);

        lenient().when(gxfsCatalogRestService.getParticipants(any()))
                .thenReturn(participantsPage);
        lenient().when(gxfsCatalogRestService.getParticipantById(eq("10")))
                .thenReturn(participantDto);
        lenient().when(gxfsCatalogRestService.getParticipantById(eq("garbage")))
                .thenThrow(HttpClientErrorException.NotFound.class);
    }

    @Test
    void getAllOrganisationsUnauthenticatedTest() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void getOrganisationByIdAuthenticatedTest() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/organization/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void getOrganisationByIdNonExistentTest() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/organization/garbage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

}
