package eu.merloteducation.organisationsorchestrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.authorizationlibrary.authorization.OrganizationRole;
import eu.merloteducation.authorizationlibrary.authorization.OrganizationRoleGrantedAuthority;
import eu.merloteducation.gxfscataloglibrary.models.exception.CredentialPresentationException;
import eu.merloteducation.gxfscataloglibrary.models.exception.CredentialSignatureException;
import eu.merloteducation.gxfscataloglibrary.models.participants.ParticipantItem;
import eu.merloteducation.gxfscataloglibrary.models.query.GXFSQueryLegalNameItem;
import eu.merloteducation.gxfscataloglibrary.models.query.GXFSQueryUriItem;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.GXFSCatalogListResponse;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescription;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescriptionItem;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescriptionVerifiableCredential;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gax.datatypes.RegistrationNumber;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gax.datatypes.TermsAndConditions;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gax.datatypes.VCard;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotOrganizationCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.service.GxfsCatalogService;
import eu.merloteducation.modelslib.api.did.ParticipantDidPrivateKeyCreateRequest;
import eu.merloteducation.modelslib.api.organization.*;
import eu.merloteducation.organisationsorchestrator.config.InitialDataLoader;
import eu.merloteducation.organisationsorchestrator.mappers.OrganizationMapper;
import eu.merloteducation.organisationsorchestrator.models.RegistrationFormContent;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganizationMetadata;
import eu.merloteducation.organisationsorchestrator.service.OmejdnConnectorApiClient;
import eu.merloteducation.organisationsorchestrator.service.OrganizationMetadataService;
import eu.merloteducation.organisationsorchestrator.service.OutgoingMessageService;
import eu.merloteducation.organisationsorchestrator.service.ParticipantService;
import org.apache.commons.text.StringEscapeUtils;

import static org.assertj.core.api.Assertions.assertThat;

import org.jetbrains.annotations.NotNull;
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

    @Value("${merlot-domain}")
    private String merlotDomain;

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

    String organizationLegalName = "Organization Legal Name";

    String registrationNumber = "DE123456789";

    String countryCode = "DE";

    String street = "Street 123";

    String providerTncHash = "hash1234567890";

    String providerTncLink = "abc.de";

    String city = "City";

    String organizationName = "Organization Name";

    String postalCode = "12345";

    String id = "12345";

    ParticipantServiceTests() throws IOException {
    }

    MerlotOrganizationCredentialSubject getExpectedCredentialSubject() {

        Map<String, String> context = getContext();

        MerlotOrganizationCredentialSubject expected = new MerlotOrganizationCredentialSubject();
        expected.setOrgaName(organizationName);
        expected.setLegalName(organizationLegalName);

        RegistrationNumber registrationNumberObj = new RegistrationNumber();
        registrationNumberObj.setType("gax-trust-framework:RegistrationNumber");
        registrationNumberObj.setLocal(registrationNumber);
        expected.setRegistrationNumber(registrationNumberObj);

        VCard vCard = new VCard();
        vCard.setLocality(city);
        vCard.setPostalCode(postalCode);
        vCard.setCountryName(countryCode);
        vCard.setStreetAddress(street);
        vCard.setType("vcard:Address");
        expected.setLegalAddress(vCard);
        expected.setHeadquarterAddress(vCard);

        TermsAndConditions termsAndConditions = new TermsAndConditions();
        termsAndConditions.setContent(providerTncLink);
        termsAndConditions.setHash(providerTncHash);
        termsAndConditions.setType("gax-trust-framework:TermsAndConditions");
        expected.setTermsAndConditions(termsAndConditions);

        expected.setContext(context);
        expected.setType("merlot:MerlotOrganization");

        return expected;
    }

    Map<String, String> getContext() {

        Map<String, String> context = new HashMap<>();
        context.put("gax-trust-framework", "http://w3id.org/gaia-x/gax-trust-framework#");
        context.put("gax-validation", "http://w3id.org/gaia-x/validation#");
        context.put("merlot", "http://w3id.org/gaia-x/merlot#");
        context.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        context.put("sh", "http://www.w3.org/ns/shacl#");
        context.put("skos", "http://www.w3.org/2004/02/skos/core#");
        context.put("vcard", "http://www.w3.org/2006/vcard/ns#");
        context.put("xsd", "http://www.w3.org/2001/XMLSchema#");
        return context;
    }

    RegistrationFormContent getTestRegistrationFormContent() throws IOException {

        RegistrationFormContent content = new RegistrationFormContent();
        content.setOrganizationName(organizationName);
        content.setOrganizationLegalName(organizationLegalName);
        content.setMailAddress(mailAddress);
        content.setRegistrationNumberLocal(registrationNumber);
        content.setCountryCode(countryCode);
        content.setPostalCode(postalCode);
        content.setCity(city);
        content.setStreet(street);
        content.setProviderTncLink(providerTncLink);
        content.setProviderTncHash(providerTncHash);
        content.setDidWeb("");

        return content;
    }

    private ParticipantItem wrapCredentialSubjectInItem(MerlotOrganizationCredentialSubject credentialSubject) {
        ParticipantItem item = new ParticipantItem();
        item.setSelfDescription(new SelfDescription());
        item.getSelfDescription().setVerifiableCredential(new SelfDescriptionVerifiableCredential());
        item.getSelfDescription().getVerifiableCredential().setCredentialSubject(credentialSubject);
        item.setId(credentialSubject.getId());
        item.setName(credentialSubject.getLegalName());
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

        String mockParticipant = """
            {
                "id": "did:web:example.com:participant:someorga",
                "name": "did:web:example.com:participant:someorga",
                "publicKey": "{\\"kty\\":\\"RSA\\",\\"e\\":\\"AQAB\\",\\"alg\\":\\"PS256\\",\\"n\\":\\"dummy\\"}",
                "selfDescription":"{\\"@id\\": \\"http://example.edu/verifiablePresentation/self-description1\\", \\"proof\\": {\\"created\\": \\"2023-04-27T13:48:11Z\\", \\"jws\\": \\"dummy\\", \\"proofPurpose\\": \\"assertionMethod\\", \\"type\\": \\"JsonWebSignature2020\\", \\"verificationMethod\\": \\"did:web:compliance.lab.gaia-x.eu\\"}, \\"type\\": [\\"VerifiablePresentation\\"], \\"@context\\": [\\"https://www.w3.org/2018/credentials/v1\\"], \\"verifiableCredential\\": {\\"credentialSubject\\": {\\"gax-trust-framework:registrationNumber\\": {\\"gax-trust-framework:local\\": {\\"@value\\": \\"0762747721\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"gax-trust-framework:RegistrationNumber\\"}, \\"gax-trust-framework:legalName\\": {\\"@value\\": \\"Gaia-X European Association for Data and Cloud AISBL\\", \\"@type\\": \\"xsd:string\\"}, \\"gax-trust-framework:headquarterAddress\\": {\\"vcard:country-name\\": {\\"@value\\": \\"BE\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"vcard:Address\\", \\"vcard:street-address\\": {\\"@value\\": \\"Avenue des Arts 6-9\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:locality\\": {\\"@value\\": \\"Brüssel\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:postal-code\\": {\\"@value\\": \\"1210\\", \\"@type\\": \\"xsd:string\\"}}, \\"merlot:termsAndConditions\\": {\\"gax-trust-framework:content\\": {\\"@value\\": \\"http://example.com\\", \\"@type\\": \\"xsd:anyURI\\"}, \\"gax-trust-framework:hash\\": {\\"@value\\": \\"hash1234\\", \\"@type\\": \\"xsd:string\\"}}, \\"@type\\": \\"merlot:MerlotOrganization\\", \\"gax-trust-framework:legalAddress\\": {\\"vcard:country-name\\": {\\"@value\\": \\"BE\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"vcard:Address\\", \\"vcard:street-address\\": {\\"@value\\": \\"Avenue des Arts 6-9\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:locality\\": {\\"@value\\": \\"Brüssel\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:postal-code\\": {\\"@value\\": \\"1210\\", \\"@type\\": \\"xsd:string\\"}}, \\"merlot:orgaName\\": {\\"@value\\": \\"Gaia-X AISBL\\", \\"@type\\": \\"xsd:string\\"}, \\"@id\\": \\"did:web:example.com:participant:someorga\\", \\"@context\\": {\\"merlot\\": \\"http://w3id.org/gaia-x/merlot#\\", \\"gax-trust-framework\\": \\"http://w3id.org/gaia-x/gax-trust-framework#\\", \\"rdf\\": \\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\\", \\"sh\\": \\"http://www.w3.org/ns/shacl#\\", \\"xsd\\": \\"http://www.w3.org/2001/XMLSchema#\\", \\"gax-validation\\": \\"http://w3id.org/gaia-x/validation#\\", \\"skos\\": \\"http://www.w3.org/2004/02/skos/core#\\", \\"vcard\\": \\"http://www.w3.org/2006/vcard/ns#\\"}}, \\"issuanceDate\\": \\"2022-10-19T18:48:09Z\\", \\"@type\\": [\\"VerifiableCredential\\"], \\"@id\\": \\"https://www.example.org/legalPerson.json\\", \\"proof\\": {\\"created\\": \\"2023-04-27T13:48:10Z\\", \\"jws\\": \\"dummy\\", \\"proofPurpose\\": \\"assertionMethod\\", \\"type\\": \\"JsonWebSignature2020\\", \\"verificationMethod\\": \\"did:web:compliance.lab.gaia-x.eu\\"}, \\"@context\\": [\\"https://www.w3.org/2018/credentials/v1\\"], \\"issuer\\": \\"did:web:example.com:participant:someorga\\"}}"
            }
            """;
        mockParticipant = StringEscapeUtils.unescapeJson(mockParticipant);
        if (mockParticipant != null)
            mockParticipant = mockParticipant.replace("\"{", "{").replace("}\"", "}");
        ParticipantItem participantItem = mapper.readValue(mockParticipant, new TypeReference<>() {});

        String mockUserResponse = """
            {
                "totalCount": 1,
                "items": [
                    {
                         "meta": {
                             "expirationTime": null,
                             "content": "{\\"@id\\": \\"http://example.edu/verifiablePresentation/self-description1\\", \\"proof\\": {\\"created\\": \\"2023-08-30T08:58:35Z\\", \\"jws\\": \\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..JENTxPd26Ke05vIjtCzMESUvla_iYqP00ppsJfKagE06-XegrCbgRFoty20Tf40tPCd9_VflRL3kW12VCoOlDPA2nc21jaa_vmv8ZCCFNBmXIJVrBmF370MdyRT53Z-TGPKoUv5iF0m5fibKqqtg8MMCNVG9J3eff-Q04Wc5jZTgq2a9mjRsuZUAcnmu6ZgO4aaCKPD1t2aI3pZpie5zk5RJ37ZezuYQa7zdRirq_8Qaa9acg-aVqLaGxFAJhcpOcck-zkaP52pxVCusLt2bVUSG6HVk9txwCoc8ZoGCXW27MN8SM3I5PwfD_3OXGvs4TR0j-9ylKSajwWYRclNDtJMSBhmtXu_wjrjDMZFG2kRow_p1xhZZ71DKlX2Efp6VAdSWYbPpguZv1qMYbBemC3DW2lhkOsk1_KkwICO3ZSySNEswsjDty3NuGUOZtyyvImbSZ5f3I7ZyMNvnL1xoYEteK6mBSB9H7Zr1E1yZr7K1eiXR2MQuxKaFYl6jikYuwpdyrD6lvOWCEKOBQ_yjaQ9lbySiOxbNykpOX6-Bbu6mVQIX08BEzg0Y8r0Bnce2KPWypMtyHW7KhVgok2aLIjFQGutG7pgeIXIK2mPIR5jxUWUUh3XDuuU21cDYbMb6wYNX_-sNHNsots-mA81kRPlSRWlXBkvsZffXo6bWhKQ\\", \\"proofPurpose\\": \\"assertionMethod\\", \\"type\\": \\"JsonWebSignature2020\\", \\"verificationMethod\\": \\"did:web:compliance.lab.gaia-x.eu\\"}, \\"type\\": [\\"VerifiablePresentation\\"], \\"@context\\": [\\"https://www.w3.org/2018/credentials/v1\\"], \\"verifiableCredential\\": {\\"credentialSubject\\": {\\"gax-trust-framework:registrationNumber\\": {\\"gax-trust-framework:local\\": {\\"@value\\": \\"0762747721\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"gax-trust-framework:RegistrationNumber\\"}, \\"gax-trust-framework:legalName\\": {\\"@value\\": \\"Gaia-X European Association for Data and Cloud AISBL\\", \\"@type\\": \\"xsd:string\\"}, \\"gax-trust-framework:headquarterAddress\\": {\\"vcard:country-name\\": {\\"@value\\": \\"BE\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"vcard:Address\\", \\"vcard:street-address\\": {\\"@value\\": \\"Avenue des Arts 6-9\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:locality\\": {\\"@value\\": \\"Br\\\\u00fcssel\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:postal-code\\": {\\"@value\\": \\"1210\\", \\"@type\\": \\"xsd:string\\"}}, \\"@type\\": \\"merlot:MerlotOrganization\\", \\"gax-trust-framework:legalAddress\\": {\\"vcard:country-name\\": {\\"@value\\": \\"BE\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"vcard:Address\\", \\"vcard:street-address\\": {\\"@value\\": \\"Avenue des Arts 6-9\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:locality\\": {\\"@value\\": \\"Br\\\\u00fcssel\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:postal-code\\": {\\"@value\\": \\"1210\\", \\"@type\\": \\"xsd:string\\"}}, \\"merlot:orgaName\\": {\\"@value\\": \\"Gaia-X AISBL\\", \\"@type\\": \\"xsd:string\\"}, \\"@id\\": \\"did:web:example.com:participant:someorga\\", \\"@context\\": {\\"merlot\\": \\"http://w3id.org/gaia-x/merlot#\\", \\"gax-trust-framework\\": \\"http://w3id.org/gaia-x/gax-trust-framework#\\", \\"rdf\\": \\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\\", \\"sh\\": \\"http://www.w3.org/ns/shacl#\\", \\"xsd\\": \\"http://www.w3.org/2001/XMLSchema#\\", \\"gax-validation\\": \\"http://w3id.org/gaia-x/validation#\\", \\"skos\\": \\"http://www.w3.org/2004/02/skos/core#\\", \\"vcard\\": \\"http://www.w3.org/2006/vcard/ns#\\"}, \\"merlot:termsAndConditions\\": {\\"gax-trust-framework:content\\": {\\"@value\\": \\"http://example.com\\", \\"@type\\": \\"xsd:anyURI\\"}, \\"@type\\": \\"gax-trust-framework:TermsAndConditions\\", \\"gax-trust-framework:hash\\": {\\"@value\\": \\"hash1234\\", \\"@type\\": \\"xsd:string\\"}}}, \\"issuanceDate\\": \\"2022-10-19T18:48:09Z\\", \\"@type\\": [\\"VerifiableCredential\\"], \\"@id\\": \\"https://www.example.org/legalPerson.json\\", \\"proof\\": {\\"created\\": \\"2023-08-30T08:58:34Z\\", \\"jws\\": \\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..yV2y2TdkrLpsrP1ZTmtEjazcRMtKKwEHviNx_cC3BxBM8R2DeAbBuxbgcO_3ZoUeuB-6laSe2RN6jIp5UMCVRzgFg1YIbNKMcyDfC2LhF0YaXxo9pB-L3qLdRJpve3-NBHj76dBUW4Q04S_4t_5M09p61fEuCRJIIrDzh2iKSLwzKrv2sT8hnMefN2P29sa5QlJrRu-kFHolq_ZmwsXzWMN3R8_8tbsS19eP1hIzkuzW1lla8jZot06Y6bFslr3S5CCbexABJb34puu2nbH2n4Qdc9BS31B34HnduC8AuKEbOfmWsGDSZT29QjL-VxUWN4lhqxb-DsiSpmDlEPt_UzJah5tvMSQzAlKpm2ZZdBuQb8Mk9-U9oRmrxm6xeOcXdcBMAHXEYlBMp6R8gEOyQ3uDMrR2x9xMTs4EeJgJlOSsyK7F5_EbMtqnLulKRD4RtNoZ_I8k0XcVZAVoBtxrEwWOE48AdW16yfemqEO8s8J_J9TrBaTTMKIMFqJjJ-HNc9n7E_saylVFRadHevbLLuBNDOjjwvI8E5r55iO2HTPxB1dSWcjidSacSCvo2zQxRLkbPQJmLp2S4SCMLbqwPdph8KH6tfAcgxH0k3sTmwvt2tTLBXCINPlnhv2ahuHzXWGpgegEyHLrtlUAwfeDilkc_lib_chWBVqqWxu-7Gw\\", \\"proofPurpose\\": \\"assertionMethod\\", \\"type\\": \\"JsonWebSignature2020\\", \\"verificationMethod\\": \\"did:web:compliance.lab.gaia-x.eu\\"}, \\"@context\\": [\\"https://www.w3.org/2018/credentials/v1\\"], \\"issuer\\": \\"did:web:example.com:participant:someorga\\"}}",
                             "subjectId": "did:web:example.com:participant:someorga",
                             "validators": [
                                 "did:web:compliance.lab.gaia-x.eu"
                             ],
                             "sdHash": "8b143ff8e0cf8f22c366cea9e1d31d97f79aa29eee5741f048637a43b7f059b0",
                             "id": "did:web:example.com:participant:someorga",
                             "status": "active",
                             "issuer": "did:web:example.com:participant:someorga",
                             "validatorDids": [
                                 "did:web:compliance.lab.gaia-x.eu"
                             ],
                             "uploadDatetime": "2023-08-30T08:58:35.894486Z",
                             "statusDatetime": "2023-08-30T08:58:35.894486Z"
                         },
                         "content": "{\\"@id\\": \\"http://example.edu/verifiablePresentation/self-description1\\", \\"proof\\": {\\"created\\": \\"2023-08-30T08:58:35Z\\", \\"jws\\": \\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..JENTxPd26Ke05vIjtCzMESUvla_iYqP00ppsJfKagE06-XegrCbgRFoty20Tf40tPCd9_VflRL3kW12VCoOlDPA2nc21jaa_vmv8ZCCFNBmXIJVrBmF370MdyRT53Z-TGPKoUv5iF0m5fibKqqtg8MMCNVG9J3eff-Q04Wc5jZTgq2a9mjRsuZUAcnmu6ZgO4aaCKPD1t2aI3pZpie5zk5RJ37ZezuYQa7zdRirq_8Qaa9acg-aVqLaGxFAJhcpOcck-zkaP52pxVCusLt2bVUSG6HVk9txwCoc8ZoGCXW27MN8SM3I5PwfD_3OXGvs4TR0j-9ylKSajwWYRclNDtJMSBhmtXu_wjrjDMZFG2kRow_p1xhZZ71DKlX2Efp6VAdSWYbPpguZv1qMYbBemC3DW2lhkOsk1_KkwICO3ZSySNEswsjDty3NuGUOZtyyvImbSZ5f3I7ZyMNvnL1xoYEteK6mBSB9H7Zr1E1yZr7K1eiXR2MQuxKaFYl6jikYuwpdyrD6lvOWCEKOBQ_yjaQ9lbySiOxbNykpOX6-Bbu6mVQIX08BEzg0Y8r0Bnce2KPWypMtyHW7KhVgok2aLIjFQGutG7pgeIXIK2mPIR5jxUWUUh3XDuuU21cDYbMb6wYNX_-sNHNsots-mA81kRPlSRWlXBkvsZffXo6bWhKQ\\", \\"proofPurpose\\": \\"assertionMethod\\", \\"type\\": \\"JsonWebSignature2020\\", \\"verificationMethod\\": \\"did:web:compliance.lab.gaia-x.eu\\"}, \\"type\\": [\\"VerifiablePresentation\\"], \\"@context\\": [\\"https://www.w3.org/2018/credentials/v1\\"], \\"verifiableCredential\\": {\\"credentialSubject\\": {\\"gax-trust-framework:registrationNumber\\": {\\"gax-trust-framework:local\\": {\\"@value\\": \\"0762747721\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"gax-trust-framework:RegistrationNumber\\"}, \\"gax-trust-framework:legalName\\": {\\"@value\\": \\"Gaia-X European Association for Data and Cloud AISBL\\", \\"@type\\": \\"xsd:string\\"}, \\"gax-trust-framework:headquarterAddress\\": {\\"vcard:country-name\\": {\\"@value\\": \\"BE\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"vcard:Address\\", \\"vcard:street-address\\": {\\"@value\\": \\"Avenue des Arts 6-9\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:locality\\": {\\"@value\\": \\"Br\\\\u00fcssel\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:postal-code\\": {\\"@value\\": \\"1210\\", \\"@type\\": \\"xsd:string\\"}}, \\"@type\\": \\"merlot:MerlotOrganization\\", \\"gax-trust-framework:legalAddress\\": {\\"vcard:country-name\\": {\\"@value\\": \\"BE\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"vcard:Address\\", \\"vcard:street-address\\": {\\"@value\\": \\"Avenue des Arts 6-9\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:locality\\": {\\"@value\\": \\"Br\\\\u00fcssel\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:postal-code\\": {\\"@value\\": \\"1210\\", \\"@type\\": \\"xsd:string\\"}}, \\"merlot:orgaName\\": {\\"@value\\": \\"Gaia-X AISBL\\", \\"@type\\": \\"xsd:string\\"}, \\"@id\\": \\"did:web:example.com:participant:someorga\\", \\"@context\\": {\\"merlot\\": \\"http://w3id.org/gaia-x/merlot#\\", \\"gax-trust-framework\\": \\"http://w3id.org/gaia-x/gax-trust-framework#\\", \\"rdf\\": \\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\\", \\"sh\\": \\"http://www.w3.org/ns/shacl#\\", \\"xsd\\": \\"http://www.w3.org/2001/XMLSchema#\\", \\"gax-validation\\": \\"http://w3id.org/gaia-x/validation#\\", \\"skos\\": \\"http://www.w3.org/2004/02/skos/core#\\", \\"vcard\\": \\"http://www.w3.org/2006/vcard/ns#\\"}, \\"merlot:termsAndConditions\\": {\\"gax-trust-framework:content\\": {\\"@value\\": \\"http://example.com\\", \\"@type\\": \\"xsd:anyURI\\"}, \\"@type\\": \\"gax-trust-framework:TermsAndConditions\\", \\"gax-trust-framework:hash\\": {\\"@value\\": \\"hash1234\\", \\"@type\\": \\"xsd:string\\"}}}, \\"issuanceDate\\": \\"2022-10-19T18:48:09Z\\", \\"@type\\": [\\"VerifiableCredential\\"], \\"@id\\": \\"https://www.example.org/legalPerson.json\\", \\"proof\\": {\\"created\\": \\"2023-08-30T08:58:34Z\\", \\"jws\\": \\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..yV2y2TdkrLpsrP1ZTmtEjazcRMtKKwEHviNx_cC3BxBM8R2DeAbBuxbgcO_3ZoUeuB-6laSe2RN6jIp5UMCVRzgFg1YIbNKMcyDfC2LhF0YaXxo9pB-L3qLdRJpve3-NBHj76dBUW4Q04S_4t_5M09p61fEuCRJIIrDzh2iKSLwzKrv2sT8hnMefN2P29sa5QlJrRu-kFHolq_ZmwsXzWMN3R8_8tbsS19eP1hIzkuzW1lla8jZot06Y6bFslr3S5CCbexABJb34puu2nbH2n4Qdc9BS31B34HnduC8AuKEbOfmWsGDSZT29QjL-VxUWN4lhqxb-DsiSpmDlEPt_UzJah5tvMSQzAlKpm2ZZdBuQb8Mk9-U9oRmrxm6xeOcXdcBMAHXEYlBMp6R8gEOyQ3uDMrR2x9xMTs4EeJgJlOSsyK7F5_EbMtqnLulKRD4RtNoZ_I8k0XcVZAVoBtxrEwWOE48AdW16yfemqEO8s8J_J9TrBaTTMKIMFqJjJ-HNc9n7E_saylVFRadHevbLLuBNDOjjwvI8E5r55iO2HTPxB1dSWcjidSacSCvo2zQxRLkbPQJmLp2S4SCMLbqwPdph8KH6tfAcgxH0k3sTmwvt2tTLBXCINPlnhv2ahuHzXWGpgegEyHLrtlUAwfeDilkc_lib_chWBVqqWxu-7Gw\\", \\"proofPurpose\\": \\"assertionMethod\\", \\"type\\": \\"JsonWebSignature2020\\", \\"verificationMethod\\": \\"did:web:compliance.lab.gaia-x.eu\\"}, \\"@context\\": [\\"https://www.w3.org/2018/credentials/v1\\"], \\"issuer\\": \\"did:web:example.com:participant:someorga\\"}}"
                    }
                ]
            }
            """;

        mockUserResponse = StringEscapeUtils.unescapeJson(mockUserResponse);
        if (mockUserResponse != null)
            mockUserResponse = mockUserResponse.replace("\"{", "{").replace("}\"", "}");
        GXFSCatalogListResponse<SelfDescriptionItem> sdItems = mapper.readValue(mockUserResponse, new TypeReference<>() {});

        String mockQueryResponse = """
            {
                "totalCount": 1,
                "items": [
                    {
                        "p.uri": "did:web:example.com:participant:someorga"
                    }
                ]
            }
            """;
        mockQueryResponse = StringEscapeUtils.unescapeJson(mockQueryResponse);
        if (mockQueryResponse != null)
            mockQueryResponse = mockQueryResponse.replace("\"{", "{").replace("}\"", "}");
        GXFSCatalogListResponse<GXFSQueryUriItem> uriItems = mapper.readValue(mockQueryResponse, new TypeReference<>() {});

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
        lenient().when(gxfsCatalogService.updateParticipant(any(), any()))
            .thenAnswer(i -> wrapCredentialSubjectInItem((MerlotOrganizationCredentialSubject) i.getArguments()[0]));
        lenient().when(gxfsCatalogService.addParticipant(any(), any()))
            .thenAnswer(i -> wrapCredentialSubjectInItem((MerlotOrganizationCredentialSubject) i.getArguments()[0]));
        lenient().when(gxfsCatalogService.getParticipantLegalNameByUri(eq("MerlotOrganization"), any()))
            .thenReturn(new GXFSCatalogListResponse<>());

        MerlotParticipantMetaDto metaDto = getTestMerlotParticipantMetaDto();

        MerlotParticipantMetaDto metaDtoNoSignerConfig = getTestMerlotParticipantMetaDto();
        metaDtoNoSignerConfig.setOrganisationSignerConfigDto(new OrganisationSignerConfigDto());

        lenient().when(organizationMetadataService.getMerlotParticipantMetaDto(eq("did:web:example.com:participant:nosignerconfig"))).thenReturn(metaDtoNoSignerConfig);
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

        String id = organizations.getContent().get(0).getSelfDescription().getVerifiableCredential()
            .getCredentialSubject().getId();
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
        MerlotOrganizationCredentialSubject subject = (MerlotOrganizationCredentialSubject)
                organization.getSelfDescription().getVerifiableCredential().getCredentialSubject();
        assertEquals("did:web:example.com:participant:someorga", subject.getId());
        assertEquals("Gaia-X European Association for Data and Cloud AISBL", subject.getLegalName());

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
        MerlotOrganizationCredentialSubject editedCredentialSubject = (MerlotOrganizationCredentialSubject) participantDtoWithEdits.getSelfDescription()
            .getVerifiableCredential().getCredentialSubject();
        MerlotParticipantMetaDto editedMetadata = participantDtoWithEdits.getMetadata();

        OrganizationRoleGrantedAuthority activeRole = new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP, "did:web:example.com:participant:someorga");

        participantDtoWithEdits.setId("did:web:example.com:participant:someorga");
        MerlotParticipantDto updatedParticipantDto = participantService.updateParticipant(participantDtoWithEdits, activeRole);

        // following attributes of the organization credential subject should have been updated
        MerlotOrganizationCredentialSubject updatedCredentialSubject =
            (MerlotOrganizationCredentialSubject) updatedParticipantDto.getSelfDescription().getVerifiableCredential()
                .getCredentialSubject();
        assertEquals(updatedCredentialSubject.getTermsAndConditions().getContent(),
            editedCredentialSubject.getTermsAndConditions().getContent());
        assertEquals(updatedCredentialSubject.getTermsAndConditions().getHash(),
            editedCredentialSubject.getTermsAndConditions().getHash());
        assertEquals(updatedCredentialSubject.getLegalAddress().getStreetAddress(),
            editedCredentialSubject.getLegalAddress().getStreetAddress());
        assertEquals(updatedCredentialSubject.getLegalAddress().getLocality(),
            editedCredentialSubject.getLegalAddress().getLocality());
        assertEquals(updatedCredentialSubject.getLegalAddress().getCountryName(),
            editedCredentialSubject.getLegalAddress().getCountryName());
        assertEquals(updatedCredentialSubject.getLegalAddress().getPostalCode(),
            editedCredentialSubject.getLegalAddress().getPostalCode());

        // following attributes of the organization credential subject should not have been updated
        assertNotEquals(updatedCredentialSubject.getId(), editedCredentialSubject.getId());
        assertNotEquals(updatedCredentialSubject.getId(),
            editedCredentialSubject.getId());
        assertNotEquals(updatedCredentialSubject.getOrgaName(),
            editedCredentialSubject.getOrgaName());
        assertNotEquals(updatedCredentialSubject.getLegalName(),
            editedCredentialSubject.getLegalName());
        assertNotEquals(updatedCredentialSubject.getRegistrationNumber().getLocal(),
            editedCredentialSubject.getRegistrationNumber().getLocal());
        assertNull(updatedCredentialSubject.getRegistrationNumber().getEuid());
        assertNull(updatedCredentialSubject.getRegistrationNumber().getEori());
        assertNull(updatedCredentialSubject.getRegistrationNumber().getVatId());

        // following metadata of the organization should have been updated
        MerlotParticipantMetaDto updatedMetadata = updatedParticipantDto.getMetadata();
        assertEquals(updatedMetadata.getMailAddress(), editedMetadata.getMailAddress());
        assertEquals(1, updatedMetadata.getConnectors().size()); // connector was added

        // following metadata of the organization should not have been updated
        assertNotEquals(updatedMetadata.getMembershipClass(), editedMetadata.getMembershipClass());
        assertNotEquals(updatedMetadata.isActive(), editedMetadata.isActive());

        verify(outgoingMessageService, times(0)).sendOrganizationMembershipRevokedMessage(any());
    }

    @Test
    void updateParticipantExistentAsFedAdmin() throws Exception {

        MerlotParticipantDto dtoWithEdits = getMerlotParticipantDtoWithEdits();
        MerlotOrganizationCredentialSubject editedCredentialSubject =
            (MerlotOrganizationCredentialSubject) dtoWithEdits.getSelfDescription().getVerifiableCredential()
                .getCredentialSubject();
        MerlotParticipantMetaDto editedMetadata = dtoWithEdits.getMetadata();

        OrganizationRoleGrantedAuthority activeRole = new OrganizationRoleGrantedAuthority(OrganizationRole.FED_ADMIN, "did:web:example.com:participant:somefedorga");

        dtoWithEdits.setId("did:web:example.com:participant:someorga");
        MerlotParticipantDto participantDto = participantService.updateParticipant(dtoWithEdits, activeRole);

        // following attributes of the organization credential subject should have been updated
        MerlotOrganizationCredentialSubject updatedCredentialSubject =
            (MerlotOrganizationCredentialSubject) participantDto.getSelfDescription().getVerifiableCredential()
                .getCredentialSubject();
        assertEquals(updatedCredentialSubject.getTermsAndConditions().getContent(),
            editedCredentialSubject.getTermsAndConditions().getContent());
        assertEquals(updatedCredentialSubject.getTermsAndConditions().getHash(),
            editedCredentialSubject.getTermsAndConditions().getHash());
        assertEquals(updatedCredentialSubject.getLegalAddress().getStreetAddress(),
            editedCredentialSubject.getLegalAddress().getStreetAddress());
        assertEquals(updatedCredentialSubject.getLegalAddress().getLocality(),
            editedCredentialSubject.getLegalAddress().getLocality());
        assertEquals(updatedCredentialSubject.getLegalAddress().getCountryName(),
            editedCredentialSubject.getLegalAddress().getCountryName());
        assertEquals(updatedCredentialSubject.getLegalAddress().getPostalCode(),
            editedCredentialSubject.getLegalAddress().getPostalCode());
        assertEquals(updatedCredentialSubject.getOrgaName(),
            editedCredentialSubject.getOrgaName());
        assertEquals(updatedCredentialSubject.getLegalName(),
            editedCredentialSubject.getLegalName());
        assertEquals(updatedCredentialSubject.getRegistrationNumber().getLocal(),
            editedCredentialSubject.getRegistrationNumber().getLocal());
        assertEquals(updatedCredentialSubject.getRegistrationNumber().getEuid(),
            editedCredentialSubject.getRegistrationNumber().getEuid());
        assertEquals(updatedCredentialSubject.getRegistrationNumber().getEori(),
            editedCredentialSubject.getRegistrationNumber().getEori());
        assertEquals(updatedCredentialSubject.getRegistrationNumber().getVatId(),
            editedCredentialSubject.getRegistrationNumber().getVatId());

        // following attributes of the organization credential subject should not have been updated
        assertNotEquals(updatedCredentialSubject.getId(), editedCredentialSubject.getId());
        assertNotEquals(updatedCredentialSubject.getId(),
            editedCredentialSubject.getId());

        // following metadata of the organization should have been updated
        MerlotParticipantMetaDto updatedMetadata = participantDto.getMetadata();
        assertEquals(updatedMetadata.getMailAddress(), editedMetadata.getMailAddress());
        assertEquals(updatedMetadata.getMembershipClass(), editedMetadata.getMembershipClass());
        assertEquals(updatedMetadata.isActive(), editedMetadata.isActive());

        // following metadata of the organization should not have been updated
        assertEquals(0, updatedMetadata.getConnectors().size()); // no connector was added

        verify(outgoingMessageService, times(1)).sendOrganizationMembershipRevokedMessage(participantDto.getId());
    }

    @Test
    void updateParticipantNonExistent() {

        MerlotParticipantDto dtoWithEdits = getMerlotParticipantDtoWithEdits();
        dtoWithEdits.setId("did:web:example.com:participant:someunknownorga");
        dtoWithEdits.getSelfDescription().getVerifiableCredential().getCredentialSubject()
                .setId("did:web:example.com:participant:someunknownorga");

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
        String orgaId = "did:web:" + merlotDomain + ":participant:someorga";
        metaDto.setOrgaId(orgaId);
        metaDto.setMailAddress("mymail@example.com");
        metaDto.setMembershipClass(MembershipClass.FEDERATOR);
        List<MerlotParticipantMetaDto> list = new ArrayList<>();
        list.add(metaDto);

        lenient().when(organizationMetadataService.getParticipantsByMembershipClass(eq(MembershipClass.FEDERATOR))).thenReturn(list);

        List<MerlotParticipantDto> organizations = participantService.getFederators();
        assertThat(organizations, not(empty()));
        assertEquals(1, organizations.size());
        assertEquals(orgaId,
            organizations.get(0).getSelfDescription().getVerifiableCredential()
                .getCredentialSubject().getId());
    }

    @Test
    void createParticipantWithValidRegistrationFormAsFederator() throws Exception {
        MerlotParticipantDto participantDto = participantService.createParticipant(getTestRegistrationFormContent(),
                new OrganizationRoleGrantedAuthority(OrganizationRole.FED_ADMIN, "did:web:example.com:participant:somefedorga"));
        MerlotOrganizationCredentialSubject resultCredentialSubject = (MerlotOrganizationCredentialSubject)
                participantDto.getSelfDescription().getVerifiableCredential().getCredentialSubject();

        assertThat(resultCredentialSubject).usingRecursiveComparison().ignoringFields("id", "merlotId")
                .isEqualTo(getExpectedCredentialSubject());

        String id = resultCredentialSubject.getId();
        assertThat(id).isNotNull().isNotBlank();

        OrganizationMetadata metadataExpected = new OrganizationMetadata(id, mailAddress,
                MembershipClass.PARTICIPANT, true);

        ArgumentCaptor<MerlotParticipantMetaDto> varArgs = ArgumentCaptor.forClass(MerlotParticipantMetaDto.class);
        verify(organizationMetadataService, times(1)).saveMerlotParticipantMeta(varArgs.capture());
        assertEquals(metadataExpected.getOrgaId(), varArgs.getValue().getOrgaId());
        assertEquals(metadataExpected.getMailAddress(), varArgs.getValue().getMailAddress());
        assertEquals(metadataExpected.getMembershipClass(), varArgs.getValue().getMembershipClass());
        assertEquals(0,  varArgs.getValue().getConnectors().size());
    }

    @Test
    void createParticipantWithInvalidDid() throws Exception {
        RegistrationFormContent registrationFormContent = getTestRegistrationFormContent();
        registrationFormContent.setDidWeb("garbage");
        OrganizationRoleGrantedAuthority role =
                new OrganizationRoleGrantedAuthority(OrganizationRole.FED_ADMIN, "did:web:example.com:participant:somefedorga");
        Exception e = assertThrows(ResponseStatusException.class,
                () -> participantService.createParticipant(registrationFormContent, role));
        assertEquals("400 BAD_REQUEST \"Invalid registration form: Invalid did:web specified.\"", e.getMessage());
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
        content.setRegistrationNumberLocal("");
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

        String orgaId = "did:web:" + merlotDomain + "#someorga";
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


    private MerlotOrganizationCredentialSubject getTestEditedMerlotOrganizationCredentialSubject() {

        MerlotOrganizationCredentialSubject credentialSubject = new MerlotOrganizationCredentialSubject();
        credentialSubject.setId("did:web:changedorga.example.com");
        RegistrationNumber registrationNumber = new RegistrationNumber();
        registrationNumber.setLocal("changedLocal");
        registrationNumber.setEori("changedEori");
        registrationNumber.setEuid("changedEuid");
        registrationNumber.setVatId("changedVatId");
        credentialSubject.setRegistrationNumber(registrationNumber);
        VCard address = new VCard();
        address.setStreetAddress("changedAddress");
        address.setLocality("changedCity");
        address.setCountryName("changedCountry");
        address.setPostalCode("changedPostCode");
        credentialSubject.setLegalAddress(address);
        credentialSubject.setHeadquarterAddress(address);
        credentialSubject.setOrgaName("changedOrgaName");
        credentialSubject.setLegalName("changedLegalName");
        TermsAndConditions termsAndConditions = new TermsAndConditions();
        termsAndConditions.setContent("http://changed.com");
        termsAndConditions.setHash("changedHash");
        credentialSubject.setTermsAndConditions(termsAndConditions);
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

    private MerlotParticipantDto getMerlotParticipantDtoWithEdits() {

        MerlotParticipantDto dtoWithEdits = new MerlotParticipantDto();

        SelfDescription selfDescription = new SelfDescription();
        SelfDescriptionVerifiableCredential verifiableCredential = new SelfDescriptionVerifiableCredential();
        MerlotOrganizationCredentialSubject editedCredentialSubject = getTestEditedMerlotOrganizationCredentialSubject();

        verifiableCredential.setCredentialSubject(editedCredentialSubject);
        selfDescription.setVerifiableCredential(verifiableCredential);

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
        signerConfigDto.setPrivateKey("privateKey");
        signerConfigDto.setMerlotVerificationMethod("did:web:example.com:participant:someorga#merlot");
        signerConfigDto.setVerificationMethod("did:web:example.com:participant:someorga#somemethod");
        metaData.setOrganisationSignerConfigDto(signerConfigDto);

        dtoWithEdits.setSelfDescription(selfDescription);
        dtoWithEdits.setId(someOrgaId);
        dtoWithEdits.setMetadata(metaData);
        return dtoWithEdits;
    }
}

