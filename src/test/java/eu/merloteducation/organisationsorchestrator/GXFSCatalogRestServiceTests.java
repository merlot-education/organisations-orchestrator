package eu.merloteducation.organisationsorchestrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.authorizationlibrary.authorization.OrganizationRoleGrantedAuthority;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantMetaDto;
import eu.merloteducation.modelslib.gxfscatalog.datatypes.RegistrationNumber;
import eu.merloteducation.modelslib.gxfscatalog.datatypes.StringTypeValue;
import eu.merloteducation.modelslib.gxfscatalog.datatypes.TermsAndConditions;
import eu.merloteducation.modelslib.gxfscatalog.datatypes.VCard;
import eu.merloteducation.modelslib.gxfscatalog.participants.ParticipantItem;
import eu.merloteducation.modelslib.gxfscatalog.participants.PublicKey;
import eu.merloteducation.modelslib.gxfscatalog.selfdescriptions.SelfDescription;
import eu.merloteducation.modelslib.gxfscatalog.selfdescriptions.SelfDescriptionVerifiableCredential;
import eu.merloteducation.modelslib.gxfscatalog.selfdescriptions.participants.MerlotOrganizationCredentialSubject;
import eu.merloteducation.organisationsorchestrator.mappers.OrganizationMapper;
import eu.merloteducation.organisationsorchestrator.mappers.OrganizationMetadataMapper;
import eu.merloteducation.organisationsorchestrator.models.entities.MembershipClass;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganizationMetadata;
import eu.merloteducation.organisationsorchestrator.service.GXFSCatalogRestService;
import eu.merloteducation.organisationsorchestrator.service.KeycloakAuthService;
import eu.merloteducation.organisationsorchestrator.service.OrganisationMetadataService;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;

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
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA;
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
class GXFSCatalogRestServiceTests {

    @Autowired
    private OrganizationMapper organizationMapper;

    @Value("${gxfscatalog.participants-uri}")
    private String gxfscatalogParticipantsUri;

    @Value("${gxfscatalog.selfdescriptions-uri}")
    private String gxfscatalogSelfdescriptionsUri;

    @Value("${gxfscatalog.query-uri}")
    private String gxfscatalogQueryUri;

    @Autowired
    private GXFSCatalogRestService gxfsCatalogRestService;

    @MockBean
    private KeycloakAuthService keycloakAuthService;

    @MockBean
    OrganisationMetadataService organisationMetadataService;

    @Autowired
    private OrganizationMetadataMapper organizationMetadataMapper;

    private final String ID = "Participant:1234";

    private final String MAIL_ADDRESS = "test@test.de";

    private final String ORGANIZATION_LEGAL_NAME = "Organization Legal Name";

    private final String REGISTRATION_NUMBER = "DE123456789";

    private final String COUNTRY_CODE = "DE";

    private final String STREET = "Street 123";

    private final String PROVIDER_TNC_HASH = "hash1234567890";

    private final String PROVIDER_TNC_LINK = "abc.de";

    private final String CITY = "City";

    private final String ORGANIZATION_NAME = "Organization Name";

    private final String POSTAL_CODE = "12345";

    private String wrapSelfDescription(String selfDescription) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        SelfDescription<MerlotOrganizationCredentialSubject> sd = objectMapper.readValue(selfDescription,
            new TypeReference<>() {
            });
        ParticipantItem item = new ParticipantItem();
        item.setSelfDescription(sd);
        item.setId(sd.getVerifiableCredential().getCredentialSubject().getId());
        item.setName("name");
        item.setPublicKey(new PublicKey());
        return objectMapper.writeValueAsString(item);
    }

    MerlotOrganizationCredentialSubject getExpectedCredentialSubject() {

        Map<String, String> context = getContext();

        MerlotOrganizationCredentialSubject expected = new MerlotOrganizationCredentialSubject();
        expected.setOrgaName(new StringTypeValue(ORGANIZATION_NAME));
        expected.setLegalName(new StringTypeValue(ORGANIZATION_LEGAL_NAME));

        RegistrationNumber registrationNumberObj = new RegistrationNumber();
        registrationNumberObj.setType("gax-trust-framework:RegistrationNumber");
        registrationNumberObj.setLocal(new StringTypeValue(REGISTRATION_NUMBER));
        expected.setRegistrationNumber(registrationNumberObj);

        VCard vCard = new VCard();
        vCard.setLocality(new StringTypeValue(CITY));
        vCard.setPostalCode(new StringTypeValue(POSTAL_CODE));
        vCard.setCountryName(new StringTypeValue(COUNTRY_CODE));
        vCard.setStreetAddress(new StringTypeValue(STREET));
        vCard.setType("vcard:Address");
        expected.setLegalAddress(vCard);
        expected.setHeadquarterAddress(vCard);

        TermsAndConditions termsAndConditions = new TermsAndConditions();
        termsAndConditions.setContent(new StringTypeValue(PROVIDER_TNC_LINK, "xsd:anyURI"));
        termsAndConditions.setHash(new StringTypeValue(PROVIDER_TNC_HASH));
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

    PDDocument getTestRegistrationDocument() throws IOException {

        PDDocument pdDocument = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        pdDocument.addPage(page);

        PDAcroForm form = new PDAcroForm(pdDocument);
        pdDocument.getDocumentCatalog().setAcroForm(form);

        PDFont font = new PDType1Font(HELVETICA);
        PDResources resources = new PDResources();
        resources.put(COSName.getPDFName("Helv"), font);
        form.setDefaultResources(resources);

        String defaultAppearance = "/Helv 12 Tf 0 0 1 rg";

        PDTextField textField = new PDTextField(form);
        textField.setPartialName("MailAddress");
        textField.setDefaultAppearance(defaultAppearance);
        form.getFields().add(textField);
        textField.setValue(MAIL_ADDRESS);

        PDTextField textField1 = new PDTextField(form);
        textField1.setPartialName("OrganizationLegalName");
        form.getFields().add(textField1);
        textField1.setDefaultAppearance(defaultAppearance);
        textField1.setValue(ORGANIZATION_LEGAL_NAME);

        PDTextField textField2 = new PDTextField(form);
        textField2.setPartialName("RegistrationNumber");
        form.getFields().add(textField2);
        textField2.setDefaultAppearance(defaultAppearance);
        textField2.setValue(REGISTRATION_NUMBER);

        PDTextField textField3 = new PDTextField(form);
        textField3.setPartialName("CountryCode");
        form.getFields().add(textField3);
        textField3.setDefaultAppearance(defaultAppearance);
        textField3.setValue(COUNTRY_CODE);

        PDTextField textField4 = new PDTextField(form);
        textField4.setPartialName("OrganizationName");
        form.getFields().add(textField4);
        textField4.setDefaultAppearance(defaultAppearance);
        textField4.setValue(ORGANIZATION_NAME);

        PDTextField textField5 = new PDTextField(form);
        textField5.setPartialName("PostalCode");
        form.getFields().add(textField5);
        textField5.setDefaultAppearance(defaultAppearance);
        textField5.setValue(POSTAL_CODE);

        PDTextField textField7 = new PDTextField(form);
        textField7.setPartialName("City");
        form.getFields().add(textField7);
        textField7.setDefaultAppearance(defaultAppearance);
        textField7.setValue(CITY);

        PDTextField textField8 = new PDTextField(form);
        textField8.setPartialName("ProviderTncLink");
        form.getFields().add(textField8);
        textField8.setDefaultAppearance(defaultAppearance);
        textField8.setValue(PROVIDER_TNC_LINK);

        PDTextField textField9 = new PDTextField(form);
        textField9.setPartialName("Street");
        form.getFields().add(textField9);
        textField9.setDefaultAppearance(defaultAppearance);
        textField9.setValue(STREET);

        PDTextField textField10 = new PDTextField(form);
        textField10.setPartialName("ProviderTncHash");
        form.getFields().add(textField10);
        textField10.setDefaultAppearance(defaultAppearance);
        textField10.setValue(PROVIDER_TNC_HASH);
        return pdDocument;
    }

    @BeforeEach
    public void setUp() {

        ReflectionTestUtils.setField(gxfsCatalogRestService, "organizationMapper", organizationMapper);
        ReflectionTestUtils.setField(gxfsCatalogRestService, "keycloakAuthService", keycloakAuthService);
        ReflectionTestUtils.setField(gxfsCatalogRestService, "organizationMetadataMapper", organizationMetadataMapper);
        ReflectionTestUtils.setField(gxfsCatalogRestService, "organisationMetadataService",
            organisationMetadataService);

        lenient().when(keycloakAuthService.webCallAuthenticated(eq(HttpMethod.GET), any(), any(), any()))
            .thenThrow(HttpClientErrorException.NotFound.class);

        String mockParticipant = """
            {
                "id": "Participant:10",
                "name": "Participant:10",
                "publicKey": "{\\"kty\\":\\"RSA\\",\\"e\\":\\"AQAB\\",\\"alg\\":\\"PS256\\",\\"n\\":\\"dummy\\"}",
                "selfDescription":"{\\"@id\\": \\"http://example.edu/verifiablePresentation/self-description1\\", \\"proof\\": {\\"created\\": \\"2023-04-27T13:48:11Z\\", \\"jws\\": \\"dummy\\", \\"proofPurpose\\": \\"assertionMethod\\", \\"type\\": \\"JsonWebSignature2020\\", \\"verificationMethod\\": \\"did:web:compliance.lab.gaia-x.eu\\"}, \\"type\\": [\\"VerifiablePresentation\\"], \\"@context\\": [\\"https://www.w3.org/2018/credentials/v1\\"], \\"verifiableCredential\\": {\\"credentialSubject\\": {\\"gax-trust-framework:registrationNumber\\": {\\"gax-trust-framework:local\\": {\\"@value\\": \\"0762747721\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"gax-trust-framework:RegistrationNumber\\"}, \\"gax-trust-framework:legalName\\": {\\"@value\\": \\"Gaia-X European Association for Data and Cloud AISBL\\", \\"@type\\": \\"xsd:string\\"}, \\"gax-trust-framework:headquarterAddress\\": {\\"vcard:country-name\\": {\\"@value\\": \\"BE\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"vcard:Address\\", \\"vcard:street-address\\": {\\"@value\\": \\"Avenue des Arts 6-9\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:locality\\": {\\"@value\\": \\"Brüssel\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:postal-code\\": {\\"@value\\": \\"1210\\", \\"@type\\": \\"xsd:string\\"}}, \\"merlot:termsAndConditions\\": {\\"gax-trust-framework:content\\": {\\"@value\\": \\"http://example.com\\", \\"@type\\": \\"xsd:anyURI\\"}, \\"gax-trust-framework:hash\\": {\\"@value\\": \\"hash1234\\", \\"@type\\": \\"xsd:string\\"}}, \\"@type\\": \\"merlot:MerlotOrganization\\", \\"merlot:merlotId\\": {\\"@value\\": \\"10\\", \\"@type\\": \\"xsd:string\\"}, \\"gax-trust-framework:legalAddress\\": {\\"vcard:country-name\\": {\\"@value\\": \\"BE\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"vcard:Address\\", \\"vcard:street-address\\": {\\"@value\\": \\"Avenue des Arts 6-9\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:locality\\": {\\"@value\\": \\"Brüssel\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:postal-code\\": {\\"@value\\": \\"1210\\", \\"@type\\": \\"xsd:string\\"}}, \\"merlot:orgaName\\": {\\"@value\\": \\"Gaia-X AISBL\\", \\"@type\\": \\"xsd:string\\"}, \\"@id\\": \\"Participant:10\\", \\"@context\\": {\\"merlot\\": \\"http://w3id.org/gaia-x/merlot#\\", \\"gax-trust-framework\\": \\"http://w3id.org/gaia-x/gax-trust-framework#\\", \\"rdf\\": \\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\\", \\"sh\\": \\"http://www.w3.org/ns/shacl#\\", \\"xsd\\": \\"http://www.w3.org/2001/XMLSchema#\\", \\"gax-validation\\": \\"http://w3id.org/gaia-x/validation#\\", \\"skos\\": \\"http://www.w3.org/2004/02/skos/core#\\", \\"vcard\\": \\"http://www.w3.org/2006/vcard/ns#\\"}}, \\"issuanceDate\\": \\"2022-10-19T18:48:09Z\\", \\"@type\\": [\\"VerifiableCredential\\"], \\"@id\\": \\"https://www.example.org/legalPerson.json\\", \\"proof\\": {\\"created\\": \\"2023-04-27T13:48:10Z\\", \\"jws\\": \\"dummy\\", \\"proofPurpose\\": \\"assertionMethod\\", \\"type\\": \\"JsonWebSignature2020\\", \\"verificationMethod\\": \\"did:web:compliance.lab.gaia-x.eu\\"}, \\"@context\\": [\\"https://www.w3.org/2018/credentials/v1\\"], \\"issuer\\": \\"Participant:10\\"}}"
            }
            """;
        mockParticipant = StringEscapeUtils.unescapeJson(mockParticipant);
        if (mockParticipant != null)
            mockParticipant = mockParticipant.replace("\"{", "{").replace("}\"", "}");

        String mockUserResponse = """
            {
                "totalCount": 1,
                "items": [
                    {
                         "meta": {
                             "expirationTime": null,
                             "content": "{\\"@id\\": \\"http://example.edu/verifiablePresentation/self-description1\\", \\"proof\\": {\\"created\\": \\"2023-08-30T08:58:35Z\\", \\"jws\\": \\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..JENTxPd26Ke05vIjtCzMESUvla_iYqP00ppsJfKagE06-XegrCbgRFoty20Tf40tPCd9_VflRL3kW12VCoOlDPA2nc21jaa_vmv8ZCCFNBmXIJVrBmF370MdyRT53Z-TGPKoUv5iF0m5fibKqqtg8MMCNVG9J3eff-Q04Wc5jZTgq2a9mjRsuZUAcnmu6ZgO4aaCKPD1t2aI3pZpie5zk5RJ37ZezuYQa7zdRirq_8Qaa9acg-aVqLaGxFAJhcpOcck-zkaP52pxVCusLt2bVUSG6HVk9txwCoc8ZoGCXW27MN8SM3I5PwfD_3OXGvs4TR0j-9ylKSajwWYRclNDtJMSBhmtXu_wjrjDMZFG2kRow_p1xhZZ71DKlX2Efp6VAdSWYbPpguZv1qMYbBemC3DW2lhkOsk1_KkwICO3ZSySNEswsjDty3NuGUOZtyyvImbSZ5f3I7ZyMNvnL1xoYEteK6mBSB9H7Zr1E1yZr7K1eiXR2MQuxKaFYl6jikYuwpdyrD6lvOWCEKOBQ_yjaQ9lbySiOxbNykpOX6-Bbu6mVQIX08BEzg0Y8r0Bnce2KPWypMtyHW7KhVgok2aLIjFQGutG7pgeIXIK2mPIR5jxUWUUh3XDuuU21cDYbMb6wYNX_-sNHNsots-mA81kRPlSRWlXBkvsZffXo6bWhKQ\\", \\"proofPurpose\\": \\"assertionMethod\\", \\"type\\": \\"JsonWebSignature2020\\", \\"verificationMethod\\": \\"did:web:compliance.lab.gaia-x.eu\\"}, \\"type\\": [\\"VerifiablePresentation\\"], \\"@context\\": [\\"https://www.w3.org/2018/credentials/v1\\"], \\"verifiableCredential\\": {\\"credentialSubject\\": {\\"gax-trust-framework:registrationNumber\\": {\\"gax-trust-framework:local\\": {\\"@value\\": \\"0762747721\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"gax-trust-framework:RegistrationNumber\\"}, \\"gax-trust-framework:legalName\\": {\\"@value\\": \\"Gaia-X European Association for Data and Cloud AISBL\\", \\"@type\\": \\"xsd:string\\"}, \\"gax-trust-framework:headquarterAddress\\": {\\"vcard:country-name\\": {\\"@value\\": \\"BE\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"vcard:Address\\", \\"vcard:street-address\\": {\\"@value\\": \\"Avenue des Arts 6-9\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:locality\\": {\\"@value\\": \\"Br\\\\u00fcssel\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:postal-code\\": {\\"@value\\": \\"1210\\", \\"@type\\": \\"xsd:string\\"}}, \\"@type\\": \\"merlot:MerlotOrganization\\", \\"merlot:merlotId\\": {\\"@value\\": \\"10\\", \\"@type\\": \\"xsd:string\\"}, \\"gax-trust-framework:legalAddress\\": {\\"vcard:country-name\\": {\\"@value\\": \\"BE\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"vcard:Address\\", \\"vcard:street-address\\": {\\"@value\\": \\"Avenue des Arts 6-9\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:locality\\": {\\"@value\\": \\"Br\\\\u00fcssel\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:postal-code\\": {\\"@value\\": \\"1210\\", \\"@type\\": \\"xsd:string\\"}}, \\"merlot:orgaName\\": {\\"@value\\": \\"Gaia-X AISBL\\", \\"@type\\": \\"xsd:string\\"}, \\"@id\\": \\"Participant:10\\", \\"@context\\": {\\"merlot\\": \\"http://w3id.org/gaia-x/merlot#\\", \\"gax-trust-framework\\": \\"http://w3id.org/gaia-x/gax-trust-framework#\\", \\"rdf\\": \\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\\", \\"sh\\": \\"http://www.w3.org/ns/shacl#\\", \\"xsd\\": \\"http://www.w3.org/2001/XMLSchema#\\", \\"gax-validation\\": \\"http://w3id.org/gaia-x/validation#\\", \\"skos\\": \\"http://www.w3.org/2004/02/skos/core#\\", \\"vcard\\": \\"http://www.w3.org/2006/vcard/ns#\\"}, \\"merlot:termsAndConditions\\": {\\"gax-trust-framework:content\\": {\\"@value\\": \\"http://example.com\\", \\"@type\\": \\"xsd:anyURI\\"}, \\"@type\\": \\"gax-trust-framework:TermsAndConditions\\", \\"gax-trust-framework:hash\\": {\\"@value\\": \\"hash1234\\", \\"@type\\": \\"xsd:string\\"}}}, \\"issuanceDate\\": \\"2022-10-19T18:48:09Z\\", \\"@type\\": [\\"VerifiableCredential\\"], \\"@id\\": \\"https://www.example.org/legalPerson.json\\", \\"proof\\": {\\"created\\": \\"2023-08-30T08:58:34Z\\", \\"jws\\": \\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..yV2y2TdkrLpsrP1ZTmtEjazcRMtKKwEHviNx_cC3BxBM8R2DeAbBuxbgcO_3ZoUeuB-6laSe2RN6jIp5UMCVRzgFg1YIbNKMcyDfC2LhF0YaXxo9pB-L3qLdRJpve3-NBHj76dBUW4Q04S_4t_5M09p61fEuCRJIIrDzh2iKSLwzKrv2sT8hnMefN2P29sa5QlJrRu-kFHolq_ZmwsXzWMN3R8_8tbsS19eP1hIzkuzW1lla8jZot06Y6bFslr3S5CCbexABJb34puu2nbH2n4Qdc9BS31B34HnduC8AuKEbOfmWsGDSZT29QjL-VxUWN4lhqxb-DsiSpmDlEPt_UzJah5tvMSQzAlKpm2ZZdBuQb8Mk9-U9oRmrxm6xeOcXdcBMAHXEYlBMp6R8gEOyQ3uDMrR2x9xMTs4EeJgJlOSsyK7F5_EbMtqnLulKRD4RtNoZ_I8k0XcVZAVoBtxrEwWOE48AdW16yfemqEO8s8J_J9TrBaTTMKIMFqJjJ-HNc9n7E_saylVFRadHevbLLuBNDOjjwvI8E5r55iO2HTPxB1dSWcjidSacSCvo2zQxRLkbPQJmLp2S4SCMLbqwPdph8KH6tfAcgxH0k3sTmwvt2tTLBXCINPlnhv2ahuHzXWGpgegEyHLrtlUAwfeDilkc_lib_chWBVqqWxu-7Gw\\", \\"proofPurpose\\": \\"assertionMethod\\", \\"type\\": \\"JsonWebSignature2020\\", \\"verificationMethod\\": \\"did:web:compliance.lab.gaia-x.eu\\"}, \\"@context\\": [\\"https://www.w3.org/2018/credentials/v1\\"], \\"issuer\\": \\"Participant:10\\"}}",
                             "subjectId": "Participant:10",
                             "validators": [
                                 "did:web:compliance.lab.gaia-x.eu"
                             ],
                             "sdHash": "8b143ff8e0cf8f22c366cea9e1d31d97f79aa29eee5741f048637a43b7f059b0",
                             "id": "Participant:10",
                             "status": "active",
                             "issuer": "Participant:10",
                             "validatorDids": [
                                 "did:web:compliance.lab.gaia-x.eu"
                             ],
                             "uploadDatetime": "2023-08-30T08:58:35.894486Z",
                             "statusDatetime": "2023-08-30T08:58:35.894486Z"
                         },
                         "content": "{\\"@id\\": \\"http://example.edu/verifiablePresentation/self-description1\\", \\"proof\\": {\\"created\\": \\"2023-08-30T08:58:35Z\\", \\"jws\\": \\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..JENTxPd26Ke05vIjtCzMESUvla_iYqP00ppsJfKagE06-XegrCbgRFoty20Tf40tPCd9_VflRL3kW12VCoOlDPA2nc21jaa_vmv8ZCCFNBmXIJVrBmF370MdyRT53Z-TGPKoUv5iF0m5fibKqqtg8MMCNVG9J3eff-Q04Wc5jZTgq2a9mjRsuZUAcnmu6ZgO4aaCKPD1t2aI3pZpie5zk5RJ37ZezuYQa7zdRirq_8Qaa9acg-aVqLaGxFAJhcpOcck-zkaP52pxVCusLt2bVUSG6HVk9txwCoc8ZoGCXW27MN8SM3I5PwfD_3OXGvs4TR0j-9ylKSajwWYRclNDtJMSBhmtXu_wjrjDMZFG2kRow_p1xhZZ71DKlX2Efp6VAdSWYbPpguZv1qMYbBemC3DW2lhkOsk1_KkwICO3ZSySNEswsjDty3NuGUOZtyyvImbSZ5f3I7ZyMNvnL1xoYEteK6mBSB9H7Zr1E1yZr7K1eiXR2MQuxKaFYl6jikYuwpdyrD6lvOWCEKOBQ_yjaQ9lbySiOxbNykpOX6-Bbu6mVQIX08BEzg0Y8r0Bnce2KPWypMtyHW7KhVgok2aLIjFQGutG7pgeIXIK2mPIR5jxUWUUh3XDuuU21cDYbMb6wYNX_-sNHNsots-mA81kRPlSRWlXBkvsZffXo6bWhKQ\\", \\"proofPurpose\\": \\"assertionMethod\\", \\"type\\": \\"JsonWebSignature2020\\", \\"verificationMethod\\": \\"did:web:compliance.lab.gaia-x.eu\\"}, \\"type\\": [\\"VerifiablePresentation\\"], \\"@context\\": [\\"https://www.w3.org/2018/credentials/v1\\"], \\"verifiableCredential\\": {\\"credentialSubject\\": {\\"gax-trust-framework:registrationNumber\\": {\\"gax-trust-framework:local\\": {\\"@value\\": \\"0762747721\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"gax-trust-framework:RegistrationNumber\\"}, \\"gax-trust-framework:legalName\\": {\\"@value\\": \\"Gaia-X European Association for Data and Cloud AISBL\\", \\"@type\\": \\"xsd:string\\"}, \\"gax-trust-framework:headquarterAddress\\": {\\"vcard:country-name\\": {\\"@value\\": \\"BE\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"vcard:Address\\", \\"vcard:street-address\\": {\\"@value\\": \\"Avenue des Arts 6-9\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:locality\\": {\\"@value\\": \\"Br\\\\u00fcssel\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:postal-code\\": {\\"@value\\": \\"1210\\", \\"@type\\": \\"xsd:string\\"}}, \\"@type\\": \\"merlot:MerlotOrganization\\", \\"merlot:merlotId\\": {\\"@value\\": \\"10\\", \\"@type\\": \\"xsd:string\\"}, \\"gax-trust-framework:legalAddress\\": {\\"vcard:country-name\\": {\\"@value\\": \\"BE\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"vcard:Address\\", \\"vcard:street-address\\": {\\"@value\\": \\"Avenue des Arts 6-9\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:locality\\": {\\"@value\\": \\"Br\\\\u00fcssel\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:postal-code\\": {\\"@value\\": \\"1210\\", \\"@type\\": \\"xsd:string\\"}}, \\"merlot:orgaName\\": {\\"@value\\": \\"Gaia-X AISBL\\", \\"@type\\": \\"xsd:string\\"}, \\"@id\\": \\"Participant:10\\", \\"@context\\": {\\"merlot\\": \\"http://w3id.org/gaia-x/merlot#\\", \\"gax-trust-framework\\": \\"http://w3id.org/gaia-x/gax-trust-framework#\\", \\"rdf\\": \\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\\", \\"sh\\": \\"http://www.w3.org/ns/shacl#\\", \\"xsd\\": \\"http://www.w3.org/2001/XMLSchema#\\", \\"gax-validation\\": \\"http://w3id.org/gaia-x/validation#\\", \\"skos\\": \\"http://www.w3.org/2004/02/skos/core#\\", \\"vcard\\": \\"http://www.w3.org/2006/vcard/ns#\\"}, \\"merlot:termsAndConditions\\": {\\"gax-trust-framework:content\\": {\\"@value\\": \\"http://example.com\\", \\"@type\\": \\"xsd:anyURI\\"}, \\"@type\\": \\"gax-trust-framework:TermsAndConditions\\", \\"gax-trust-framework:hash\\": {\\"@value\\": \\"hash1234\\", \\"@type\\": \\"xsd:string\\"}}}, \\"issuanceDate\\": \\"2022-10-19T18:48:09Z\\", \\"@type\\": [\\"VerifiableCredential\\"], \\"@id\\": \\"https://www.example.org/legalPerson.json\\", \\"proof\\": {\\"created\\": \\"2023-08-30T08:58:34Z\\", \\"jws\\": \\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..yV2y2TdkrLpsrP1ZTmtEjazcRMtKKwEHviNx_cC3BxBM8R2DeAbBuxbgcO_3ZoUeuB-6laSe2RN6jIp5UMCVRzgFg1YIbNKMcyDfC2LhF0YaXxo9pB-L3qLdRJpve3-NBHj76dBUW4Q04S_4t_5M09p61fEuCRJIIrDzh2iKSLwzKrv2sT8hnMefN2P29sa5QlJrRu-kFHolq_ZmwsXzWMN3R8_8tbsS19eP1hIzkuzW1lla8jZot06Y6bFslr3S5CCbexABJb34puu2nbH2n4Qdc9BS31B34HnduC8AuKEbOfmWsGDSZT29QjL-VxUWN4lhqxb-DsiSpmDlEPt_UzJah5tvMSQzAlKpm2ZZdBuQb8Mk9-U9oRmrxm6xeOcXdcBMAHXEYlBMp6R8gEOyQ3uDMrR2x9xMTs4EeJgJlOSsyK7F5_EbMtqnLulKRD4RtNoZ_I8k0XcVZAVoBtxrEwWOE48AdW16yfemqEO8s8J_J9TrBaTTMKIMFqJjJ-HNc9n7E_saylVFRadHevbLLuBNDOjjwvI8E5r55iO2HTPxB1dSWcjidSacSCvo2zQxRLkbPQJmLp2S4SCMLbqwPdph8KH6tfAcgxH0k3sTmwvt2tTLBXCINPlnhv2ahuHzXWGpgegEyHLrtlUAwfeDilkc_lib_chWBVqqWxu-7Gw\\", \\"proofPurpose\\": \\"assertionMethod\\", \\"type\\": \\"JsonWebSignature2020\\", \\"verificationMethod\\": \\"did:web:compliance.lab.gaia-x.eu\\"}, \\"@context\\": [\\"https://www.w3.org/2018/credentials/v1\\"], \\"issuer\\": \\"Participant:10\\"}}"
                    }
                ]
            }
            """;

        mockUserResponse = StringEscapeUtils.unescapeJson(mockUserResponse);
        if (mockUserResponse != null)
            mockUserResponse = mockUserResponse.replace("\"{", "{").replace("}\"", "}");

        String mockQueryResponse = """
            {
                "totalCount": 1,
                "items": [
                    {
                        "p.uri": "Participant:10"
                    }
                ]
            }
            """;
        mockQueryResponse = StringEscapeUtils.unescapeJson(mockQueryResponse);
        if (mockQueryResponse != null)
            mockQueryResponse = mockQueryResponse.replace("\"{", "{").replace("}\"", "}");

        // return a dummy list of one item
        lenient().when(
            keycloakAuthService.webCallAuthenticated(eq(HttpMethod.POST), startsWith(gxfscatalogQueryUri), any(),
                any())).thenReturn(mockQueryResponse);
        lenient().when(
            keycloakAuthService.webCallAuthenticated(eq(HttpMethod.GET), startsWith(gxfscatalogSelfdescriptionsUri),
                any(), any())).thenReturn(mockUserResponse);
        lenient().when(keycloakAuthService.webCallAuthenticated(eq(HttpMethod.GET),
                eq(URI.create(gxfscatalogParticipantsUri + "/Participant:10").toString()), any(), any()))
            .thenReturn(mockParticipant);
        lenient().when(keycloakAuthService.webCallAuthenticated(eq(HttpMethod.PUT),
                eq(URI.create(gxfscatalogParticipantsUri + "/Participant:10").toString()), anyString(), any()))
            .thenAnswer(i -> wrapSelfDescription((String) i.getArguments()[2]));
        lenient().when(keycloakAuthService.webCallAuthenticated(eq(HttpMethod.POST),
                eq(URI.create(gxfscatalogParticipantsUri).toString()), anyString(), any()))
            .thenAnswer(i -> wrapSelfDescription((String) i.getArguments()[2]));

        MerlotParticipantMetaDto metaDto = new MerlotParticipantMetaDto();
        metaDto.setOrgaId("10");
        metaDto.setMailAddress("mymail@example.com");
        metaDto.setMembershipClass("PARTICIPANT");

        lenient().when(organisationMetadataService.getMerlotParticipantMetaDto(eq("10"))).thenReturn(metaDto);
        lenient().when(organisationMetadataService.updateMerlotParticipantMeta(any())).thenAnswer(i -> i.getArguments()[0]);
    }

    @Test
    void getAllParticipants() throws Exception {

        Page<MerlotParticipantDto> organizations = gxfsCatalogRestService.getParticipants(PageRequest.of(0, 9));
        assertThat(organizations.getContent(), isA(List.class));
        assertThat(organizations.getContent(), not(empty()));
        assertEquals(1, organizations.getContent().size());

        String merlotId = organizations.getContent().get(0).getSelfDescription().getVerifiableCredential()
            .getCredentialSubject().getMerlotId().getValue();
        assertEquals("10", merlotId);

        String mailAddress = organizations.getContent().get(0).getMetadata().getMailAddress();
        assertEquals("mymail@example.com", mailAddress);

        String membershipClass = organizations.getContent().get(0).getMetadata().getMembershipClass();
        assertEquals("PARTICIPANT", membershipClass);
    }

    @Test
    void getParticipantById() throws Exception {

        MerlotParticipantDto organization = gxfsCatalogRestService.getParticipantById("10");
        assertThat(organization, isA(MerlotParticipantDto.class));
        assertEquals("10",
            organization.getSelfDescription().getVerifiableCredential().getCredentialSubject().getMerlotId()
                .getValue());
        assertEquals("Gaia-X European Association for Data and Cloud AISBL",
            organization.getSelfDescription().getVerifiableCredential().getCredentialSubject().getLegalName()
                .getValue());

        String mailAddress = organization.getMetadata().getMailAddress();
        assertEquals("mymail@example.com", mailAddress);

        String membershipClass = organization.getMetadata().getMembershipClass();
        assertEquals("PARTICIPANT", membershipClass);
    }

    @Test
    void getParticipantByInvalidId() {

        assertThrows(IllegalArgumentException.class, () -> gxfsCatalogRestService.getParticipantById("asdf"));
    }

    @Test
    void getParticipantByNonexistentId() {

        assertThrows(HttpClientErrorException.NotFound.class, () -> gxfsCatalogRestService.getParticipantById("11"));
    }

    @Test
    void updateParticipantExistentAsParticipant() throws Exception {

        MerlotParticipantDto participantDtoWithEdits = getMerlotParticipantDtoWithEdits();
        MerlotOrganizationCredentialSubject editedCredentialSubject = participantDtoWithEdits.getSelfDescription()
            .getVerifiableCredential().getCredentialSubject();
        MerlotParticipantMetaDto editedMetadata = participantDtoWithEdits.getMetadata();

        OrganizationRoleGrantedAuthority activeRole = new OrganizationRoleGrantedAuthority("OrgLegRep_10");

        MerlotParticipantDto updatedParticipantDto = gxfsCatalogRestService.updateParticipant(participantDtoWithEdits, activeRole, "10");

        // following attributes of the organization credential subject should have been updated
        MerlotOrganizationCredentialSubject updatedCredentialSubject = updatedParticipantDto.getSelfDescription()
            .getVerifiableCredential().getCredentialSubject();
        assertEquals(updatedCredentialSubject.getTermsAndConditions().getContent().getValue(),
            editedCredentialSubject.getTermsAndConditions().getContent().getValue());
        assertEquals(updatedCredentialSubject.getTermsAndConditions().getHash().getValue(),
            editedCredentialSubject.getTermsAndConditions().getHash().getValue());
        assertEquals(updatedCredentialSubject.getLegalAddress().getStreetAddress().getValue(),
            editedCredentialSubject.getLegalAddress().getStreetAddress().getValue());
        assertEquals(updatedCredentialSubject.getLegalAddress().getLocality().getValue(),
            editedCredentialSubject.getLegalAddress().getLocality().getValue());
        assertEquals(updatedCredentialSubject.getLegalAddress().getCountryName().getValue(),
            editedCredentialSubject.getLegalAddress().getCountryName().getValue());
        assertEquals(updatedCredentialSubject.getLegalAddress().getPostalCode().getValue(),
            editedCredentialSubject.getLegalAddress().getPostalCode().getValue());

        // following attributes of the organization credential subject should not have been updated
        assertNotEquals(updatedCredentialSubject.getId(), editedCredentialSubject.getId());
        assertNotEquals(updatedCredentialSubject.getMerlotId().getValue(),
            editedCredentialSubject.getMerlotId().getValue());
        assertNotEquals(updatedCredentialSubject.getOrgaName().getValue(),
            editedCredentialSubject.getOrgaName().getValue());
        assertNotEquals(updatedCredentialSubject.getLegalName().getValue(),
            editedCredentialSubject.getLegalName().getValue());
        assertNotEquals(updatedCredentialSubject.getRegistrationNumber().getLocal().getValue(),
            editedCredentialSubject.getRegistrationNumber().getLocal().getValue());
        assertNull(updatedCredentialSubject.getRegistrationNumber().getEuid());
        assertNull(updatedCredentialSubject.getRegistrationNumber().getEori());
        assertNull(updatedCredentialSubject.getRegistrationNumber().getVatId());

        // following metadata of the organization should have been updated
        MerlotParticipantMetaDto updatedMetadata = updatedParticipantDto.getMetadata();
        assertEquals(updatedMetadata.getMailAddress(), editedMetadata.getMailAddress());

        // following metadata of the organization should not have been updated
        assertNotEquals(updatedMetadata.getMembershipClass(), editedMetadata.getMembershipClass());
        assertNotEquals(updatedMetadata.getOrgaId(), editedMetadata.getOrgaId());
    }

    @Test
    void updateParticipantExistentAsFedAdmin() throws Exception {

        MerlotParticipantDto dtoWithEdits = getMerlotParticipantDtoWithEdits();
        MerlotOrganizationCredentialSubject editedCredentialSubject = dtoWithEdits.getSelfDescription()
            .getVerifiableCredential().getCredentialSubject();
        MerlotParticipantMetaDto editedMetadata = dtoWithEdits.getMetadata();

        OrganizationRoleGrantedAuthority activeRole = new OrganizationRoleGrantedAuthority("FedAdmin_10");

        MerlotParticipantDto participantDto = gxfsCatalogRestService.updateParticipant(dtoWithEdits, activeRole, "10");

        // following attributes of the organization credential subject should have been updated
        MerlotOrganizationCredentialSubject updatedCredentialSubject = participantDto.getSelfDescription()
            .getVerifiableCredential().getCredentialSubject();
        assertEquals(updatedCredentialSubject.getTermsAndConditions().getContent().getValue(),
            editedCredentialSubject.getTermsAndConditions().getContent().getValue());
        assertEquals(updatedCredentialSubject.getTermsAndConditions().getHash().getValue(),
            editedCredentialSubject.getTermsAndConditions().getHash().getValue());
        assertEquals(updatedCredentialSubject.getLegalAddress().getStreetAddress().getValue(),
            editedCredentialSubject.getLegalAddress().getStreetAddress().getValue());
        assertEquals(updatedCredentialSubject.getLegalAddress().getLocality().getValue(),
            editedCredentialSubject.getLegalAddress().getLocality().getValue());
        assertEquals(updatedCredentialSubject.getLegalAddress().getCountryName().getValue(),
            editedCredentialSubject.getLegalAddress().getCountryName().getValue());
        assertEquals(updatedCredentialSubject.getLegalAddress().getPostalCode().getValue(),
            editedCredentialSubject.getLegalAddress().getPostalCode().getValue());
        assertEquals(updatedCredentialSubject.getOrgaName().getValue(),
            editedCredentialSubject.getOrgaName().getValue());
        assertEquals(updatedCredentialSubject.getLegalName().getValue(),
            editedCredentialSubject.getLegalName().getValue());
        assertEquals(updatedCredentialSubject.getRegistrationNumber().getLocal().getValue(),
            editedCredentialSubject.getRegistrationNumber().getLocal().getValue());
        assertEquals(updatedCredentialSubject.getRegistrationNumber().getEuid().getValue(),
            editedCredentialSubject.getRegistrationNumber().getEuid().getValue());
        assertEquals(updatedCredentialSubject.getRegistrationNumber().getEori().getValue(),
            editedCredentialSubject.getRegistrationNumber().getEori().getValue());
        assertEquals(updatedCredentialSubject.getRegistrationNumber().getVatId().getValue(),
            editedCredentialSubject.getRegistrationNumber().getVatId().getValue());

        // following attributes of the organization credential subject should not have been updated
        assertNotEquals(updatedCredentialSubject.getId(), editedCredentialSubject.getId());
        assertNotEquals(updatedCredentialSubject.getMerlotId().getValue(),
            editedCredentialSubject.getMerlotId().getValue());

        // following metadata of the organization should have been updated
        MerlotParticipantMetaDto updatedMetadata = participantDto.getMetadata();
        assertEquals(updatedMetadata.getMailAddress(), editedMetadata.getMailAddress());
        assertEquals(updatedMetadata.getMembershipClass(), editedMetadata.getMembershipClass());

        // following metadata of the organization should not have been updated
        assertNotEquals(updatedMetadata.getOrgaId(), editedMetadata.getOrgaId());
    }

    @Test
    void updateParticipantNonExistent() {

        MerlotParticipantDto dtoWithEdits = getMerlotParticipantDtoWithEdits();

        OrganizationRoleGrantedAuthority activeRole = new OrganizationRoleGrantedAuthority("FedAdmin_10");

        assertThrows(HttpClientErrorException.NotFound.class,
            () -> gxfsCatalogRestService.updateParticipant(dtoWithEdits, activeRole, "11"));
    }

    @Test
    void getFederatorsNoFederatorsExisting() throws Exception {

        List<MerlotParticipantDto> organizations = gxfsCatalogRestService.getFederators();
        assertThat(organizations, empty());
    }

    @Test
    void getFederators() throws Exception {

        MerlotParticipantMetaDto metaDto = new MerlotParticipantMetaDto();
        metaDto.setOrgaId("10");
        metaDto.setMailAddress("mymail@example.com");
        metaDto.setMembershipClass("FEDERATOR");

        lenient().when(organisationMetadataService.getMerlotParticipantMetaDto(eq("10"))).thenReturn(metaDto);

        List<MerlotParticipantDto> organizations = gxfsCatalogRestService.getFederators();
        assertThat(organizations, not(empty()));
        assertEquals(1, organizations.size());
        assertEquals("10",
            organizations.get(0).getSelfDescription().getVerifiableCredential().getCredentialSubject().getMerlotId()
                .getValue());
    }

    @Test
    void createParticipantWithValidRegistrationForm() throws Exception {

        MerlotParticipantDto participantDto = gxfsCatalogRestService.createParticipant(getTestRegistrationDocument());
        MerlotOrganizationCredentialSubject resultCredentialSubject = participantDto.getSelfDescription()
            .getVerifiableCredential().getCredentialSubject();

        assertThat(resultCredentialSubject).usingRecursiveComparison().ignoringFields("id", "merlotId")
            .isEqualTo(getExpectedCredentialSubject());

        StringTypeValue merlotId = resultCredentialSubject.getMerlotId();
        assertThat(merlotId).isNotNull();
        assertThat(merlotId.getType()).isEqualTo("xsd:string");
        assertThat(merlotId.getValue()).isNotBlank();
        assertThat(resultCredentialSubject.getId()).isNotBlank().isEqualTo("Participant:" + merlotId.getValue());

        OrganizationMetadata metadataExpected = new OrganizationMetadata(merlotId.getValue(), MAIL_ADDRESS,
            MembershipClass.PARTICIPANT);

        ArgumentCaptor<OrganizationMetadata> varArgs = ArgumentCaptor.forClass(OrganizationMetadata.class);
        verify(organisationMetadataService, times(1)).saveMerlotParticipantMeta(varArgs.capture());
        assertEquals(metadataExpected.getMerlotId(), varArgs.getValue().getMerlotId());
        assertEquals(metadataExpected.getMailAddress(), varArgs.getValue().getMailAddress());
        assertEquals(metadataExpected.getMembershipClass(), varArgs.getValue().getMembershipClass());
    }

    @Test
    void createParticipantWithInvalidRegistrationForm() {

        PDDocument pdDocument = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        pdDocument.addPage(page);

        PDAcroForm form = new PDAcroForm(pdDocument);
        pdDocument.getDocumentCatalog().setAcroForm(form);

        PDFont font = new PDType1Font(HELVETICA);
        PDResources resources = new PDResources();
        resources.put(COSName.getPDFName("Helv"), font);
        form.setDefaultResources(resources);

        Exception e = assertThrows(ResponseStatusException.class,
            () -> gxfsCatalogRestService.createParticipant(pdDocument));

        assertEquals("400 BAD_REQUEST \"Invalid registration form file.\"", e.getMessage());
    }

    @Test
    void createParticipantWithEmptyFieldsInRegistrationForm() {

        PDDocument pdDocument = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        pdDocument.addPage(page);

        PDAcroForm form = new PDAcroForm(pdDocument);
        pdDocument.getDocumentCatalog().setAcroForm(form);

        PDFont font = new PDType1Font(HELVETICA);
        PDResources resources = new PDResources();
        resources.put(COSName.getPDFName("Helv"), font);
        form.setDefaultResources(resources);

        String defaultAppearance = "/Helv 12 Tf 0 0 1 rg";

        PDTextField textField = new PDTextField(form);
        textField.setPartialName("MailAddress");
        textField.setDefaultAppearance(defaultAppearance);
        form.getFields().add(textField);

        PDTextField textField1 = new PDTextField(form);
        textField1.setPartialName("OrganizationLegalName");
        form.getFields().add(textField1);
        textField1.setDefaultAppearance(defaultAppearance);

        PDTextField textField2 = new PDTextField(form);
        textField2.setPartialName("RegistrationNumber");
        form.getFields().add(textField2);
        textField2.setDefaultAppearance(defaultAppearance);

        PDTextField textField3 = new PDTextField(form);
        textField3.setPartialName("CountryCode");
        form.getFields().add(textField3);
        textField3.setDefaultAppearance(defaultAppearance);

        PDTextField textField4 = new PDTextField(form);
        textField4.setPartialName("OrganizationName");
        form.getFields().add(textField4);
        textField4.setDefaultAppearance(defaultAppearance);

        PDTextField textField5 = new PDTextField(form);
        textField5.setPartialName("PostalCode");
        form.getFields().add(textField5);
        textField5.setDefaultAppearance(defaultAppearance);

        PDTextField textField7 = new PDTextField(form);
        textField7.setPartialName("City");
        form.getFields().add(textField7);
        textField7.setDefaultAppearance(defaultAppearance);

        PDTextField textField8 = new PDTextField(form);
        textField8.setPartialName("ProviderTncLink");
        form.getFields().add(textField8);
        textField8.setDefaultAppearance(defaultAppearance);

        PDTextField textField9 = new PDTextField(form);
        textField9.setPartialName("Street");
        form.getFields().add(textField9);
        textField9.setDefaultAppearance(defaultAppearance);

        PDTextField textField10 = new PDTextField(form);
        textField10.setPartialName("ProviderTncHash");
        form.getFields().add(textField10);
        textField10.setDefaultAppearance(defaultAppearance);

        Exception e = assertThrows(ResponseStatusException.class,
            () -> gxfsCatalogRestService.createParticipant(pdDocument));

        assertEquals("400 BAD_REQUEST \"Invalid registration form: Empty or blank fields.\"", e.getMessage());
    }

    private MerlotOrganizationCredentialSubject getTestEditedMerlotOrganizationCredentialSubject() {

        MerlotOrganizationCredentialSubject credentialSubject = new MerlotOrganizationCredentialSubject();
        credentialSubject.setId("changedId");
        RegistrationNumber registrationNumber = new RegistrationNumber();
        registrationNumber.setLocal(new StringTypeValue("changedLocal"));
        registrationNumber.setEori(new StringTypeValue("changedEori"));
        registrationNumber.setEuid(new StringTypeValue("changedEuid"));
        registrationNumber.setVatId(new StringTypeValue("changedVatId"));
        credentialSubject.setRegistrationNumber(registrationNumber);
        VCard address = new VCard();
        address.setStreetAddress(new StringTypeValue("changedAddress"));
        address.setLocality(new StringTypeValue("changedCity"));
        address.setCountryName(new StringTypeValue("changedCountry"));
        address.setPostalCode(new StringTypeValue("changedPostCode"));
        credentialSubject.setLegalAddress(address);
        credentialSubject.setHeadquarterAddress(address);
        credentialSubject.setOrgaName(new StringTypeValue("changedOrgaName"));
        credentialSubject.setLegalName(new StringTypeValue("changedLegalName"));
        credentialSubject.setMerlotId(new StringTypeValue("changedMerlotId"));
        TermsAndConditions termsAndConditions = new TermsAndConditions();
        termsAndConditions.setContent(new StringTypeValue("http://changed.com"));
        termsAndConditions.setHash(new StringTypeValue("changedHash"));
        credentialSubject.setTermsAndConditions(termsAndConditions);
        return credentialSubject;
    }

    private MerlotParticipantDto getMerlotParticipantDtoWithEdits() {

        MerlotParticipantDto dtoWithEdits = new MerlotParticipantDto();

        SelfDescription<MerlotOrganizationCredentialSubject> selfDescription = new SelfDescription<>();
        SelfDescriptionVerifiableCredential<MerlotOrganizationCredentialSubject> verifiableCredential = new SelfDescriptionVerifiableCredential<>();
        MerlotOrganizationCredentialSubject editedCredentialSubject = getTestEditedMerlotOrganizationCredentialSubject();

        verifiableCredential.setCredentialSubject(editedCredentialSubject);
        selfDescription.setVerifiableCredential(verifiableCredential);

        MerlotParticipantMetaDto metaData = new MerlotParticipantMetaDto();
        metaData.setOrgaId("changedMerlotId");
        metaData.setMailAddress("changedMailAddress");
        metaData.setMembershipClass("Federator");

        dtoWithEdits.setSelfDescription(selfDescription);
        dtoWithEdits.setId(editedCredentialSubject.getId());
        dtoWithEdits.setMetadata(metaData);
        return dtoWithEdits;
    }
}

