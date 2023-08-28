package eu.merloteducation.organisationsorchestrator;

import eu.merloteducation.organisationsorchestrator.mappers.OrganizationMapper;
import eu.merloteducation.organisationsorchestrator.models.dto.MerlotParticipantDto;
import eu.merloteducation.organisationsorchestrator.service.GXFSCatalogRestService;
import eu.merloteducation.organisationsorchestrator.service.KeycloakAuthService;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@EnableConfigurationProperties
public class GXFSCatalogRestServiceTests {


    @Autowired
    private OrganizationMapper organizationMapper;

    @Value("${gxfscatalog.participants-uri}")
    private String gxfscatalogParticipantsUri;
    @Autowired
    private GXFSCatalogRestService gxfsCatalogRestService;

    @MockBean
    private KeycloakAuthService keycloakAuthService;

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
                    "selfDescription":"{\\"@id\\": \\"http://example.edu/verifiablePresentation/self-description1\\", \\"proof\\": {\\"created\\": \\"2023-04-27T13:48:11Z\\", \\"jws\\": \\"dummy\\", \\"proofPurpose\\": \\"assertionMethod\\", \\"type\\": \\"JsonWebSignature2020\\", \\"verificationMethod\\": \\"did:web:compliance.lab.gaia-x.eu\\"}, \\"type\\": [\\"VerifiablePresentation\\"], \\"@context\\": [\\"https://www.w3.org/2018/credentials/v1\\"], \\"verifiableCredential\\": {\\"credentialSubject\\": {\\"gax-trust-framework:registrationNumber\\": {\\"gax-trust-framework:local\\": {\\"@value\\": \\"0762747721\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"gax-trust-framework:RegistrationNumber\\"}, \\"gax-trust-framework:legalName\\": {\\"@value\\": \\"Gaia-X European Association for Data and Cloud AISBL\\", \\"@type\\": \\"xsd:string\\"}, \\"gax-trust-framework:headquarterAddress\\": {\\"vcard:country-name\\": {\\"@value\\": \\"BE\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"vcard:Address\\", \\"vcard:street-address\\": {\\"@value\\": \\"Avenue des Arts 6-9\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:locality\\": {\\"@value\\": \\"Br\\üssel\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:postal-code\\": {\\"@value\\": \\"1210\\", \\"@type\\": \\"xsd:string\\"}}, \\"gax-trust-framework:termsAndConditions\\": {\\"gax-trust-framework:content\\": {\\"@value\\": \\"http://example.com\\", \\"@type\\": \\"xsd:anyURI\\"}, \\"gax-trust-framework:hash\\": {\\"@value\\": \\"hash1234\\", \\"@type\\": \\"xsd:string\\"}}, \\"@type\\": \\"merlot:MerlotOrganization\\", \\"merlot:merlotId\\": {\\"@value\\": \\"10\\", \\"@type\\": \\"xsd:string\\"}, \\"gax-trust-framework:legalAddress\\": {\\"vcard:country-name\\": {\\"@value\\": \\"BE\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"vcard:Address\\", \\"vcard:street-address\\": {\\"@value\\": \\"Avenue des Arts 6-9\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:locality\\": {\\"@value\\": \\"Br\\üssel\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:postal-code\\": {\\"@value\\": \\"1210\\", \\"@type\\": \\"xsd:string\\"}}, \\"merlot:orgaName\\": {\\"@value\\": \\"Gaia-X AISBL\\", \\"@type\\": \\"xsd:string\\"}, \\"@id\\": \\"Participant:10\\", \\"@context\\": {\\"merlot\\": \\"http://w3id.org/gaia-x/merlot#\\", \\"gax-trust-framework\\": \\"http://w3id.org/gaia-x/gax-trust-framework#\\", \\"rdf\\": \\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\\", \\"sh\\": \\"http://www.w3.org/ns/shacl#\\", \\"xsd\\": \\"http://www.w3.org/2001/XMLSchema#\\", \\"gax-validation\\": \\"http://w3id.org/gaia-x/validation#\\", \\"skos\\": \\"http://www.w3.org/2004/02/skos/core#\\", \\"vcard\\": \\"http://www.w3.org/2006/vcard/ns#\\"}, \\"merlot:addressCode\\": {\\"@value\\": \\"BE-BRU\\", \\"@type\\": \\"xsd:string\\"}}, \\"issuanceDate\\": \\"2022-10-19T18:48:09Z\\", \\"@type\\": [\\"VerifiableCredential\\"], \\"@id\\": \\"https://www.example.org/legalPerson.json\\", \\"proof\\": {\\"created\\": \\"2023-04-27T13:48:10Z\\", \\"jws\\": \\"dummy\\", \\"proofPurpose\\": \\"assertionMethod\\", \\"type\\": \\"JsonWebSignature2020\\", \\"verificationMethod\\": \\"did:web:compliance.lab.gaia-x.eu\\"}, \\"@context\\": [\\"https://www.w3.org/2018/credentials/v1\\"], \\"issuer\\": \\"Participant:10\\"}}"
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
                            "id": "Participant:10",
                            "name": "Participant:10",
                            "publicKey": "{\\"kty\\":\\"RSA\\",\\"e\\":\\"AQAB\\",\\"alg\\":\\"PS256\\",\\"n\\":\\"dummy\\"}",
                            "selfDescription":"{\\"@id\\": \\"http://example.edu/verifiablePresentation/self-description1\\", \\"proof\\": {\\"created\\": \\"2023-04-27T13:48:11Z\\", \\"jws\\": \\"dummy\\", \\"proofPurpose\\": \\"assertionMethod\\", \\"type\\": \\"JsonWebSignature2020\\", \\"verificationMethod\\": \\"did:web:compliance.lab.gaia-x.eu\\"}, \\"type\\": [\\"VerifiablePresentation\\"], \\"@context\\": [\\"https://www.w3.org/2018/credentials/v1\\"], \\"verifiableCredential\\": {\\"credentialSubject\\": {\\"gax-trust-framework:registrationNumber\\": {\\"gax-trust-framework:local\\": {\\"@value\\": \\"0762747721\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"gax-trust-framework:RegistrationNumber\\"}, \\"gax-trust-framework:legalName\\": {\\"@value\\": \\"Gaia-X European Association for Data and Cloud AISBL\\", \\"@type\\": \\"xsd:string\\"}, \\"gax-trust-framework:headquarterAddress\\": {\\"vcard:country-name\\": {\\"@value\\": \\"BE\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"vcard:Address\\", \\"vcard:street-address\\": {\\"@value\\": \\"Avenue des Arts 6-9\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:locality\\": {\\"@value\\": \\"Br\\üssel\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:postal-code\\": {\\"@value\\": \\"1210\\", \\"@type\\": \\"xsd:string\\"}}, \\"gax-trust-framework:termsAndConditions\\": {\\"gax-trust-framework:content\\": {\\"@value\\": \\"http://example.com\\", \\"@type\\": \\"xsd:anyURI\\"}, \\"gax-trust-framework:hash\\": {\\"@value\\": \\"hash1234\\", \\"@type\\": \\"xsd:string\\"}}, \\"@type\\": \\"merlot:MerlotOrganization\\", \\"merlot:merlotId\\": {\\"@value\\": \\"10\\", \\"@type\\": \\"xsd:string\\"}, \\"gax-trust-framework:legalAddress\\": {\\"vcard:country-name\\": {\\"@value\\": \\"BE\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"vcard:Address\\", \\"vcard:street-address\\": {\\"@value\\": \\"Avenue des Arts 6-9\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:locality\\": {\\"@value\\": \\"Br\\üssel\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:postal-code\\": {\\"@value\\": \\"1210\\", \\"@type\\": \\"xsd:string\\"}}, \\"merlot:orgaName\\": {\\"@value\\": \\"Gaia-X AISBL\\", \\"@type\\": \\"xsd:string\\"}, \\"@id\\": \\"Participant:10\\", \\"@context\\": {\\"merlot\\": \\"http://w3id.org/gaia-x/merlot#\\", \\"gax-trust-framework\\": \\"http://w3id.org/gaia-x/gax-trust-framework#\\", \\"rdf\\": \\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\\", \\"sh\\": \\"http://www.w3.org/ns/shacl#\\", \\"xsd\\": \\"http://www.w3.org/2001/XMLSchema#\\", \\"gax-validation\\": \\"http://w3id.org/gaia-x/validation#\\", \\"skos\\": \\"http://www.w3.org/2004/02/skos/core#\\", \\"vcard\\": \\"http://www.w3.org/2006/vcard/ns#\\"}, \\"merlot:addressCode\\": {\\"@value\\": \\"BE-BRU\\", \\"@type\\": \\"xsd:string\\"}}, \\"issuanceDate\\": \\"2022-10-19T18:48:09Z\\", \\"@type\\": [\\"VerifiableCredential\\"], \\"@id\\": \\"https://www.example.org/legalPerson.json\\", \\"proof\\": {\\"created\\": \\"2023-04-27T13:48:10Z\\", \\"jws\\": \\"dummy\\", \\"proofPurpose\\": \\"assertionMethod\\", \\"type\\": \\"JsonWebSignature2020\\", \\"verificationMethod\\": \\"did:web:compliance.lab.gaia-x.eu\\"}, \\"@context\\": [\\"https://www.w3.org/2018/credentials/v1\\"], \\"issuer\\": \\"Participant:10\\"}}"
                        }
                    ]
                }
                """;
        mockUserResponse = StringEscapeUtils.unescapeJson(mockUserResponse);
        if (mockUserResponse != null)
            mockUserResponse = mockUserResponse.replace("\"{", "{").replace("}\"", "}");

        // for participant endpoint return a dummy list of one item
        lenient().when(keycloakAuthService.webCallAuthenticated(eq(HttpMethod.GET),
                        startsWith(gxfscatalogParticipantsUri + "?offset="), any(), any()))
                .thenReturn(mockUserResponse);
        lenient().when(keycloakAuthService.webCallAuthenticated(eq(HttpMethod.GET),
                        eq(URI.create(gxfscatalogParticipantsUri + "/Participant:10").toString()), any(), any()))
                .thenReturn(mockParticipant);

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

}

