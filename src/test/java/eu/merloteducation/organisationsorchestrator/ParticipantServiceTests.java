package eu.merloteducation.organisationsorchestrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.authorizationlibrary.authorization.OrganizationRole;
import eu.merloteducation.authorizationlibrary.authorization.OrganizationRoleGrantedAuthority;
import eu.merloteducation.gxfscataloglibrary.models.exception.CredentialPresentationException;
import eu.merloteducation.gxfscataloglibrary.models.exception.CredentialSignatureException;
import eu.merloteducation.gxfscataloglibrary.models.participants.ParticipantItem;
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
import eu.merloteducation.modelslib.api.organization.MembershipClass;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantMetaDto;
import eu.merloteducation.organisationsorchestrator.mappers.OrganizationMapper;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganizationMetadata;
import eu.merloteducation.organisationsorchestrator.service.OrganizationMetadataService;
import eu.merloteducation.organisationsorchestrator.service.ParticipantService;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;

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
class ParticipantServiceTests {

    @Autowired
    private OrganizationMapper organizationMapper;

    @Value("${gxfscatalog.participants-uri}")
    private String gxfscatalogParticipantsUri;

    @Value("${gxfscatalog.selfdescriptions-uri}")
    private String gxfscatalogSelfdescriptionsUri;

    @Value("${gxfscatalog.query-uri}")
    private String gxfscatalogQueryUri;

    @Autowired
    private ParticipantService participantService;

    @MockBean
    private GxfsCatalogService gxfsCatalogService;

    @MockBean
    OrganizationMetadataService organizationMetadataService;

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
        textField.setValue(mailAddress);

        PDTextField textField1 = new PDTextField(form);
        textField1.setPartialName("OrganizationLegalName");
        form.getFields().add(textField1);
        textField1.setDefaultAppearance(defaultAppearance);
        textField1.setValue(organizationLegalName);

        PDTextField textField2 = new PDTextField(form);
        textField2.setPartialName("RegistrationNumber");
        form.getFields().add(textField2);
        textField2.setDefaultAppearance(defaultAppearance);
        textField2.setValue(registrationNumber);

        PDTextField textField3 = new PDTextField(form);
        textField3.setPartialName("CountryCode");
        form.getFields().add(textField3);
        textField3.setDefaultAppearance(defaultAppearance);
        textField3.setValue(countryCode);

        PDTextField textField4 = new PDTextField(form);
        textField4.setPartialName("OrganizationName");
        form.getFields().add(textField4);
        textField4.setDefaultAppearance(defaultAppearance);
        textField4.setValue(organizationName);

        PDTextField textField5 = new PDTextField(form);
        textField5.setPartialName("PostalCode");
        form.getFields().add(textField5);
        textField5.setDefaultAppearance(defaultAppearance);
        textField5.setValue(postalCode);

        PDTextField textField7 = new PDTextField(form);
        textField7.setPartialName("City");
        form.getFields().add(textField7);
        textField7.setDefaultAppearance(defaultAppearance);
        textField7.setValue(city);

        PDTextField textField8 = new PDTextField(form);
        textField8.setPartialName("ProviderTncLink");
        form.getFields().add(textField8);
        textField8.setDefaultAppearance(defaultAppearance);
        textField8.setValue(providerTncLink);

        PDTextField textField9 = new PDTextField(form);
        textField9.setPartialName("Street");
        form.getFields().add(textField9);
        textField9.setDefaultAppearance(defaultAppearance);
        textField9.setValue(street);

        PDTextField textField10 = new PDTextField(form);
        textField10.setPartialName("ProviderTncHash");
        form.getFields().add(textField10);
        textField10.setDefaultAppearance(defaultAppearance);
        textField10.setValue(providerTncHash);
        return pdDocument;
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

    @BeforeEach
    public void setUp() throws JsonProcessingException, CredentialSignatureException, CredentialPresentationException {
        ObjectMapper mapper = new ObjectMapper();
        ReflectionTestUtils.setField(participantService, "organizationMapper", organizationMapper);
        ReflectionTestUtils.setField(participantService, "gxfsCatalogService", gxfsCatalogService);
        ReflectionTestUtils.setField(participantService, "organizationMetadataService", organizationMetadataService);

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
        ParticipantItem participantItem = mapper.readValue(mockParticipant, new TypeReference<>() {});

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
        GXFSCatalogListResponse<SelfDescriptionItem> sdItems = mapper.readValue(mockUserResponse, new TypeReference<>() {});

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
        GXFSCatalogListResponse<GXFSQueryUriItem> uriItems = mapper.readValue(mockQueryResponse, new TypeReference<>() {});

        lenient().when(gxfsCatalogService.getSortedParticipantUriPage(any(), any(), anyLong(), anyLong()))
                .thenReturn(uriItems);
        lenient().when(gxfsCatalogService.getSelfDescriptionsByIds(any()))
                    .thenReturn(sdItems);
        lenient().when(gxfsCatalogService.getParticipantById(eq("Participant:10")))
            .thenReturn(participantItem);
        lenient().when(gxfsCatalogService.updateParticipant(any()))
            .thenAnswer(i -> wrapCredentialSubjectInItem((MerlotOrganizationCredentialSubject) i.getArguments()[0]));
        lenient().when(gxfsCatalogService.addParticipant(any()))
                .thenAnswer(i -> wrapCredentialSubjectInItem((MerlotOrganizationCredentialSubject) i.getArguments()[0]));

        MerlotParticipantMetaDto metaDto = new MerlotParticipantMetaDto();
        metaDto.setOrgaId("10");
        metaDto.setMailAddress("mymail@example.com");
        metaDto.setMembershipClass(MembershipClass.PARTICIPANT);
        metaDto.setActive(true);

        lenient().when(organizationMetadataService.getMerlotParticipantMetaDto(eq("10"))).thenReturn(metaDto);
        lenient().when(organizationMetadataService.getParticipantsByMembershipClass(eq(MembershipClass.FEDERATOR))).thenReturn(new ArrayList<>());
        lenient().when(organizationMetadataService.updateMerlotParticipantMeta(any())).thenAnswer(i -> i.getArguments()[0]);
        lenient().when(organizationMetadataService.getInactiveParticipants()).thenReturn(new ArrayList<>());
    }

    @Test
    void getAllParticipants() throws Exception {
        OrganizationRoleGrantedAuthority activeRole = new OrganizationRoleGrantedAuthority(OrganizationRole.FED_ADMIN.getRoleName() + "_anything");

        Page<MerlotParticipantDto> organizations = participantService.getParticipants(PageRequest.of(0, 9), activeRole);
        assertThat(organizations.getContent(), isA(List.class));
        assertThat(organizations.getContent(), not(empty()));
        assertEquals(1, organizations.getContent().size());

        String merlotId = ((MerlotOrganizationCredentialSubject) organizations.getContent().get(0).getSelfDescription().getVerifiableCredential()
            .getCredentialSubject()).getMerlotId();
        assertEquals("10", merlotId);

        String mailAddress = organizations.getContent().get(0).getMetadata().getMailAddress();
        assertEquals("mymail@example.com", mailAddress);

        MembershipClass membershipClass = organizations.getContent().get(0).getMetadata().getMembershipClass();
        assertEquals(MembershipClass.PARTICIPANT, membershipClass);
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

        MerlotParticipantMetaDto metaDto = new MerlotParticipantMetaDto();
        metaDto.setOrgaId("10");
        metaDto.setMailAddress("mymail@example.com");
        metaDto.setMembershipClass(MembershipClass.PARTICIPANT);
        metaDto.setActive(false);

        lenient().when(organizationMetadataService.getInactiveParticipants()).thenReturn(List.of("10"));

        OrganizationRoleGrantedAuthority activeRole = new OrganizationRoleGrantedAuthority(OrganizationRole.ORG_LEG_REP.getRoleName() + "_anything");
        participantService.getParticipants(PageRequest.of(0, 9), activeRole);

        verify(organizationMetadataService, times(1)).getInactiveParticipants();
        verify(gxfsCatalogService, times(1)).getSortedParticipantUriPageWithExcludedUris(any(), any(), eq(List.of("10")), anyLong(), anyLong());
        verify(gxfsCatalogService, times(1)).getSelfDescriptionsByIds(argThat(strings -> strings != null && strings.length == 0));
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

        MerlotParticipantDto organization = participantService.getParticipantById("10");
        assertThat(organization, isA(MerlotParticipantDto.class));
        MerlotOrganizationCredentialSubject subject = (MerlotOrganizationCredentialSubject)
                organization.getSelfDescription().getVerifiableCredential().getCredentialSubject();
        assertEquals("10", subject.getMerlotId());
        assertEquals("Gaia-X European Association for Data and Cloud AISBL", subject.getLegalName());

        String mailAddress = organization.getMetadata().getMailAddress();
        assertEquals("mymail@example.com", mailAddress);

        MembershipClass membershipClass = organization.getMetadata().getMembershipClass();
        assertEquals(MembershipClass.PARTICIPANT, membershipClass);
    }

    @Test
    void getParticipantByIdFail() throws Exception {
        doThrow(getWebClientResponseException()).when(gxfsCatalogService).getParticipantById(any());

        assertThrows(ResponseStatusException.class, () -> participantService.getParticipantById("10"));
    }

    @Test
    void getParticipantByInvalidId() {

        assertThrows(IllegalArgumentException.class, () -> participantService.getParticipantById("asdf"));
    }

    @Test
    void getParticipantByNonexistentId() {
        ResponseStatusException e =
                assertThrows(ResponseStatusException.class, () -> participantService.getParticipantById("11"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
    }

    @Test
    void updateParticipantExistentAsParticipant() throws Exception {

        MerlotParticipantDto participantDtoWithEdits = getMerlotParticipantDtoWithEdits();
        MerlotOrganizationCredentialSubject editedCredentialSubject = (MerlotOrganizationCredentialSubject) participantDtoWithEdits.getSelfDescription()
            .getVerifiableCredential().getCredentialSubject();
        MerlotParticipantMetaDto editedMetadata = participantDtoWithEdits.getMetadata();

        OrganizationRoleGrantedAuthority activeRole = new OrganizationRoleGrantedAuthority("OrgLegRep_10");

        MerlotParticipantDto updatedParticipantDto = participantService.updateParticipant(participantDtoWithEdits, activeRole, "10");

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
        assertNotEquals(updatedCredentialSubject.getMerlotId(),
            editedCredentialSubject.getMerlotId());
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

        // following metadata of the organization should not have been updated
        assertNotEquals(updatedMetadata.getMembershipClass(), editedMetadata.getMembershipClass());
        assertNotEquals(updatedMetadata.getOrgaId(), editedMetadata.getOrgaId());
    }

    @Test
    void updateParticipantExistentAsFedAdmin() throws Exception {

        MerlotParticipantDto dtoWithEdits = getMerlotParticipantDtoWithEdits();
        MerlotOrganizationCredentialSubject editedCredentialSubject =
            (MerlotOrganizationCredentialSubject) dtoWithEdits.getSelfDescription().getVerifiableCredential()
                .getCredentialSubject();
        MerlotParticipantMetaDto editedMetadata = dtoWithEdits.getMetadata();

        OrganizationRoleGrantedAuthority activeRole = new OrganizationRoleGrantedAuthority("FedAdmin_10");

        MerlotParticipantDto participantDto = participantService.updateParticipant(dtoWithEdits, activeRole, "10");

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
        assertNotEquals(updatedCredentialSubject.getMerlotId(),
            editedCredentialSubject.getMerlotId());

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

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
            () -> participantService.updateParticipant(dtoWithEdits, activeRole, "11"));
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
    void getAllFederators() throws Exception {

        MerlotParticipantMetaDto metaDto = new MerlotParticipantMetaDto();
        metaDto.setOrgaId("10");
        metaDto.setMailAddress("mymail@example.com");
        metaDto.setMembershipClass(MembershipClass.FEDERATOR);
        List<MerlotParticipantMetaDto> list = new ArrayList<>();
        list.add(metaDto);

        lenient().when(organizationMetadataService.getParticipantsByMembershipClass(eq(MembershipClass.FEDERATOR))).thenReturn(list);

        List<MerlotParticipantDto> organizations = participantService.getFederators();
        assertThat(organizations, not(empty()));
        assertEquals(1, organizations.size());
        assertEquals("10",
            ((MerlotOrganizationCredentialSubject) organizations.get(0).getSelfDescription().getVerifiableCredential()
                .getCredentialSubject()).getMerlotId());
    }

    @Test
    void createParticipantWithValidRegistrationForm() throws Exception {

        MerlotParticipantDto participantDto = participantService.createParticipant(getTestRegistrationDocument());
        MerlotOrganizationCredentialSubject resultCredentialSubject = (MerlotOrganizationCredentialSubject)
                participantDto.getSelfDescription().getVerifiableCredential().getCredentialSubject();

        assertThat(resultCredentialSubject).usingRecursiveComparison().ignoringFields("id", "merlotId")
            .isEqualTo(getExpectedCredentialSubject());

        String merlotId = resultCredentialSubject.getMerlotId();
        assertThat(merlotId).isNotNull();
        assertThat(merlotId).isNotBlank();
        assertThat(resultCredentialSubject.getId()).isNotBlank().isEqualTo("Participant:" + merlotId);

        OrganizationMetadata metadataExpected = new OrganizationMetadata(merlotId, mailAddress,
            MembershipClass.PARTICIPANT, true);

        ArgumentCaptor<MerlotParticipantMetaDto> varArgs = ArgumentCaptor.forClass(MerlotParticipantMetaDto.class);
        verify(organizationMetadataService, times(1)).saveMerlotParticipantMeta(varArgs.capture());
        assertEquals(metadataExpected.getOrgaId(), varArgs.getValue().getOrgaId());
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
            () -> participantService.createParticipant(pdDocument));

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
            () -> participantService.createParticipant(pdDocument));

        assertEquals("400 BAD_REQUEST \"Invalid registration form: Empty or blank fields.\"", e.getMessage());
    }

    private MerlotOrganizationCredentialSubject getTestEditedMerlotOrganizationCredentialSubject() {

        MerlotOrganizationCredentialSubject credentialSubject = new MerlotOrganizationCredentialSubject();
        credentialSubject.setId("changedId");
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
        credentialSubject.setMerlotId("changedMerlotId");
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

        MerlotParticipantMetaDto metaData = new MerlotParticipantMetaDto();
        metaData.setOrgaId("changedMerlotId");
        metaData.setMailAddress("changedMailAddress");
        metaData.setMembershipClass(MembershipClass.FEDERATOR);

        dtoWithEdits.setSelfDescription(selfDescription);
        dtoWithEdits.setId(editedCredentialSubject.getId());
        dtoWithEdits.setMetadata(metaData);
        return dtoWithEdits;
    }
}

