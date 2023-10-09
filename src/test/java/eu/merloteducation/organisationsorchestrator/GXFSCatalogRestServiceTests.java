package eu.merloteducation.organisationsorchestrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.organisationsorchestrator.mappers.OrganizationMapper;
import eu.merloteducation.organisationsorchestrator.models.dto.MerlotParticipantDto;
import eu.merloteducation.organisationsorchestrator.models.gxfscatalog.*;
import eu.merloteducation.organisationsorchestrator.service.GXFSCatalogRestService;
import eu.merloteducation.organisationsorchestrator.service.KeycloakAuthService;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

import java.net.URI;
import java.util.List;

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


    private String wrapSelfDescription(String selfDescription) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        ParticipantSelfDescription sd = objectMapper.readValue(selfDescription, ParticipantSelfDescription.class);
        ParticipantItem item = new ParticipantItem();
        item.setSelfDescription(sd);
        item.setId(sd.getVerifiableCredential().getCredentialSubject().getId());
        item.setName("name");
        item.setPublicKey(new PublicKey());
        return objectMapper.writeValueAsString(item);
    }
    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(gxfsCatalogRestService, "organizationMapper", organizationMapper);
        ReflectionTestUtils.setField(gxfsCatalogRestService, "keycloakAuthService", keycloakAuthService);

        lenient().when(keycloakAuthService.webCallAuthenticated(eq(HttpMethod.GET), any(), any(), any()))
                .thenThrow(HttpClientErrorException.NotFound.class);

        String mockParticipant = """
                {
                    "id": "Participant:10",
                    "name": "Participant:10",
                    "publicKey": "{\\"kty\\":\\"RSA\\",\\"e\\":\\"AQAB\\",\\"alg\\":\\"PS256\\",\\"n\\":\\"dummy\\"}",
                    "selfDescription":"{\\"@id\\": \\"http://example.edu/verifiablePresentation/self-description1\\", \\"proof\\": {\\"created\\": \\"2023-04-27T13:48:11Z\\", \\"jws\\": \\"dummy\\", \\"proofPurpose\\": \\"assertionMethod\\", \\"type\\": \\"JsonWebSignature2020\\", \\"verificationMethod\\": \\"did:web:compliance.lab.gaia-x.eu\\"}, \\"type\\": [\\"VerifiablePresentation\\"], \\"@context\\": [\\"https://www.w3.org/2018/credentials/v1\\"], \\"verifiableCredential\\": {\\"credentialSubject\\": {\\"gax-trust-framework:registrationNumber\\": {\\"gax-trust-framework:local\\": {\\"@value\\": \\"0762747721\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"gax-trust-framework:RegistrationNumber\\"}, \\"gax-trust-framework:legalName\\": {\\"@value\\": \\"Gaia-X European Association for Data and Cloud AISBL\\", \\"@type\\": \\"xsd:string\\"}, \\"gax-trust-framework:headquarterAddress\\": {\\"vcard:country-name\\": {\\"@value\\": \\"BE\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"vcard:Address\\", \\"vcard:street-address\\": {\\"@value\\": \\"Avenue des Arts 6-9\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:locality\\": {\\"@value\\": \\"Brüssel\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:postal-code\\": {\\"@value\\": \\"1210\\", \\"@type\\": \\"xsd:string\\"}}, \\"merlot:termsAndConditions\\": {\\"gax-trust-framework:content\\": {\\"@value\\": \\"http://example.com\\", \\"@type\\": \\"xsd:anyURI\\"}, \\"gax-trust-framework:hash\\": {\\"@value\\": \\"hash1234\\", \\"@type\\": \\"xsd:string\\"}}, \\"@type\\": \\"merlot:MerlotOrganization\\", \\"merlot:merlotId\\": {\\"@value\\": \\"10\\", \\"@type\\": \\"xsd:string\\"}, \\"gax-trust-framework:legalAddress\\": {\\"vcard:country-name\\": {\\"@value\\": \\"BE\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"vcard:Address\\", \\"vcard:street-address\\": {\\"@value\\": \\"Avenue des Arts 6-9\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:locality\\": {\\"@value\\": \\"Brüssel\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:postal-code\\": {\\"@value\\": \\"1210\\", \\"@type\\": \\"xsd:string\\"}}, \\"merlot:orgaName\\": {\\"@value\\": \\"Gaia-X AISBL\\", \\"@type\\": \\"xsd:string\\"}, \\"@id\\": \\"Participant:10\\", \\"@context\\": {\\"merlot\\": \\"http://w3id.org/gaia-x/merlot#\\", \\"gax-trust-framework\\": \\"http://w3id.org/gaia-x/gax-trust-framework#\\", \\"rdf\\": \\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\\", \\"sh\\": \\"http://www.w3.org/ns/shacl#\\", \\"xsd\\": \\"http://www.w3.org/2001/XMLSchema#\\", \\"gax-validation\\": \\"http://w3id.org/gaia-x/validation#\\", \\"skos\\": \\"http://www.w3.org/2004/02/skos/core#\\", \\"vcard\\": \\"http://www.w3.org/2006/vcard/ns#\\"}, \\"merlot:addressCode\\": {\\"@value\\": \\"BE-BRU\\", \\"@type\\": \\"xsd:string\\"}}, \\"issuanceDate\\": \\"2022-10-19T18:48:09Z\\", \\"@type\\": [\\"VerifiableCredential\\"], \\"@id\\": \\"https://www.example.org/legalPerson.json\\", \\"proof\\": {\\"created\\": \\"2023-04-27T13:48:10Z\\", \\"jws\\": \\"dummy\\", \\"proofPurpose\\": \\"assertionMethod\\", \\"type\\": \\"JsonWebSignature2020\\", \\"verificationMethod\\": \\"did:web:compliance.lab.gaia-x.eu\\"}, \\"@context\\": [\\"https://www.w3.org/2018/credentials/v1\\"], \\"issuer\\": \\"Participant:10\\"}}"
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
                                 "content": "{\\"@id\\": \\"http://example.edu/verifiablePresentation/self-description1\\", \\"proof\\": {\\"created\\": \\"2023-08-30T08:58:35Z\\", \\"jws\\": \\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..JENTxPd26Ke05vIjtCzMESUvla_iYqP00ppsJfKagE06-XegrCbgRFoty20Tf40tPCd9_VflRL3kW12VCoOlDPA2nc21jaa_vmv8ZCCFNBmXIJVrBmF370MdyRT53Z-TGPKoUv5iF0m5fibKqqtg8MMCNVG9J3eff-Q04Wc5jZTgq2a9mjRsuZUAcnmu6ZgO4aaCKPD1t2aI3pZpie5zk5RJ37ZezuYQa7zdRirq_8Qaa9acg-aVqLaGxFAJhcpOcck-zkaP52pxVCusLt2bVUSG6HVk9txwCoc8ZoGCXW27MN8SM3I5PwfD_3OXGvs4TR0j-9ylKSajwWYRclNDtJMSBhmtXu_wjrjDMZFG2kRow_p1xhZZ71DKlX2Efp6VAdSWYbPpguZv1qMYbBemC3DW2lhkOsk1_KkwICO3ZSySNEswsjDty3NuGUOZtyyvImbSZ5f3I7ZyMNvnL1xoYEteK6mBSB9H7Zr1E1yZr7K1eiXR2MQuxKaFYl6jikYuwpdyrD6lvOWCEKOBQ_yjaQ9lbySiOxbNykpOX6-Bbu6mVQIX08BEzg0Y8r0Bnce2KPWypMtyHW7KhVgok2aLIjFQGutG7pgeIXIK2mPIR5jxUWUUh3XDuuU21cDYbMb6wYNX_-sNHNsots-mA81kRPlSRWlXBkvsZffXo6bWhKQ\\", \\"proofPurpose\\": \\"assertionMethod\\", \\"type\\": \\"JsonWebSignature2020\\", \\"verificationMethod\\": \\"did:web:compliance.lab.gaia-x.eu\\"}, \\"type\\": [\\"VerifiablePresentation\\"], \\"@context\\": [\\"https://www.w3.org/2018/credentials/v1\\"], \\"verifiableCredential\\": {\\"credentialSubject\\": {\\"gax-trust-framework:registrationNumber\\": {\\"gax-trust-framework:local\\": {\\"@value\\": \\"0762747721\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"gax-trust-framework:RegistrationNumber\\"}, \\"gax-trust-framework:legalName\\": {\\"@value\\": \\"Gaia-X European Association for Data and Cloud AISBL\\", \\"@type\\": \\"xsd:string\\"}, \\"gax-trust-framework:headquarterAddress\\": {\\"vcard:country-name\\": {\\"@value\\": \\"BE\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"vcard:Address\\", \\"vcard:street-address\\": {\\"@value\\": \\"Avenue des Arts 6-9\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:locality\\": {\\"@value\\": \\"Br\\\\u00fcssel\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:postal-code\\": {\\"@value\\": \\"1210\\", \\"@type\\": \\"xsd:string\\"}}, \\"@type\\": \\"merlot:MerlotOrganization\\", \\"merlot:merlotId\\": {\\"@value\\": \\"10\\", \\"@type\\": \\"xsd:string\\"}, \\"gax-trust-framework:legalAddress\\": {\\"vcard:country-name\\": {\\"@value\\": \\"BE\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"vcard:Address\\", \\"vcard:street-address\\": {\\"@value\\": \\"Avenue des Arts 6-9\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:locality\\": {\\"@value\\": \\"Br\\\\u00fcssel\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:postal-code\\": {\\"@value\\": \\"1210\\", \\"@type\\": \\"xsd:string\\"}}, \\"merlot:orgaName\\": {\\"@value\\": \\"Gaia-X AISBL\\", \\"@type\\": \\"xsd:string\\"}, \\"@id\\": \\"Participant:10\\", \\"@context\\": {\\"merlot\\": \\"http://w3id.org/gaia-x/merlot#\\", \\"gax-trust-framework\\": \\"http://w3id.org/gaia-x/gax-trust-framework#\\", \\"rdf\\": \\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\\", \\"sh\\": \\"http://www.w3.org/ns/shacl#\\", \\"xsd\\": \\"http://www.w3.org/2001/XMLSchema#\\", \\"gax-validation\\": \\"http://w3id.org/gaia-x/validation#\\", \\"skos\\": \\"http://www.w3.org/2004/02/skos/core#\\", \\"vcard\\": \\"http://www.w3.org/2006/vcard/ns#\\"}, \\"merlot:termsAndConditions\\": {\\"gax-trust-framework:content\\": {\\"@value\\": \\"http://example.com\\", \\"@type\\": \\"xsd:anyURI\\"}, \\"@type\\": \\"gax-trust-framework:TermsAndConditions\\", \\"gax-trust-framework:hash\\": {\\"@value\\": \\"hash1234\\", \\"@type\\": \\"xsd:string\\"}}, \\"merlot:addressCode\\": {\\"@value\\": \\"BE-BRU\\", \\"@type\\": \\"xsd:string\\"}, \\"merlot:mailAddress\\": {\\"@value\\": \\"mymail@example.com\\", \\"@type\\": \\"xsd:string\\"}}, \\"issuanceDate\\": \\"2022-10-19T18:48:09Z\\", \\"@type\\": [\\"VerifiableCredential\\"], \\"@id\\": \\"https://www.example.org/legalPerson.json\\", \\"proof\\": {\\"created\\": \\"2023-08-30T08:58:34Z\\", \\"jws\\": \\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..yV2y2TdkrLpsrP1ZTmtEjazcRMtKKwEHviNx_cC3BxBM8R2DeAbBuxbgcO_3ZoUeuB-6laSe2RN6jIp5UMCVRzgFg1YIbNKMcyDfC2LhF0YaXxo9pB-L3qLdRJpve3-NBHj76dBUW4Q04S_4t_5M09p61fEuCRJIIrDzh2iKSLwzKrv2sT8hnMefN2P29sa5QlJrRu-kFHolq_ZmwsXzWMN3R8_8tbsS19eP1hIzkuzW1lla8jZot06Y6bFslr3S5CCbexABJb34puu2nbH2n4Qdc9BS31B34HnduC8AuKEbOfmWsGDSZT29QjL-VxUWN4lhqxb-DsiSpmDlEPt_UzJah5tvMSQzAlKpm2ZZdBuQb8Mk9-U9oRmrxm6xeOcXdcBMAHXEYlBMp6R8gEOyQ3uDMrR2x9xMTs4EeJgJlOSsyK7F5_EbMtqnLulKRD4RtNoZ_I8k0XcVZAVoBtxrEwWOE48AdW16yfemqEO8s8J_J9TrBaTTMKIMFqJjJ-HNc9n7E_saylVFRadHevbLLuBNDOjjwvI8E5r55iO2HTPxB1dSWcjidSacSCvo2zQxRLkbPQJmLp2S4SCMLbqwPdph8KH6tfAcgxH0k3sTmwvt2tTLBXCINPlnhv2ahuHzXWGpgegEyHLrtlUAwfeDilkc_lib_chWBVqqWxu-7Gw\\", \\"proofPurpose\\": \\"assertionMethod\\", \\"type\\": \\"JsonWebSignature2020\\", \\"verificationMethod\\": \\"did:web:compliance.lab.gaia-x.eu\\"}, \\"@context\\": [\\"https://www.w3.org/2018/credentials/v1\\"], \\"issuer\\": \\"Participant:10\\"}}",
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
                             "content": "{\\"@id\\": \\"http://example.edu/verifiablePresentation/self-description1\\", \\"proof\\": {\\"created\\": \\"2023-08-30T08:58:35Z\\", \\"jws\\": \\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..JENTxPd26Ke05vIjtCzMESUvla_iYqP00ppsJfKagE06-XegrCbgRFoty20Tf40tPCd9_VflRL3kW12VCoOlDPA2nc21jaa_vmv8ZCCFNBmXIJVrBmF370MdyRT53Z-TGPKoUv5iF0m5fibKqqtg8MMCNVG9J3eff-Q04Wc5jZTgq2a9mjRsuZUAcnmu6ZgO4aaCKPD1t2aI3pZpie5zk5RJ37ZezuYQa7zdRirq_8Qaa9acg-aVqLaGxFAJhcpOcck-zkaP52pxVCusLt2bVUSG6HVk9txwCoc8ZoGCXW27MN8SM3I5PwfD_3OXGvs4TR0j-9ylKSajwWYRclNDtJMSBhmtXu_wjrjDMZFG2kRow_p1xhZZ71DKlX2Efp6VAdSWYbPpguZv1qMYbBemC3DW2lhkOsk1_KkwICO3ZSySNEswsjDty3NuGUOZtyyvImbSZ5f3I7ZyMNvnL1xoYEteK6mBSB9H7Zr1E1yZr7K1eiXR2MQuxKaFYl6jikYuwpdyrD6lvOWCEKOBQ_yjaQ9lbySiOxbNykpOX6-Bbu6mVQIX08BEzg0Y8r0Bnce2KPWypMtyHW7KhVgok2aLIjFQGutG7pgeIXIK2mPIR5jxUWUUh3XDuuU21cDYbMb6wYNX_-sNHNsots-mA81kRPlSRWlXBkvsZffXo6bWhKQ\\", \\"proofPurpose\\": \\"assertionMethod\\", \\"type\\": \\"JsonWebSignature2020\\", \\"verificationMethod\\": \\"did:web:compliance.lab.gaia-x.eu\\"}, \\"type\\": [\\"VerifiablePresentation\\"], \\"@context\\": [\\"https://www.w3.org/2018/credentials/v1\\"], \\"verifiableCredential\\": {\\"credentialSubject\\": {\\"gax-trust-framework:registrationNumber\\": {\\"gax-trust-framework:local\\": {\\"@value\\": \\"0762747721\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"gax-trust-framework:RegistrationNumber\\"}, \\"gax-trust-framework:legalName\\": {\\"@value\\": \\"Gaia-X European Association for Data and Cloud AISBL\\", \\"@type\\": \\"xsd:string\\"}, \\"gax-trust-framework:headquarterAddress\\": {\\"vcard:country-name\\": {\\"@value\\": \\"BE\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"vcard:Address\\", \\"vcard:street-address\\": {\\"@value\\": \\"Avenue des Arts 6-9\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:locality\\": {\\"@value\\": \\"Br\\\\u00fcssel\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:postal-code\\": {\\"@value\\": \\"1210\\", \\"@type\\": \\"xsd:string\\"}}, \\"@type\\": \\"merlot:MerlotOrganization\\", \\"merlot:merlotId\\": {\\"@value\\": \\"10\\", \\"@type\\": \\"xsd:string\\"}, \\"gax-trust-framework:legalAddress\\": {\\"vcard:country-name\\": {\\"@value\\": \\"BE\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"vcard:Address\\", \\"vcard:street-address\\": {\\"@value\\": \\"Avenue des Arts 6-9\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:locality\\": {\\"@value\\": \\"Br\\\\u00fcssel\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:postal-code\\": {\\"@value\\": \\"1210\\", \\"@type\\": \\"xsd:string\\"}}, \\"merlot:orgaName\\": {\\"@value\\": \\"Gaia-X AISBL\\", \\"@type\\": \\"xsd:string\\"}, \\"@id\\": \\"Participant:10\\", \\"@context\\": {\\"merlot\\": \\"http://w3id.org/gaia-x/merlot#\\", \\"gax-trust-framework\\": \\"http://w3id.org/gaia-x/gax-trust-framework#\\", \\"rdf\\": \\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\\", \\"sh\\": \\"http://www.w3.org/ns/shacl#\\", \\"xsd\\": \\"http://www.w3.org/2001/XMLSchema#\\", \\"gax-validation\\": \\"http://w3id.org/gaia-x/validation#\\", \\"skos\\": \\"http://www.w3.org/2004/02/skos/core#\\", \\"vcard\\": \\"http://www.w3.org/2006/vcard/ns#\\"}, \\"merlot:termsAndConditions\\": {\\"gax-trust-framework:content\\": {\\"@value\\": \\"http://example.com\\", \\"@type\\": \\"xsd:anyURI\\"}, \\"@type\\": \\"gax-trust-framework:TermsAndConditions\\", \\"gax-trust-framework:hash\\": {\\"@value\\": \\"hash1234\\", \\"@type\\": \\"xsd:string\\"}}, \\"merlot:addressCode\\": {\\"@value\\": \\"BE-BRU\\", \\"@type\\": \\"xsd:string\\"}, \\"merlot:mailAddress\\": {\\"@value\\": \\"mymail@example.com\\", \\"@type\\": \\"xsd:string\\"}}, \\"issuanceDate\\": \\"2022-10-19T18:48:09Z\\", \\"@type\\": [\\"VerifiableCredential\\"], \\"@id\\": \\"https://www.example.org/legalPerson.json\\", \\"proof\\": {\\"created\\": \\"2023-08-30T08:58:34Z\\", \\"jws\\": \\"eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJQUzI1NiJ9..yV2y2TdkrLpsrP1ZTmtEjazcRMtKKwEHviNx_cC3BxBM8R2DeAbBuxbgcO_3ZoUeuB-6laSe2RN6jIp5UMCVRzgFg1YIbNKMcyDfC2LhF0YaXxo9pB-L3qLdRJpve3-NBHj76dBUW4Q04S_4t_5M09p61fEuCRJIIrDzh2iKSLwzKrv2sT8hnMefN2P29sa5QlJrRu-kFHolq_ZmwsXzWMN3R8_8tbsS19eP1hIzkuzW1lla8jZot06Y6bFslr3S5CCbexABJb34puu2nbH2n4Qdc9BS31B34HnduC8AuKEbOfmWsGDSZT29QjL-VxUWN4lhqxb-DsiSpmDlEPt_UzJah5tvMSQzAlKpm2ZZdBuQb8Mk9-U9oRmrxm6xeOcXdcBMAHXEYlBMp6R8gEOyQ3uDMrR2x9xMTs4EeJgJlOSsyK7F5_EbMtqnLulKRD4RtNoZ_I8k0XcVZAVoBtxrEwWOE48AdW16yfemqEO8s8J_J9TrBaTTMKIMFqJjJ-HNc9n7E_saylVFRadHevbLLuBNDOjjwvI8E5r55iO2HTPxB1dSWcjidSacSCvo2zQxRLkbPQJmLp2S4SCMLbqwPdph8KH6tfAcgxH0k3sTmwvt2tTLBXCINPlnhv2ahuHzXWGpgegEyHLrtlUAwfeDilkc_lib_chWBVqqWxu-7Gw\\", \\"proofPurpose\\": \\"assertionMethod\\", \\"type\\": \\"JsonWebSignature2020\\", \\"verificationMethod\\": \\"did:web:compliance.lab.gaia-x.eu\\"}, \\"@context\\": [\\"https://www.w3.org/2018/credentials/v1\\"], \\"issuer\\": \\"Participant:10\\"}}"
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
        lenient().when(keycloakAuthService.webCallAuthenticated(eq(HttpMethod.POST),
                        startsWith(gxfscatalogQueryUri), any(), any()))
                .thenReturn(mockQueryResponse);
        lenient().when(keycloakAuthService.webCallAuthenticated(eq(HttpMethod.GET),
                        startsWith(gxfscatalogSelfdescriptionsUri), any(), any()))
                .thenReturn(mockUserResponse);
        lenient().when(keycloakAuthService.webCallAuthenticated(eq(HttpMethod.GET),
                        eq(URI.create(gxfscatalogParticipantsUri + "/Participant:10").toString()), any(), any()))
                .thenReturn(mockParticipant);
        lenient().when(keycloakAuthService.webCallAuthenticated(eq(HttpMethod.PUT),
                        eq(URI.create(gxfscatalogParticipantsUri + "/Participant:10").toString()), anyString(), any()))
                .thenAnswer(i -> wrapSelfDescription((String) i.getArguments()[2]));

    }

    @Test
    void getAllParticipants() throws Exception {

        Page<MerlotParticipantDto> organizations = gxfsCatalogRestService.getParticipants(PageRequest.of(0, 9));
        assertThat(organizations.getContent(), isA(List.class));
        assertThat(organizations.getContent(), not(empty()));

    }

    @Test
    void getParticipantById() throws Exception {
        MerlotParticipantDto organization = gxfsCatalogRestService.getParticipantById("10");
        assertThat(organization, isA(MerlotParticipantDto.class));
        assertEquals("10", organization.getSelfDescription().getVerifiableCredential().getCredentialSubject()
                .getMerlotId().getValue());
        assertEquals("Gaia-X European Association for Data and Cloud AISBL", organization.getSelfDescription()
                .getVerifiableCredential().getCredentialSubject().getLegalName().getValue());
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
        MerlotOrganizationCredentialSubject credentialSubject = new MerlotOrganizationCredentialSubject();
        credentialSubject.setId("garbage");
        RegistrationNumber registrationNumber = new RegistrationNumber();
        registrationNumber.setLocal(new StringTypeValue("garbage"));
        credentialSubject.setRegistrationNumber(registrationNumber);
        VCard address = new VCard();
        address.setStreetAddress(new StringTypeValue("address"));
        address.setLocality(new StringTypeValue("Berlin"));
        address.setCountryName(new StringTypeValue("DE"));
        address.setPostalCode(new StringTypeValue("12345"));
        credentialSubject.setLegalAddress(address);
        credentialSubject.setHeadquarterAddress(address);
        credentialSubject.setAddressCode(new StringTypeValue("DE-BER"));
        credentialSubject.setOrgaName(new StringTypeValue("garbage"));
        credentialSubject.setLegalName(new StringTypeValue("garbage"));
        credentialSubject.setMerlotId(new StringTypeValue("garbage"));
        credentialSubject.setMailAddress(new StringTypeValue("me@mail.me"));
        TermsAndConditions termsAndConditions = new TermsAndConditions();
        termsAndConditions.setContent(new StringTypeValue("http://example.com"));
        termsAndConditions.setHash(new StringTypeValue("1234"));
        credentialSubject.setTermsAndConditions(termsAndConditions);

        MerlotParticipantDto participantDto = gxfsCatalogRestService.updateParticipant(credentialSubject, "10");
        MerlotOrganizationCredentialSubject resultCredentialSubject = participantDto.getSelfDescription()
                .getVerifiableCredential().getCredentialSubject();
        assertEquals(resultCredentialSubject.getMailAddress().getValue(), credentialSubject.getMailAddress().getValue());
        assertEquals(resultCredentialSubject.getAddressCode().getValue(), credentialSubject.getAddressCode().getValue());
        assertEquals(resultCredentialSubject.getTermsAndConditions().getContent().getValue(),
                credentialSubject.getTermsAndConditions().getContent().getValue());
        assertEquals(resultCredentialSubject.getTermsAndConditions().getHash().getValue(),
                credentialSubject.getTermsAndConditions().getHash().getValue());
        assertEquals(resultCredentialSubject.getLegalAddress().getStreetAddress().getValue(),
                credentialSubject.getLegalAddress().getStreetAddress().getValue());
        assertEquals(resultCredentialSubject.getLegalAddress().getLocality().getValue(),
                credentialSubject.getLegalAddress().getLocality().getValue());
        assertEquals(resultCredentialSubject.getLegalAddress().getCountryName().getValue(),
                credentialSubject.getLegalAddress().getCountryName().getValue());
        assertEquals(resultCredentialSubject.getLegalAddress().getPostalCode().getValue(),
                credentialSubject.getLegalAddress().getPostalCode().getValue());

        assertNotEquals(resultCredentialSubject.getId(), credentialSubject.getId());
        assertNotEquals(resultCredentialSubject.getMerlotId().getValue(), credentialSubject.getMerlotId().getValue());
        assertNotEquals(resultCredentialSubject.getOrgaName().getValue(), credentialSubject.getOrgaName().getValue());
        assertNotEquals(resultCredentialSubject.getLegalName().getValue(), credentialSubject.getLegalName().getValue());
        assertNotEquals(resultCredentialSubject.getRegistrationNumber().getLocal().getValue(),
                credentialSubject.getRegistrationNumber().getLocal().getValue());
    }

    @Test
    void updateParticipantNonExistent() {
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
        assertThrows(HttpClientErrorException.NotFound.class, () -> gxfsCatalogRestService.updateParticipant(credentialSubject, "11"));
    }

    @Test
    void getAllFederators() throws Exception {
        Page<MerlotParticipantDto> organizations = gxfsCatalogRestService.getFederators(PageRequest.of(0, Integer.MAX_VALUE));
        assertThat(organizations.getContent(), isA(List.class));
        assertThat(organizations.getContent(), not(empty()));
    }
}

