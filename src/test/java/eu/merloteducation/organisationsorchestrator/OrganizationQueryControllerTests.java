package eu.merloteducation.organisationsorchestrator;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.merloteducation.authorizationlibrary.authorization.*;
import eu.merloteducation.authorizationlibrary.config.InterceptorConfig;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gax.datatypes.RegistrationNumber;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gax.datatypes.StringTypeValue;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gax.datatypes.TermsAndConditions;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gax.datatypes.VCard;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotOrganizationCredentialSubject;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.organisationsorchestrator.config.WebSecurityConfig;
import eu.merloteducation.organisationsorchestrator.controller.OrganizationQueryController;
import eu.merloteducation.organisationsorchestrator.service.ParticipantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({OrganizationQueryController.class, WebSecurityConfig.class})
@Import({JwtAuthConverter.class, AuthorityChecker.class, InterceptorConfig.class, ActiveRoleHeaderHandlerInterceptor.class, AuthorityChecker.class})
@AutoConfigureMockMvc()
class OrganizationQueryControllerTests {

    @MockBean
    private ParticipantService participantService;

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

        lenient().when(participantService.getParticipants(any()))
                .thenReturn(participantsPage);
        lenient().when(participantService.getParticipantById(eq("10")))
                .thenReturn(participantDto);
        lenient().when(participantService.getParticipantById(eq("garbage")))
                .thenThrow(HttpClientErrorException.NotFound.class);
        lenient().when(participantService.updateParticipant(any(), any(), eq("10")))
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

        MerlotOrganizationCredentialSubject credentialSubject = getTestEditedMerlotOrganizationCredentialSubject();
        mvc.perform(MockMvcRequestBuilders
                        .put("/organization/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(credentialSubject))
                        .header("Active-Role", "OrgLegRep_10")
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_10"),
                                new SimpleGrantedAuthority("ROLE_some_other_role")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void updateOrganizationAuthorizedAsFedAdminExistent() throws Exception {

        MerlotOrganizationCredentialSubject credentialSubject = getTestEditedMerlotOrganizationCredentialSubject();
        mvc.perform(MockMvcRequestBuilders
                .put("/organization/10")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectAsJsonString(credentialSubject))
                .header("Active-Role", "FedAdmin_10")
                .with(csrf())
                .with(jwt().authorities(
                    new OrganizationRoleGrantedAuthority("OrgLegRep_20"),
                    new OrganizationRoleGrantedAuthority("FedAdmin_10"),
                    new SimpleGrantedAuthority("ROLE_some_other_role")
                )))
            .andDo(print())
            .andExpect(status().isOk());
    }

    @Test
    void updateOrganizationAuthorizedAsFedAdminExistentInconsistentId() throws Exception {

        MerlotOrganizationCredentialSubject credentialSubject = getTestEditedMerlotOrganizationCredentialSubject();
        credentialSubject.setMerlotId(new StringTypeValue("30"));
        credentialSubject.setId("Participant:30");

        mvc.perform(MockMvcRequestBuilders
                .put("/organization/10")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectAsJsonString(credentialSubject))
                .header("Active-Role", "OrgLegRep_10")
                .with(csrf())
                .with(jwt().authorities(
                    new OrganizationRoleGrantedAuthority("OrgLegRep_20"),
                    new OrganizationRoleGrantedAuthority("OrgLegRep_10"),
                    new SimpleGrantedAuthority("ROLE_some_other_role")
                )))
            .andDo(print())
            .andExpect(status().isForbidden());
    }

    @Test
    void updateOrganizationUnauthorizedExistent() throws Exception {

        MerlotOrganizationCredentialSubject credentialSubject = getTestEditedMerlotOrganizationCredentialSubject();
        mvc.perform(MockMvcRequestBuilders
                        .put("/organization/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(credentialSubject))
                        .header("Active-Role", "OrgLegRep_20")
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_20")
                        )))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllFederatorsUnauthenticatedTest() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                .get("/federators")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .with(csrf()))
            .andDo(print())
            .andExpect(status().isOk());
    }

    @Test
    void createOrganizationAuthorized() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile("file", "test.pdf",
            MediaType.APPLICATION_PDF_VALUE, Base64.getDecoder()
            .decode(("JVBERi0xLjIgCjkgMCBvYmoKPDwKPj4Kc3RyZWFtCkJULyA5IFRmKFRlc3QpJyBFVAplbmRzdHJlYW0KZW" +
                "5kb2JqCjQgMCBvYmoKPDwKL1R5cGUgL1BhZ2UKL1BhcmVudCA1IDAgUgovQ29udGVudHMgOSAwIFIKPj4KZ" +
                "W5kb2JqCjUgMCBvYmoKPDwKL0tpZHMgWzQgMCBSIF0KL0NvdW50IDEKL1R5cGUgL1BhZ2VzCi9NZWRpYUJv" +
                "eCBbIDAgMCA5OSA5IF0KPj4KZW5kb2JqCjMgMCBvYmoKPDwKL1BhZ2VzIDUgMCBSCi9UeXBlIC9DYXRhbG9n" +
                "Cj4+CmVuZG9iagp0cmFpbGVyCjw8Ci9Sb290IDMgMCBSCj4+CiUlRU9G")
                .getBytes(StandardCharsets.UTF_8)));
        mvc.perform(multipart(HttpMethod.POST, "/organization")
                .file(multipartFile)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "")
                .header("Active-Role", "FedAdmin_10")
                .accept(MediaType.APPLICATION_JSON)
                .with(csrf())
                .with(jwt().authorities(
                    new OrganizationRoleGrantedAuthority("FedAdmin_10")
                )))
            .andDo(print())
            .andExpect(status().isOk());
    }

    @Test
    void createOrganizationAuthorizedMultipleFiles() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile("file", "test.pdf",
            MediaType.APPLICATION_PDF_VALUE, Base64.getDecoder()
            .decode(("JVBERi0xLjIgCjkgMCBvYmoKPDwKPj4Kc3RyZWFtCkJULyA5IFRmKFRlc3QpJyBFVAplbmRzdHJlYW0KZW" +
                "5kb2JqCjQgMCBvYmoKPDwKL1R5cGUgL1BhZ2UKL1BhcmVudCA1IDAgUgovQ29udGVudHMgOSAwIFIKPj4KZ" +
                "W5kb2JqCjUgMCBvYmoKPDwKL0tpZHMgWzQgMCBSIF0KL0NvdW50IDEKL1R5cGUgL1BhZ2VzCi9NZWRpYUJv" +
                "eCBbIDAgMCA5OSA5IF0KPj4KZW5kb2JqCjMgMCBvYmoKPDwKL1BhZ2VzIDUgMCBSCi9UeXBlIC9DYXRhbG9n" +
                "Cj4+CmVuZG9iagp0cmFpbGVyCjw8Ci9Sb290IDMgMCBSCj4+CiUlRU9G")
                .getBytes(StandardCharsets.UTF_8)));
        mvc.perform(multipart(HttpMethod.POST, "/organization")
                .file(multipartFile)
                .file(multipartFile)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "")
                .header("Active-Role", "FedAdmin_10")
                .accept(MediaType.APPLICATION_JSON)
                .with(csrf())
                .with(jwt().authorities(
                    new OrganizationRoleGrantedAuthority("FedAdmin_10")
                )))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Too many files specified"));
    }

    @Test
    void createOrganizationAuthorizedTextFile() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile("file", "test.txt",
            "text/plain", "It's not a PDF!".getBytes());
        mvc.perform(multipart(HttpMethod.POST, "/organization")
                .file(multipartFile)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "")
                .accept(MediaType.APPLICATION_JSON)
                .header("Active-Role", "FedAdmin_10")
                .with(csrf())
                .with(jwt().authorities(
                    new OrganizationRoleGrantedAuthority("FedAdmin_10")
                )))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Invalid registration form file."));
    }
    @Test
    void createOrganizationUnauthorized() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile("file", "test.pdf",
            MediaType.APPLICATION_PDF_VALUE, Base64.getDecoder()
            .decode(("JVBERi0xLjIgCjkgMCBvYmoKPDwKPj4Kc3RyZWFtCkJULyA5IFRmKFRlc3QpJyBFVAplbmRzdHJlYW0KZW" +
                "5kb2JqCjQgMCBvYmoKPDwKL1R5cGUgL1BhZ2UKL1BhcmVudCA1IDAgUgovQ29udGVudHMgOSAwIFIKPj4KZ" +
                "W5kb2JqCjUgMCBvYmoKPDwKL0tpZHMgWzQgMCBSIF0KL0NvdW50IDEKL1R5cGUgL1BhZ2VzCi9NZWRpYUJv" +
                "eCBbIDAgMCA5OSA5IF0KPj4KZW5kb2JqCjMgMCBvYmoKPDwKL1BhZ2VzIDUgMCBSCi9UeXBlIC9DYXRhbG9n" +
                "Cj4+CmVuZG9iagp0cmFpbGVyCjw8Ci9Sb290IDMgMCBSCj4+CiUlRU9G")
                .getBytes(StandardCharsets.UTF_8)));
        mvc.perform(multipart(HttpMethod.POST, "/organization")
                .file(multipartFile)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "")
                .accept(MediaType.APPLICATION_JSON)
                .with(csrf()))
            .andDo(print())
            .andExpect(status().isUnauthorized());
    }

    private MerlotOrganizationCredentialSubject getTestEditedMerlotOrganizationCredentialSubject() {

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
        credentialSubject.setOrgaName(new StringTypeValue("MyOrga"));
        credentialSubject.setMerlotId(new StringTypeValue("10"));
        credentialSubject.setMailAddress(new StringTypeValue("me@mail.me"));
        TermsAndConditions termsAndConditions = new TermsAndConditions();
        termsAndConditions.setContent(new StringTypeValue("http://example.com"));
        termsAndConditions.setHash(new StringTypeValue("1234"));
        credentialSubject.setTermsAndConditions(termsAndConditions);
        return credentialSubject;
    }
}