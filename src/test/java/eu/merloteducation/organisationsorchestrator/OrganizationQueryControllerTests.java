package eu.merloteducation.organisationsorchestrator;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.merloteducation.organisationsorchestrator.auth.AuthorityChecker;
import eu.merloteducation.organisationsorchestrator.auth.JwtAuthConverter;
import eu.merloteducation.organisationsorchestrator.auth.JwtAuthConverterProperties;
import eu.merloteducation.organisationsorchestrator.auth.OrganizationRoleGrantedAuthority;
import eu.merloteducation.organisationsorchestrator.config.WebSecurityConfig;
import eu.merloteducation.organisationsorchestrator.controller.OrganizationQueryController;
import eu.merloteducation.organisationsorchestrator.models.dto.MerlotParticipantDto;
import eu.merloteducation.organisationsorchestrator.models.gxfscatalog.*;
import eu.merloteducation.organisationsorchestrator.service.GXFSCatalogRestService;
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

@WebMvcTest({OrganizationQueryController.class, WebSecurityConfig.class, AuthorityChecker.class})
@AutoConfigureMockMvc()
class OrganizationQueryControllerTests {

    @MockBean
    private GXFSCatalogRestService gxfsCatalogRestService;

    @Autowired
    private JwtAuthConverter jwtAuthConverter;

    @MockBean
    private JwtAuthConverterProperties jwtAuthConverterProperties;

    @Autowired
    private MockMvc mvc;

    private String objectAsJsonString(final Object obj) {
        try {
            return JsonMapper.builder()
                    .addModule(new JavaTimeModule())
                    .build().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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
        lenient().when(gxfsCatalogRestService.updateParticipant(any(), eq("10")))
                .thenReturn(participantDto);

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
    void getOrganisationByIdUnauthenticatedTest() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/organization/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void getOrganisationByIdNonExistentTest() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/organization/garbage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void updateOrganizationAuthorizedExistent() throws Exception {
        MerlotOrganizationCredentialSubject credentialSubject = new MerlotOrganizationCredentialSubject();
        credentialSubject.setId("Participant:10");
        RegistrationNumber registrationNumber = new RegistrationNumber();
        registrationNumber.setLocal(new StringTypeValue("localRegNum"));
        credentialSubject.setRegistrationNumber(registrationNumber);
        VCard address = new VCard();
        address.setStreetAddress(new StringTypeValue("address"));
        address.setLocality(new StringTypeValue("Berlin"));
        address.setCountryName(new StringTypeValue("DE"));
        address.setPostalCode(new StringTypeValue("12345"));
        credentialSubject.setLegalAddress(address);
        credentialSubject.setHeadquarterAddress(address);
        credentialSubject.setAddressCode(new StringTypeValue("DE-BER"));
        credentialSubject.setOrgaName(new StringTypeValue("MyOrga"));
        credentialSubject.setMerlotId(new StringTypeValue("10"));
        credentialSubject.setMailAddress(new StringTypeValue("me@mail.me"));
        TermsAndConditions termsAndConditions = new TermsAndConditions();
        termsAndConditions.setContent(new StringTypeValue("http://example.com"));
        termsAndConditions.setHash(new StringTypeValue("1234"));
        credentialSubject.setTermsAndConditions(termsAndConditions);
        mvc.perform(MockMvcRequestBuilders
                        .put("/organization/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(credentialSubject))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void updateOrganizationUnauthorizedExistent() throws Exception {
        MerlotOrganizationCredentialSubject credentialSubject = new MerlotOrganizationCredentialSubject();
        credentialSubject.setId("Participant:10");
        RegistrationNumber registrationNumber = new RegistrationNumber();
        registrationNumber.setLocal(new StringTypeValue("localRegNum"));
        credentialSubject.setRegistrationNumber(registrationNumber);
        VCard address = new VCard();
        address.setStreetAddress(new StringTypeValue("address"));
        address.setLocality(new StringTypeValue("Berlin"));
        address.setCountryName(new StringTypeValue("DE"));
        address.setPostalCode(new StringTypeValue("12345"));
        credentialSubject.setLegalAddress(address);
        credentialSubject.setHeadquarterAddress(address);
        credentialSubject.setAddressCode(new StringTypeValue("DE-BER"));
        credentialSubject.setOrgaName(new StringTypeValue("MyOrga"));
        credentialSubject.setMerlotId(new StringTypeValue("10"));
        credentialSubject.setMailAddress(new StringTypeValue("me@mail.me"));
        TermsAndConditions termsAndConditions = new TermsAndConditions();
        termsAndConditions.setContent(new StringTypeValue("http://example.com"));
        termsAndConditions.setHash(new StringTypeValue("1234"));
        credentialSubject.setTermsAndConditions(termsAndConditions);
        mvc.perform(MockMvcRequestBuilders
                        .put("/organization/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(credentialSubject))
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_20")
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}
