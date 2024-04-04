package eu.merloteducation.organisationsorchestrator;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.merloteducation.authorizationlibrary.authorization.*;
import eu.merloteducation.authorizationlibrary.config.InterceptorConfig;
import eu.merloteducation.gxfscataloglibrary.models.query.GXFSQueryLegalNameItem;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.GXFSCatalogListResponse;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescription;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescriptionVerifiableCredential;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gax.datatypes.RegistrationNumber;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gax.datatypes.TermsAndConditions;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gax.datatypes.VCard;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotOrganizationCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.service.GxfsCatalogService;
import eu.merloteducation.modelslib.api.organization.MembershipClass;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantMetaDto;
import eu.merloteducation.organisationsorchestrator.config.WebSecurityConfig;
import eu.merloteducation.organisationsorchestrator.controller.OrganizationQueryController;
import eu.merloteducation.organisationsorchestrator.mappers.PdfContentMapper;
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
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.mockito.Mockito.lenient;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({OrganizationQueryController.class, WebSecurityConfig.class, PdfContentMapper.class})
@Import({JwtAuthConverter.class, AuthorityChecker.class, InterceptorConfig.class, ActiveRoleHeaderHandlerInterceptor.class, AuthorityChecker.class})
@AutoConfigureMockMvc()
class OrganizationQueryControllerTests {

    @MockBean
    private ParticipantService participantService;

    @MockBean
    private GxfsCatalogService gxfsCatalogService;

    @MockBean
    private JwtAuthConverterProperties jwtAuthConverterProperties;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PdfContentMapper pdfContentMapper;


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
        participantDto.setId("did:web:example.com:participant:someid");
        participantDto.setMetadata(new MerlotParticipantMetaDto());
        participantDto.getMetadata().setMembershipClass(MembershipClass.PARTICIPANT);
        participants.add(participantDto);

        Page<MerlotParticipantDto> participantsPage = new PageImpl<>(participants);

        lenient().when(participantService.getParticipants(any(), any()))
                .thenReturn(participantsPage);
        lenient().when(participantService.getParticipantById(eq("10")))
                .thenReturn(participantDto);
        lenient().when(participantService.getParticipantById(eq("garbage")))
                .thenThrow(HttpClientErrorException.NotFound.class);
        lenient().when(participantService.updateParticipant(any(), any()))
                .thenReturn(participantDto);

        GXFSCatalogListResponse<GXFSQueryLegalNameItem> legalNameResponse = new GXFSCatalogListResponse<>();
        GXFSQueryLegalNameItem item = new GXFSQueryLegalNameItem();
        item.setLegalName("Some Orga");
        legalNameResponse.setTotalCount(1);
        legalNameResponse.setItems(List.of(item));
        lenient().when(gxfsCatalogService.getParticipantLegalNameByUri(any(), any())).thenReturn(legalNameResponse);
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

        MerlotParticipantDto participantDtoWithEdits = getMerlotParticipantDtoWithEdits();
        mvc.perform(MockMvcRequestBuilders
                        .put("/organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(participantDtoWithEdits))
                        .header("Active-Role", "OrgLegRep_did:web:someorga.example.com")
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_did:web:someorga.example.com"),
                                new SimpleGrantedAuthority("ROLE_some_other_role")
                        )))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void updateOrganizationAuthorizedAsFedAdminExistent() throws Exception {

        MerlotParticipantDto participantDtoWithEdits = getMerlotParticipantDtoWithEdits();
        mvc.perform(MockMvcRequestBuilders
                .put("/organization")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectAsJsonString(participantDtoWithEdits))
                .header("Active-Role", "FedAdmin_did:web:someorga.example.com")
                .with(csrf())
                .with(jwt().authorities(
                    new OrganizationRoleGrantedAuthority("OrgLegRep_did:web:someotherorga.example.com"),
                    new OrganizationRoleGrantedAuthority("FedAdmin_did:web:someorga.example.com"),
                    new SimpleGrantedAuthority("ROLE_some_other_role")
                )))
            .andDo(print())
            .andExpect(status().isOk());
    }

    @Test
    void updateOrganizationAuthorizedAsFedAdminExistentInconsistentId() throws Exception {

        MerlotParticipantDto participantDtoWithEdits = getMerlotParticipantDtoWithEdits();
        MerlotOrganizationCredentialSubject credentialSubject =
            (MerlotOrganizationCredentialSubject) participantDtoWithEdits.getSelfDescription().getVerifiableCredential()
                .getCredentialSubject();
        credentialSubject.setId("Participant:did:web:somethirdorga.example.com");

        mvc.perform(MockMvcRequestBuilders
                .put("/organization")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectAsJsonString(participantDtoWithEdits))
                .header("Active-Role", "OrgLegRep_did:web:someorga.example.com")
                .with(csrf())
                .with(jwt().authorities(
                    new OrganizationRoleGrantedAuthority("OrgLegRep_did:web:someotherorga.example.com"),
                    new OrganizationRoleGrantedAuthority("OrgLegRep_did:web:someorga.example.com"),
                    new SimpleGrantedAuthority("ROLE_some_other_role")
                )))
            .andDo(print())
            .andExpect(status().isForbidden());
    }

    @Test
    void updateOrganizationUnauthorizedExistent() throws Exception {

        MerlotParticipantDto participantDtoWithEdits = getMerlotParticipantDtoWithEdits();
        mvc.perform(MockMvcRequestBuilders
                        .put("/organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectAsJsonString(participantDtoWithEdits))
                        .header("Active-Role", "OrgLegRep_did:web:someotherorga.example.com")
                        .with(csrf())
                        .with(jwt().authorities(
                                new OrganizationRoleGrantedAuthority("OrgLegRep_did:web:someotherorga.example.com")
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
                .header("Active-Role", "FedAdmin_did:web:someorga.example.com")
                .accept(MediaType.APPLICATION_JSON)
                .with(csrf())
                .with(jwt().authorities(
                    new OrganizationRoleGrantedAuthority("FedAdmin_did:web:someorga.example.com")
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
                .header("Active-Role", "FedAdmin_did:web:someorga.example.com")
                .accept(MediaType.APPLICATION_JSON)
                .with(csrf())
                .with(jwt().authorities(
                    new OrganizationRoleGrantedAuthority("FedAdmin_did:web:someorga.example.com")
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
                .header("Active-Role", "FedAdmin_did:web:someorga.example.com")
                .with(csrf())
                .with(jwt().authorities(
                    new OrganizationRoleGrantedAuthority("FedAdmin_did:web:someorga.example.com")
                )))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(status().reason(startsWith("Invalid registration form file.")));
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

    @Test
    void getTrustedDidsUnauthenticatedTest() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                .get("/trustedDids")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .with(csrf()))
            .andDo(print())
            .andExpect(status().isOk());
    }

    private MerlotOrganizationCredentialSubject getTestEditedMerlotOrganizationCredentialSubject() {

        MerlotOrganizationCredentialSubject credentialSubject = new MerlotOrganizationCredentialSubject();
        credentialSubject.setId("did:web:someorga.example.com");
        RegistrationNumber registrationNumber = new RegistrationNumber();
        registrationNumber.setLocal("localRegNum");
        credentialSubject.setRegistrationNumber(registrationNumber);
        VCard address = new VCard();
        address.setStreetAddress("address");
        address.setLocality("Berlin");
        address.setCountryName("DE");
        address.setPostalCode("12345");
        credentialSubject.setLegalAddress(address);
        credentialSubject.setHeadquarterAddress(address);
        credentialSubject.setOrgaName("MyOrga");
        TermsAndConditions termsAndConditions = new TermsAndConditions();
        termsAndConditions.setContent("http://example.com");
        termsAndConditions.setHash("1234");
        credentialSubject.setTermsAndConditions(termsAndConditions);
        return credentialSubject;
    }

    private MerlotParticipantDto getMerlotParticipantDtoWithEdits() {

        MerlotParticipantDto dtoWithEdits = new MerlotParticipantDto();

        SelfDescription selfDescription = new SelfDescription();
        SelfDescriptionVerifiableCredential verifiableCredential = new SelfDescriptionVerifiableCredential();
        MerlotOrganizationCredentialSubject editedCredentialSubject = getTestEditedMerlotOrganizationCredentialSubject();

        verifiableCredential.setCredentialSubject(editedCredentialSubject);
        selfDescription.setVerifiableCredential(verifiableCredential);

        MerlotParticipantMetaDto metaData = new MerlotParticipantMetaDto();
        metaData.setOrgaId("did:web:someorga.example.com");
        metaData.setMailAddress("me@mail.me");
        metaData.setMembershipClass(MembershipClass.FEDERATOR);

        dtoWithEdits.setSelfDescription(selfDescription);
        dtoWithEdits.setId(editedCredentialSubject.getId());
        dtoWithEdits.setMetadata(metaData);
        return dtoWithEdits;
    }
}