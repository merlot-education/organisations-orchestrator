/*
 *  Copyright 2024 Dataport. All rights reserved. Developed as part of the MERLOT project.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.merloteducation.organisationsorchestrator;

import com.danubetech.verifiablecredentials.VerifiableCredential;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.authorizationlibrary.authorization.OrganizationRole;
import eu.merloteducation.authorizationlibrary.authorization.OrganizationRoleGrantedAuthority;
import eu.merloteducation.gxfscataloglibrary.models.credentials.CastableCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.credentials.ExtendedVerifiableCredential;
import eu.merloteducation.gxfscataloglibrary.models.credentials.ExtendedVerifiablePresentation;
import eu.merloteducation.gxfscataloglibrary.models.exception.CredentialPresentationException;
import eu.merloteducation.gxfscataloglibrary.models.exception.CredentialSignatureException;
import eu.merloteducation.gxfscataloglibrary.models.participants.ParticipantItem;
import eu.merloteducation.gxfscataloglibrary.models.query.GXFSQueryLegalNameItem;
import eu.merloteducation.gxfscataloglibrary.models.query.GXFSQueryUriItem;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.GXFSCatalogListResponse;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.PojoCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescriptionItem;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescriptionMeta;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.datatypes.GxVcard;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.datatypes.NodeKindIRITypeId;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.participants.GxLegalParticipantCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.participants.GxLegalRegistrationNumberCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.ParticipantTermsAndConditions;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotLegalParticipantCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.service.GxfsCatalogService;
import eu.merloteducation.modelslib.api.did.ParticipantDidPrivateKeyCreateRequest;
import eu.merloteducation.modelslib.api.organization.*;
import eu.merloteducation.organisationsorchestrator.config.InitialDataLoader;
import eu.merloteducation.organisationsorchestrator.mappers.OrganizationMapper;
import eu.merloteducation.organisationsorchestrator.models.RegistrationFormContent;
import eu.merloteducation.organisationsorchestrator.models.entities.OcmAgentSettings;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganizationMetadata;
import eu.merloteducation.organisationsorchestrator.service.OmejdnConnectorApiClient;
import eu.merloteducation.organisationsorchestrator.service.OrganizationMetadataService;
import eu.merloteducation.organisationsorchestrator.service.OutgoingMessageService;
import eu.merloteducation.organisationsorchestrator.service.ParticipantService;
import org.apache.commons.text.StringEscapeUtils;

import static eu.merloteducation.organisationsorchestrator.SelfDescriptionDemoData.*;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@EnableConfigurationProperties
class ParticipantServiceTests {

    @Autowired
    private OrganizationMapper organizationMapper;

    @Autowired
    private ParticipantService participantService;

    @MockBean
    private GxfsCatalogService gxfsCatalogService;

    @MockBean
    OrganizationMetadataService organizationMetadataService;

    @MockBean
    private InitialDataLoader initialDataLoader;

    @MockBean
    private OutgoingMessageService outgoingMessageService;

    @MockBean
    private OmejdnConnectorApiClient omejdnConnectorApiClient;

    private final MerlotDidServiceClientFake merlotDidServiceClientFake = new MerlotDidServiceClientFake();

    String mailAddress = "test@test.de";

    String organizationLegalName = "MyOrga";
    String organizationLegalForm = "LLC";

    String registrationNumber = "0110";

    String countryCode = "DE";

    String countrySubdivisionCode = "DE-BE";

    String street = "Some Street 3";

    String providerTncHash = "1234";

    String providerTncLink = "http://example.com";

    String city = "Berlin";

    String organizationName = "MyOrga";

    String postalCode = "12345";

    private final String merlotDomain = "example.com";
    String id = "did:web:" + merlotDomain + ":participant:someorga";

    ParticipantServiceTests() throws IOException {
    }

    RegistrationFormContent getTestRegistrationFormContent() throws IOException {

        RegistrationFormContent content = new RegistrationFormContent();
        content.setOrganizationName(organizationName);
        content.setOrganizationLegalName(organizationLegalName);
        content.setLegalForm(organizationLegalForm);
        content.setMailAddress(mailAddress);
        content.setRegistrationNumberLeiCode(registrationNumber);
        content.setRegistrationNumberVatID("");
        content.setRegistrationNumberEori("");
        content.setCountryCode(countryCode);
        content.setCountrySubdivisionCode(countrySubdivisionCode);
        content.setPostalCode(postalCode);
        content.setCity(city);
        content.setStreet(street);
        content.setProviderTncLink(providerTncLink);
        content.setProviderTncHash(providerTncHash);

        return content;
    }

    private ParticipantItem createMockParticipantItem() throws JsonProcessingException {
        GxLegalParticipantCredentialSubject gxParticipantCs = getGxParticipantCs(id);
        GxLegalRegistrationNumberCredentialSubject gxRegistrationNumberCs = getGxRegistrationNumberCs(id);
        MerlotLegalParticipantCredentialSubject merlotParticipantCs = getMerlotParticipantCs(id);

        return wrapCredentialSubjectInItem(List.of(gxParticipantCs, gxRegistrationNumberCs, merlotParticipantCs));
    }

    private GXFSCatalogListResponse<SelfDescriptionItem> createMockSdItems() throws JsonProcessingException {
        GxLegalParticipantCredentialSubject gxParticipantCs = getGxParticipantCs(id);
        GxLegalRegistrationNumberCredentialSubject gxRegistrationNumberCs = getGxRegistrationNumberCs(id);
        MerlotLegalParticipantCredentialSubject merlotParticipantCs = getMerlotParticipantCs(id);

        return wrapCredentialSubjectInSdResponse(List.of(gxParticipantCs, gxRegistrationNumberCs, merlotParticipantCs));
    }

    private GXFSCatalogListResponse<SelfDescriptionItem> wrapCredentialSubjectInSdResponse(List<PojoCredentialSubject> csList) throws JsonProcessingException {
        SelfDescriptionItem item = new SelfDescriptionItem();
        SelfDescriptionMeta meta = new SelfDescriptionMeta();
        meta.setContent(createVpFromCsList(csList, "did:web:someorga"));
        meta.setId("did:web:example.com:participant:someorga");
        meta.setSubjectId("did:web:example.com:participant:someorga");
        meta.setSdHash("8b143ff8e0cf8f22c366cea9e1d31d97f79aa29eee5741f048637a43b7f059b0");
        meta.setStatus("active");
        meta.setIssuer("did:web:example.com:participant:someorga");
        item.setMeta(meta);

        GXFSCatalogListResponse<SelfDescriptionItem> response = new GXFSCatalogListResponse<>();
        response.setTotalCount(1);
        response.setItems(List.of(item));
        return response;
    }

    private ParticipantItem wrapCredentialSubjectInItem(List<PojoCredentialSubject> csList) throws JsonProcessingException {
        ParticipantItem item = new ParticipantItem();
        ExtendedVerifiablePresentation vp = createVpFromCsList(csList, "did:web:someorga");
        MerlotLegalParticipantCredentialSubject merlotCs = vp
                .findFirstCredentialSubjectByType(MerlotLegalParticipantCredentialSubject.class);
        item.setSelfDescription(vp);
        item.setId(merlotCs.getId());
        item.setName(merlotCs.getLegalName());
        return item;
    }

    private MerlotParticipantMetaDto getTestMerlotParticipantMetaDto() {

        MerlotParticipantMetaDto metaDto = new MerlotParticipantMetaDto();
        metaDto.setOrgaId("did:web:example.com:participant:someorga");
        metaDto.setMailAddress("mymail@example.com");
        metaDto.setMembershipClass(MembershipClass.PARTICIPANT);
        metaDto.setActive(true);
        OrganisationSignerConfigDto signerConfigDto = new OrganisationSignerConfigDto();
        signerConfigDto.setPrivateKey("privateKey");
        signerConfigDto.setMerlotVerificationMethod("did:web:example.com:participant:someorga#merlot");
        signerConfigDto.setVerificationMethod("did:web:example.com:participant:someorga#somemethod");
        metaDto.setOrganisationSignerConfigDto(signerConfigDto);
        ParticipantAgentSettingsDto settings = new ParticipantAgentSettingsDto();
        settings.setAgentDid("123456");
        metaDto.setOcmAgentSettings(Set.of(settings));
        return metaDto;
    }

    @BeforeEach
    public void setUp() throws IOException, CredentialSignatureException, CredentialPresentationException {
        ObjectMapper mapper = new ObjectMapper();
        ReflectionTestUtils.setField(participantService, "organizationMapper", organizationMapper);
        ReflectionTestUtils.setField(participantService, "gxfsCatalogService", gxfsCatalogService);
        ReflectionTestUtils.setField(participantService, "organizationMetadataService", organizationMetadataService);
        ReflectionTestUtils.setField(participantService, "outgoingMessageService", outgoingMessageService);
        ReflectionTestUtils.setField(participantService, "omejdnConnectorApiClient", new OmejdnConnectorApiClientFake());

        ParticipantItem participantItem = createMockParticipantItem();

        GXFSCatalogListResponse<SelfDescriptionItem> sdItems = createMockSdItems();

        GXFSQueryUriItem gxfsQueryUriItem = new GXFSQueryUriItem();
        gxfsQueryUriItem.setUri("did:web:example.com:participant:someorga");
        GXFSCatalogListResponse<GXFSQueryUriItem> uriItems = new GXFSCatalogListResponse<>();
        uriItems.setTotalCount(1);
        uriItems.setItems(List.of(gxfsQueryUriItem));

        lenient().when(gxfsCatalogService.getSortedParticipantUriPage(any(), any(), anyLong(), anyLong()))
            .thenReturn(uriItems);
        lenient().when(gxfsCatalogService.getSelfDescriptionsByIds(any()))
            .thenReturn(sdItems);
        lenient().when(gxfsCatalogService.getSelfDescriptionsByIds(any(), any()))
            .thenReturn(sdItems);
        lenient().when(gxfsCatalogService.getParticipantById(eq("did:web:example.com:participant:someorga")))
            .thenReturn(participantItem);
        lenient().when(gxfsCatalogService.getParticipantById(eq("did:web:example.com:participant:nosignerconfig")))
            .thenReturn(participantItem);
        lenient().when(gxfsCatalogService.getParticipantById(eq("did:web:example.com:participant:emptysignerconfig")))
            .thenReturn(participantItem);
        lenient().when(gxfsCatalogService.updateParticipant(any(), any()))
            .thenAnswer(i -> wrapCredentialSubjectInItem((List<PojoCredentialSubject>) i.getArguments()[0]));
        lenient().when(gxfsCatalogService.addParticipant(any(), any()))
            .thenAnswer(i -> wrapCredentialSubjectInItem((List<PojoCredentialSubject>) i.getArguments()[0]));
        lenient().when(gxfsCatalogService.getParticipantLegalNameByUri(eq("MerlotOrganization"), any()))
            .thenReturn(new GXFSCatalogListResponse<>());

        MerlotParticipantMetaDto metaDto = getTestMerlotParticipantMetaDto();

        MerlotParticipantMetaDto metaDtoNoSignerConfig = getTestMerlotParticipantMetaDto();
        metaDtoNoSignerConfig.setOrganisationSignerConfigDto(null);

        MerlotParticipantMetaDto metaDtoEmptySignerConfig = getTestMerlotParticipantMetaDto();
        metaDtoEmptySignerConfig.setOrganisationSignerConfigDto(new OrganisationSignerConfigDto());

        lenient().when(organizationMetadataService.getMerlotParticipantMetaDto(eq("did:web:example.com:participant:nosignerconfig"))).thenReturn(metaDtoNoSignerConfig);
        lenient().when(organizationMetadataService.getMerlotParticipantMetaDto(eq("did:web:example.com:participant:emptysignerconfig"))).thenReturn(metaDtoEmptySignerConfig);
        lenient().when(organizationMetadataService.getMerlotParticipantMetaDto(eq("did:web:example.com:participant:someorga"))).thenReturn(metaDto);
        lenient().when(organizationMetadataService.getMerlotParticipantMetaDto(eq("did:web:example.com:participant:somefedorga"))).thenReturn(metaDto);
        lenient().when(organizationMetadataService.getParticipantsByMembershipClass(eq(MembershipClass.FEDERATOR))).thenReturn(new ArrayList<>());
        lenient().when(organizationMetadataService.updateMerlotParticipantMeta(any())).thenAnswer(i -> i.getArguments()[0]);
        lenient().when(organizationMetadataService.getInactiveParticipantsIds()).thenReturn(new ArrayList<>());
        lenient().when(organizationMetadataService.saveMerlotParticipantMeta(any())).thenReturn(metaDto);

        lenient().when(outgoingMessageService.requestNewDidPrivateKey(any())).thenReturn(
                merlotDidServiceClientFake.generateDidAndPrivateKey(new ParticipantDidPrivateKeyCreateRequest()));
    }

    @Test
    void getAllParticipantsAsFedAdmin() throws Exception {
        OrganizationRoleGrantedAuthority activeRole = new OrganizationRoleGrantedAuthority(OrganizationRole.FED_ADMIN.getRoleName() + "_anything");

        Page<MerlotParticipantDto> organizations = participantService.getParticipants(PageRequest.of(0, 9), activeRole);
        assertThat(organizations.getContent(), isA(List.class));
        assertThat(organizations.getContent(), not(empty()));
        assertEquals(1, organizations.getContent().size());

        String id = organizations.getContent().get(0).getSelfDescription()
                .findFirstCredentialSubjectByType(GxLegalParticipantCredentialSubject.class).getId();
        assertEquals("did:web:example.com:participant:someorga", id);

        String mailAddress = organizations.getContent().get(0).getMetadata().getMailAddress();
        assertEquals("mymail@example.com", mailAddress);

        MembershipClass membershipClass = organizations.getContent().get(0).getMetadata().getMembershipClass();
        assertEquals(MembershipClass.PARTICIPANT, membershipClass);

        assertEquals(0,  organizations.getContent().get(0).getMetadata().getConnectors().size());
        assertNull(organizations.getContent().get(0).getMetadata().getSignedBy());
    }

    @Test
    void getAllParticipantsNotAsFedAdmin() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String mockQueryResponse = """
            {
                "totalCount": 0,
                "items": []
            }
            """;
        mockQueryResponse = StringEscapeUtils.unescapeJson(mockQueryResponse);
        if (mockQueryResponse != null)
            mockQueryResponse = mockQueryResponse.replace("\"{", "{").replace("}\"", "}");
        GXFSCatalogListResponse<GXFSQueryUriItem> uriItems = mapper.readValue(mockQueryResponse, new TypeReference<>() {});

        lenient().when(gxfsCatalogService.getSortedParticipantUriPageWithExcludedUris(any(), any(), any(), anyLong(), anyLong()))
            .thenReturn(uriItems);

        lenient().when(organizationMetadataService.getInactiveParticipantsIds()).thenReturn(List.of("did:web:example.com:participant:someorga"));

        OrganizationRoleGrantedAuthority activeRole = new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP.getRoleName() + "_anything");
        Page<MerlotParticipantDto> participants = participantService.getParticipants(PageRequest.of(0, 9), activeRole);
        assertThat(participants).isEmpty();

        verify(organizationMetadataService, times(1)).getInactiveParticipantsIds();
        verify(gxfsCatalogService, times(1)).getSortedParticipantUriPageWithExcludedUris(any(), any(), eq(List.of("did:web:example.com:participant:someorga")), anyLong(), anyLong());
    }

    @Test
    void getAllParticipantsFailAtSdUri() throws Exception {
        OrganizationRoleGrantedAuthority activeRole = new OrganizationRoleGrantedAuthority(OrganizationRole.FED_ADMIN.getRoleName() + "_anything");
        doThrow(getWebClientResponseException()).when(gxfsCatalogService).getSelfDescriptionsByIds(any());

        PageRequest pageRequest = PageRequest.of(0, 9);
        assertThrows(ResponseStatusException.class, () -> participantService.getParticipants(pageRequest, activeRole));
    }

    @Test
    void getAllParticipantsFailAtQueryUri() throws Exception {
        OrganizationRoleGrantedAuthority activeRole = new OrganizationRoleGrantedAuthority(OrganizationRole.FED_ADMIN.getRoleName() + "_anything");
        doThrow(getWebClientResponseException()).when(gxfsCatalogService)
                .getSortedParticipantUriPage(any(), any(), anyLong(), anyLong());

        PageRequest pageRequest = PageRequest.of(0, 9);
        assertThrows(ResponseStatusException.class, () -> participantService.getParticipants(pageRequest, activeRole));
    }

    @Test
    void getParticipantById() throws Exception {
        GXFSCatalogListResponse<GXFSQueryLegalNameItem> legalNameItems = new GXFSCatalogListResponse<>();
        GXFSQueryLegalNameItem legalNameItem = new GXFSQueryLegalNameItem();
        legalNameItem.setLegalName("Some Orga");
        legalNameItems.setItems(List.of(legalNameItem));
        legalNameItems.setTotalCount(1);

        lenient().when(gxfsCatalogService.getParticipantLegalNameByUri(eq("MerlotOrganization"), eq("did:web:compliance.lab.gaia-x.eu")))
            .thenReturn(legalNameItems);

        MerlotParticipantDto organization = participantService.getParticipantById("did:web:example.com:participant:someorga");
        assertThat(organization, isA(MerlotParticipantDto.class));
        MerlotLegalParticipantCredentialSubject subject = organization.getSelfDescription()
                .findFirstCredentialSubjectByType(MerlotLegalParticipantCredentialSubject.class);
        assertEquals("did:web:example.com:participant:someorga", subject.getId());
        assertEquals(organizationLegalName, subject.getLegalName());

        String mailAddress = organization.getMetadata().getMailAddress();
        assertEquals("mymail@example.com", mailAddress);

        MembershipClass membershipClass = organization.getMetadata().getMembershipClass();
        assertEquals(MembershipClass.PARTICIPANT, membershipClass);

        assertEquals(0,  organization.getMetadata().getConnectors().size());
    }

    @Test
    void getParticipantByIdFail() {
        doThrow(getWebClientResponseException()).when(gxfsCatalogService).getParticipantById(any());

        assertThrows(ResponseStatusException.class, () -> participantService.getParticipantById("did:web:example.com:participant:someorga"));
    }

    @Test
    void getParticipantByInvalidId() {

        assertThrows(IllegalArgumentException.class, () -> participantService.getParticipantById("asdf"));
    }

    @Test
    void getParticipantByNonexistentId() {
        ResponseStatusException e =
                assertThrows(ResponseStatusException.class, () -> participantService.getParticipantById("did:web:example.com#someotherorga"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
    }

    @Test
    void updateParticipantExistentAsParticipant() throws Exception {

        MerlotParticipantDto participantDtoWithEdits = getMerlotParticipantDtoWithEdits();
        GxLegalParticipantCredentialSubject editedGxParticipantCs = participantDtoWithEdits.getSelfDescription()
                .findFirstCredentialSubjectByType(GxLegalParticipantCredentialSubject.class);
        GxLegalRegistrationNumberCredentialSubject editedGxRegistrationNumberCs = participantDtoWithEdits.getSelfDescription()
                .findFirstCredentialSubjectByType(GxLegalRegistrationNumberCredentialSubject.class);
        MerlotLegalParticipantCredentialSubject editedMerlotParticipantCs = participantDtoWithEdits.getSelfDescription()
                .findFirstCredentialSubjectByType(MerlotLegalParticipantCredentialSubject.class);
        MerlotParticipantMetaDto editedMetadata = participantDtoWithEdits.getMetadata();

        OrganizationRoleGrantedAuthority activeRole = new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, "did:web:example.com:participant:someorga");

        participantDtoWithEdits.setId("did:web:example.com:participant:someorga");
        MerlotParticipantDto updatedParticipantDto = participantService.updateParticipant(participantDtoWithEdits, activeRole);

        // following attributes of the organization credential subject should have been updated
        GxLegalParticipantCredentialSubject updatedGxParticipantCs = updatedParticipantDto.getSelfDescription()
                .findFirstCredentialSubjectByType(GxLegalParticipantCredentialSubject.class);
        GxLegalRegistrationNumberCredentialSubject updatedGxRegistrationNumberCs = updatedParticipantDto.getSelfDescription()
                .findFirstCredentialSubjectByType(GxLegalRegistrationNumberCredentialSubject.class);
        MerlotLegalParticipantCredentialSubject updatedMerlotParticipantCs = updatedParticipantDto.getSelfDescription()
                .findFirstCredentialSubjectByType(MerlotLegalParticipantCredentialSubject.class);

        assertEquals(updatedMerlotParticipantCs.getTermsAndConditions().getUrl(),
                editedMerlotParticipantCs.getTermsAndConditions().getUrl());
        assertEquals(updatedMerlotParticipantCs.getTermsAndConditions().getHash(),
                editedMerlotParticipantCs.getTermsAndConditions().getHash());
        assertEquals(updatedGxParticipantCs.getLegalAddress().getStreetAddress(),
                editedGxParticipantCs.getLegalAddress().getStreetAddress());
        assertEquals(updatedGxParticipantCs.getLegalAddress().getLocality(),
                editedGxParticipantCs.getLegalAddress().getLocality());
        assertEquals(updatedGxParticipantCs.getLegalAddress().getCountryCode(),
                editedGxParticipantCs.getLegalAddress().getCountryCode());
        assertEquals(updatedGxParticipantCs.getLegalAddress().getCountrySubdivisionCode(),
                editedGxParticipantCs.getLegalAddress().getCountrySubdivisionCode());
        assertEquals(updatedGxParticipantCs.getLegalAddress().getPostalCode(),
                editedGxParticipantCs.getLegalAddress().getPostalCode());

        // following attributes of the organization credential subject should not have been updated
        assertNotEquals(updatedGxParticipantCs.getId(), editedGxParticipantCs.getId());
        assertNotEquals(updatedGxParticipantCs.getName(),
                editedGxParticipantCs.getName());
        assertNotEquals(updatedMerlotParticipantCs.getLegalName(),
                editedMerlotParticipantCs.getLegalName());
        assertNotEquals(updatedGxRegistrationNumberCs.getLeiCode(), editedGxRegistrationNumberCs.getLeiCode());
        assertNotEquals(updatedGxRegistrationNumberCs.getEori(), editedGxRegistrationNumberCs.getEori());
        assertNotEquals(updatedGxRegistrationNumberCs.getEuid(), editedGxRegistrationNumberCs.getEuid());
        assertNotEquals(updatedGxRegistrationNumberCs.getTaxID(), editedGxRegistrationNumberCs.getTaxID());
        assertNotEquals(updatedGxRegistrationNumberCs.getVatID(), editedGxRegistrationNumberCs.getVatID());

        // following metadata of the organization should have been updated
        MerlotParticipantMetaDto updatedMetadata = updatedParticipantDto.getMetadata();
        assertEquals(updatedMetadata.getMailAddress(), editedMetadata.getMailAddress());
        assertEquals(1, updatedMetadata.getConnectors().size()); // connector was added

        // following metadata of the organization should not have been updated
        assertNotEquals(updatedMetadata.getMembershipClass(), editedMetadata.getMembershipClass());
        assertNotEquals(updatedMetadata.isActive(), editedMetadata.isActive());
        assertNotEquals(updatedMetadata.getOrganisationSignerConfigDto().getVerificationMethod(),
            editedMetadata.getOrganisationSignerConfigDto().getVerificationMethod());
        assertNotEquals(updatedMetadata.getOrganisationSignerConfigDto().getPrivateKey(),
            editedMetadata.getOrganisationSignerConfigDto().getPrivateKey());
        assertNotEquals(updatedMetadata.getOrganisationSignerConfigDto().getMerlotVerificationMethod(),
            editedMetadata.getOrganisationSignerConfigDto().getMerlotVerificationMethod());

        verify(outgoingMessageService, times(0)).sendOrganizationMembershipRevokedMessage(any());
    }

    @Test
    void updateParticipantExistentAsFedAdmin() throws Exception {

        MerlotParticipantDto dtoWithEdits = getMerlotParticipantDtoWithEdits();
        GxLegalParticipantCredentialSubject editedGxParticipantCs = dtoWithEdits.getSelfDescription()
                .findFirstCredentialSubjectByType(GxLegalParticipantCredentialSubject.class);
        GxLegalRegistrationNumberCredentialSubject editedGxRegistrationNumberCs = dtoWithEdits.getSelfDescription()
                .findFirstCredentialSubjectByType(GxLegalRegistrationNumberCredentialSubject.class);
        MerlotLegalParticipantCredentialSubject editedMerlotParticipantCs = dtoWithEdits.getSelfDescription()
                .findFirstCredentialSubjectByType(MerlotLegalParticipantCredentialSubject.class);
        MerlotParticipantMetaDto editedMetadata = dtoWithEdits.getMetadata();

        OrganizationRoleGrantedAuthority activeRole = new OrganizationRoleGrantedAuthority(OrganizationRole.FED_ADMIN, "did:web:example.com:participant:somefedorga");

        dtoWithEdits.setId("did:web:example.com:participant:someorga");
        MerlotParticipantDto participantDto = participantService.updateParticipant(dtoWithEdits, activeRole);

        // following attributes of the organization credential subject should have been updated
        GxLegalParticipantCredentialSubject updatedGxParticipantCs = participantDto.getSelfDescription()
                .findFirstCredentialSubjectByType(GxLegalParticipantCredentialSubject.class);
        GxLegalRegistrationNumberCredentialSubject updatedGxRegistrationNumberCs = participantDto.getSelfDescription()
                .findFirstCredentialSubjectByType(GxLegalRegistrationNumberCredentialSubject.class);
        MerlotLegalParticipantCredentialSubject updatedMerlotParticipantCs = participantDto.getSelfDescription()
                .findFirstCredentialSubjectByType(MerlotLegalParticipantCredentialSubject.class);

        assertEquals(updatedMerlotParticipantCs.getTermsAndConditions().getUrl(),
                editedMerlotParticipantCs.getTermsAndConditions().getUrl());
        assertEquals(updatedMerlotParticipantCs.getTermsAndConditions().getHash(),
                editedMerlotParticipantCs.getTermsAndConditions().getHash());
        assertEquals(updatedGxParticipantCs.getLegalAddress().getStreetAddress(),
                editedGxParticipantCs.getLegalAddress().getStreetAddress());
        assertEquals(updatedGxParticipantCs.getLegalAddress().getLocality(),
                editedGxParticipantCs.getLegalAddress().getLocality());
        assertEquals(updatedGxParticipantCs.getLegalAddress().getCountryCode(),
                editedGxParticipantCs.getLegalAddress().getCountryCode());
        assertEquals(updatedGxParticipantCs.getLegalAddress().getCountrySubdivisionCode(),
                editedGxParticipantCs.getLegalAddress().getCountrySubdivisionCode());
        assertEquals(updatedGxParticipantCs.getLegalAddress().getPostalCode(),
                editedGxParticipantCs.getLegalAddress().getPostalCode());
        assertEquals(updatedGxParticipantCs.getName(),
                editedGxParticipantCs.getName());
        assertEquals(updatedMerlotParticipantCs.getLegalName(),
                editedMerlotParticipantCs.getLegalName());
        assertEquals(updatedGxRegistrationNumberCs.getLeiCode(), editedGxRegistrationNumberCs.getLeiCode());
        assertEquals(updatedGxRegistrationNumberCs.getEori(), editedGxRegistrationNumberCs.getEori());
        assertEquals(updatedGxRegistrationNumberCs.getEuid(), editedGxRegistrationNumberCs.getEuid());
        assertEquals(updatedGxRegistrationNumberCs.getTaxID(), editedGxRegistrationNumberCs.getTaxID());
        assertEquals(updatedGxRegistrationNumberCs.getVatID(), editedGxRegistrationNumberCs.getVatID());

        // following attributes of the organization credential subject should not have been updated
        assertNotEquals(updatedGxParticipantCs.getId(), editedGxParticipantCs.getId());

        // following metadata of the organization should have been updated
        MerlotParticipantMetaDto updatedMetadata = participantDto.getMetadata();
        assertEquals(updatedMetadata.getMailAddress(), editedMetadata.getMailAddress());
        assertEquals(updatedMetadata.getMembershipClass(), editedMetadata.getMembershipClass());
        assertEquals(updatedMetadata.isActive(), editedMetadata.isActive());

        // following metadata of the organization should not have been updated
        assertEquals(0, updatedMetadata.getConnectors().size()); // no connector was added
        assertNotEquals(updatedMetadata.getOrganisationSignerConfigDto().getVerificationMethod(),
            editedMetadata.getOrganisationSignerConfigDto().getVerificationMethod());
        assertNotEquals(updatedMetadata.getOrganisationSignerConfigDto().getPrivateKey(),
            editedMetadata.getOrganisationSignerConfigDto().getPrivateKey());
        assertNotEquals(updatedMetadata.getOrganisationSignerConfigDto().getMerlotVerificationMethod(),
            editedMetadata.getOrganisationSignerConfigDto().getMerlotVerificationMethod());

        verify(outgoingMessageService, times(1)).sendOrganizationMembershipRevokedMessage(participantDto.getId());
    }

    @Test
    void updateParticipantNonExistent() throws JsonProcessingException {

        MerlotParticipantDto dtoWithEdits = getMerlotParticipantDtoWithEdits();
        dtoWithEdits.setId("did:web:example.com:participant:someunknownorga");

        OrganizationRoleGrantedAuthority activeRole = new OrganizationRoleGrantedAuthority(OrganizationRole.FED_ADMIN, "did:web:example.com:participant:someorga");

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
            () -> participantService.updateParticipant(dtoWithEdits, activeRole));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
    }

    @Test
    void getAllFederatorsNoFederatorsExisting() throws Exception {

        GXFSCatalogListResponse<SelfDescriptionItem> sdItems = new GXFSCatalogListResponse<>();
        sdItems.setItems(new ArrayList<>());
        sdItems.setTotalCount(0);

        lenient().when(gxfsCatalogService.getSelfDescriptionsByIds(eq(new String[0]))).thenReturn(sdItems);

        List<MerlotParticipantDto> organizations = participantService.getFederators();
        assertThat(organizations, empty());
    }

    @Test
    void getAllFederators() {

        MerlotParticipantMetaDto metaDto = new MerlotParticipantMetaDto();
        metaDto.setOrgaId(id);
        metaDto.setMailAddress("mymail@example.com");
        metaDto.setMembershipClass(MembershipClass.FEDERATOR);
        List<MerlotParticipantMetaDto> list = new ArrayList<>();
        list.add(metaDto);

        lenient().when(organizationMetadataService.getParticipantsByMembershipClass(eq(MembershipClass.FEDERATOR))).thenReturn(list);

        List<MerlotParticipantDto> organizations = participantService.getFederators();
        assertThat(organizations, not(empty()));
        assertEquals(1, organizations.size());
        String resultId = organizations.get(0).getSelfDescription()
                .findFirstCredentialSubjectByType(GxLegalParticipantCredentialSubject.class).getId();
        assertEquals(id, resultId);
    }

    @Test
    void createParticipantWithValidRegistrationFormAsFederator() throws Exception {
        MerlotParticipantDto participantDto = participantService.createParticipant(getTestRegistrationFormContent(),
                new OrganizationRoleGrantedAuthority(OrganizationRole.FED_ADMIN, "did:web:example.com:participant:somefedorga"));
        MerlotLegalParticipantCredentialSubject resultMerlotParticipantCs = participantDto.getSelfDescription()
                        .findFirstCredentialSubjectByType(MerlotLegalParticipantCredentialSubject.class);
        GxLegalParticipantCredentialSubject resultGxParticipantCs = participantDto.getSelfDescription()
                .findFirstCredentialSubjectByType(GxLegalParticipantCredentialSubject.class);
        GxLegalRegistrationNumberCredentialSubject resultGxRegistrationNumberCs = participantDto.getSelfDescription()
                .findFirstCredentialSubjectByType(GxLegalRegistrationNumberCredentialSubject.class);

        assertThat(resultMerlotParticipantCs).usingRecursiveComparison().ignoringFields("id")
                .isEqualTo(getMerlotParticipantCs(id));
        assertThat(resultGxParticipantCs).usingRecursiveComparison().ignoringFields("id", "legalRegistrationNumber")
                .isEqualTo(getGxParticipantCs(id));
        assertThat(resultGxRegistrationNumberCs).usingRecursiveComparison().ignoringFields("id")
                .isEqualTo(getGxRegistrationNumberCs(id));

        String id = resultGxParticipantCs.getId();
        assertThat(id).isNotNull().isNotBlank();

        OrganizationMetadata metadataExpected = new OrganizationMetadata(id, mailAddress,
                MembershipClass.PARTICIPANT, true, Collections.emptySet());

        ArgumentCaptor<MerlotParticipantMetaDto> varArgs = ArgumentCaptor.forClass(MerlotParticipantMetaDto.class);
        verify(organizationMetadataService, times(1)).saveMerlotParticipantMeta(varArgs.capture());
        assertEquals(metadataExpected.getOrgaId(), varArgs.getValue().getOrgaId());
        assertEquals(metadataExpected.getMailAddress(), varArgs.getValue().getMailAddress());
        assertEquals(metadataExpected.getMembershipClass(), varArgs.getValue().getMembershipClass());
        assertEquals(0,  varArgs.getValue().getConnectors().size());
    }

    @Test
    void createParticipantWithInvalidRegistrationForm() {

        RegistrationFormContent content = new RegistrationFormContent();
        OrganizationRoleGrantedAuthority role =
                new OrganizationRoleGrantedAuthority(OrganizationRole.FED_ADMIN, "did:web:example.com:participant:someorga");
        Exception e = assertThrows(ResponseStatusException.class,
            () -> participantService.createParticipant(content, role));

        assertEquals("400 BAD_REQUEST \"Invalid registration form file.\"", e.getMessage());
    }

    @Test
    void createParticipantWithEmptyFieldsInRegistrationForm() {

        RegistrationFormContent content = new RegistrationFormContent();
        content.setOrganizationName("");
        content.setOrganizationLegalName("");
        content.setMailAddress("");
        content.setRegistrationNumberLeiCode("");
        content.setRegistrationNumberEori("");
        content.setRegistrationNumberVatID("");
        content.setCountryCode("");
        content.setPostalCode("");
        content.setCity("");
        content.setStreet("");
        content.setProviderTncLink("");
        content.setProviderTncHash("");

        OrganizationRoleGrantedAuthority role =
                new OrganizationRoleGrantedAuthority(OrganizationRole.FED_ADMIN, "did:web:example.com:participant:someorga");
        Exception e = assertThrows(ResponseStatusException.class,
            () -> participantService.createParticipant(content, role));

        assertEquals("400 BAD_REQUEST \"Invalid registration form: Empty or blank fields.\"", e.getMessage());
    }

    @Test
    void getTrustedDidsNoFederatorsExisting() {

        List<String> trustedDids = participantService.getTrustedDids();
        assertThat(trustedDids, empty());
    }

    @Test
    void getTrustedDids() {

        String orgaId = "did:web:" + merlotDomain + ":participant:someorga";
        lenient().when(organizationMetadataService.getParticipantIdsByMembershipClass(eq(MembershipClass.FEDERATOR))).thenReturn(List.of(orgaId));

        List<String> trustedDids = participantService.getTrustedDids();
        assertThat(trustedDids, not(empty()));
        assertEquals(1, trustedDids.size());
        assertEquals(orgaId, trustedDids.get(0));
    }

    @Test
    void updateParticipantAsParticipantNoSignerConfig() throws Exception {

        MerlotParticipantDto participantDtoWithEdits = getMerlotParticipantDtoWithEdits();
        participantDtoWithEdits.setId("did:web:example.com:participant:nosignerconfig");
        participantDtoWithEdits.getMetadata().setOrganisationSignerConfigDto(null);

        OrganizationRoleGrantedAuthority activeRole = new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, "did:web:example.com:participant:nosignerconfig");

        ResponseStatusException e =
            assertThrows(ResponseStatusException.class, () -> participantService.updateParticipant(participantDtoWithEdits, activeRole));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, e.getStatusCode());


    }

    @Test
    void updateParticipantAsFedAdminNoSignerConfig() throws Exception {

        MerlotParticipantDto participantDtoWithEdits = getMerlotParticipantDtoWithEdits();

        OrganizationRoleGrantedAuthority activeRole = new OrganizationRoleGrantedAuthority(OrganizationRole.FED_ADMIN, "did:web:example.com:participant:nosignerconfig");

        ResponseStatusException e =
            assertThrows(ResponseStatusException.class, () -> participantService.updateParticipant(participantDtoWithEdits, activeRole));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, e.getStatusCode());
    }

    @Test
    void createParticipantAsFederatorNoSignerConfig() throws Exception {

        RegistrationFormContent registrationFormContent = getTestRegistrationFormContent();

        OrganizationRoleGrantedAuthority activeRole = new OrganizationRoleGrantedAuthority(OrganizationRole.FED_ADMIN, "did:web:example.com:participant:nosignerconfig");

        ResponseStatusException e =
            assertThrows(ResponseStatusException.class, () -> participantService.createParticipant(registrationFormContent, activeRole));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, e.getStatusCode());
    }

    @Test
    void updateParticipantAsParticipantEmptySignerConfig() throws Exception {

        MerlotParticipantDto participantDtoWithEdits = getMerlotParticipantDtoWithEdits();
        participantDtoWithEdits.setId("did:web:example.com:participant:emptysignerconfig");
        participantDtoWithEdits.getMetadata().setOrganisationSignerConfigDto(null);

        OrganizationRoleGrantedAuthority activeRole = new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, "did:web:example.com:participant:emptysignerconfig");

        ResponseStatusException e =
            assertThrows(ResponseStatusException.class, () -> participantService.updateParticipant(participantDtoWithEdits, activeRole));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, e.getStatusCode());


    }

    @Test
    void updateParticipantAsFedAdminEmptySignerConfig() throws Exception {

        MerlotParticipantDto participantDtoWithEdits = getMerlotParticipantDtoWithEdits();

        OrganizationRoleGrantedAuthority activeRole = new OrganizationRoleGrantedAuthority(OrganizationRole.FED_ADMIN, "did:web:example.com:participant:emptysignerconfig");

        ResponseStatusException e =
            assertThrows(ResponseStatusException.class, () -> participantService.updateParticipant(participantDtoWithEdits, activeRole));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, e.getStatusCode());
    }

    @Test
    void createParticipantAsFederatorEmptySignerConfig() throws Exception {

        RegistrationFormContent registrationFormContent = getTestRegistrationFormContent();

        OrganizationRoleGrantedAuthority activeRole = new OrganizationRoleGrantedAuthority(OrganizationRole.FED_ADMIN, "did:web:example.com:participant:emptysignerconfig");

        ResponseStatusException e =
            assertThrows(ResponseStatusException.class, () -> participantService.createParticipant(registrationFormContent, activeRole));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, e.getStatusCode());
    }

    @Test
    void getAgentDidsFromParticipant() {
        ParticipantAgentDidsDto didsDto = participantService.getAgentDidsByParticipantId("did:web:example.com:participant:someorga");
        assertNotNull(didsDto);
        assertFalse(didsDto.getAgentDids().isEmpty());
        assertEquals("123456", didsDto.getAgentDids().iterator().next());
    }

    private MerlotLegalParticipantCredentialSubject getTestEditedMerlotParticipantCs() {
        MerlotLegalParticipantCredentialSubject credentialSubject = new MerlotLegalParticipantCredentialSubject();
        credentialSubject.setId("did:web:changedorga.example.com");
        credentialSubject.setLegalName("changedLegalName");
        ParticipantTermsAndConditions termsAndConditions = new ParticipantTermsAndConditions();
        termsAndConditions.setUrl("http://changed.com");
        termsAndConditions.setHash("changedHash");
        credentialSubject.setTermsAndConditions(termsAndConditions);
        return credentialSubject;
    }

    private GxLegalParticipantCredentialSubject getTestEditedGxParticipantCs() {

        GxLegalParticipantCredentialSubject credentialSubject = new GxLegalParticipantCredentialSubject();
        credentialSubject.setId("did:web:changedorga.example.com");
        credentialSubject.setLegalRegistrationNumber(List.of(new NodeKindIRITypeId("did:web:changedorga.example.com-regId")));
        GxVcard address = new GxVcard();
        address.setStreetAddress("changedAddress");
        address.setLocality("changedCity");
        address.setCountryCode("changedCountry");
        address.setCountrySubdivisionCode("changedCountry-BE");
        address.setPostalCode("changedPostCode");
        credentialSubject.setLegalAddress(address);
        credentialSubject.setHeadquarterAddress(address);
        credentialSubject.setName("changedOrgaName");
        return credentialSubject;
    }

    private GxLegalRegistrationNumberCredentialSubject getTestEditedGxRegistrationNumberCs() {
        GxLegalRegistrationNumberCredentialSubject credentialSubject = new GxLegalRegistrationNumberCredentialSubject();
        credentialSubject.setId("did:web:changedorga.example.com-regId");
        credentialSubject.setLeiCode("changedLeiCode");
        credentialSubject.setEori("changedEori");
        credentialSubject.setEuid("changedEuid");
        credentialSubject.setTaxID("changedTaxId");
        credentialSubject.setVatID("changedVatId");
        return credentialSubject;
    }

    private WebClientResponseException getWebClientResponseException(){
        byte[] byteArray = {123, 34, 99, 111, 100, 101, 34, 58, 34, 110, 111, 116, 95, 102, 111, 117, 110, 100, 95, 101,
            114, 114, 111, 114, 34, 44, 34, 109, 101, 115, 115, 97, 103, 101, 34, 58, 34, 80, 97, 114,
            116, 105, 99, 105, 112, 97, 110, 116, 32, 110, 111, 116, 32, 102, 111, 117, 110, 100, 58,
            32, 80, 97, 114, 116, 105, 99, 105, 112, 97, 110, 116, 58, 49, 50, 51, 52, 49, 51, 52, 50,
            51, 52, 50, 49, 34, 125};
        return new WebClientResponseException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "garbage", null, byteArray, null);
    }

    private MerlotParticipantDto getMerlotParticipantDtoWithEdits() throws JsonProcessingException {

        MerlotParticipantDto dtoWithEdits = new MerlotParticipantDto();

        MerlotLegalParticipantCredentialSubject editedMerlotParticipantCs = getTestEditedMerlotParticipantCs();
        GxLegalParticipantCredentialSubject editedGxParticipantCs = getTestEditedGxParticipantCs();
        GxLegalRegistrationNumberCredentialSubject editedGxRegistrationNumberCs = getTestEditedGxRegistrationNumberCs();

        ExtendedVerifiablePresentation vp = createVpFromCsList(
                List.of(editedGxParticipantCs, editedGxRegistrationNumberCs, editedMerlotParticipantCs), "did:web:someorga");

        String someOrgaId = "did:web:example.com:participant:someorga";
        MerlotParticipantMetaDto metaData = new MerlotParticipantMetaDto();
        metaData.setOrgaId(someOrgaId);
        metaData.setMailAddress("changedMailAddress");
        metaData.setMembershipClass(MembershipClass.FEDERATOR);
        metaData.setActive(false);

        OrganizationConnectorDto connector = new OrganizationConnectorDto();
        connector.setConnectorId("edc1");
        connector.setConnectorEndpoint("https://edc1.edchub.dev");
        connector.setConnectorAccessToken("token$123?");
        connector.setIonosS3ExtensionConfig(new IonosS3ExtensionConfigDto());
        connector.getIonosS3ExtensionConfig().setBuckets(List.of(
                new IonosS3BucketDto("bucket1", "http://example.com"),
                new IonosS3BucketDto("bucket2", "http://example.com"),
                new IonosS3BucketDto("bucket3", "http://example.com")));
        metaData.setConnectors(Set.of(connector));
        OrganisationSignerConfigDto signerConfigDto = new OrganisationSignerConfigDto();
        signerConfigDto.setPrivateKey("changedprivateKey");
        signerConfigDto.setMerlotVerificationMethod("did:web:example.com:participant:someorga#changedmerlot");
        signerConfigDto.setVerificationMethod("did:web:example.com:participant:someorga#changedsomemethod");
        metaData.setOrganisationSignerConfigDto(signerConfigDto);

        dtoWithEdits.setSelfDescription(vp);
        dtoWithEdits.setId(someOrgaId);
        dtoWithEdits.setMetadata(metaData);
        return dtoWithEdits;
    }
}

