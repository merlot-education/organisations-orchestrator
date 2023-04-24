package eu.merloteducation.organisationsorchestrator;

import eu.merloteducation.organisationsorchestrator.models.OrganizationModel;
import eu.merloteducation.organisationsorchestrator.service.GXFSCatalogRestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.will;
import static org.mockito.Mockito.*;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@EnableConfigurationProperties
public class GXFSCatalogRestServiceTests {

    @Mock
    private RestTemplate restTemplate;

    @Value("${keycloak.token-uri}")
    private String keycloakTokenUri;

    @Value("${keycloak.logout-uri}")
    private String keycloakLogoutUri;


    @Value("${gxfscatalog.participants-uri}")
    private String gxfscatalogParticipantsUri;
    @InjectMocks
    private GXFSCatalogRestService gxfsCatalogRestService;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(gxfsCatalogRestService, "keycloakTokenUri", keycloakTokenUri);
        ReflectionTestUtils.setField(gxfsCatalogRestService, "keycloakLogoutUri", keycloakLogoutUri);
        ReflectionTestUtils.setField(gxfsCatalogRestService, "gxfscatalogParticipantsUri", gxfscatalogParticipantsUri);
        lenient().when(restTemplate.postForObject(eq(keycloakTokenUri), any(), eq(String.class)))
                .thenReturn("{\"access_token\": \"1234\", \"refresh_token\": \"5678\"}");

        lenient().when(restTemplate.postForObject(eq(keycloakLogoutUri), any(), eq(String.class)))
                .thenReturn("");

        lenient().when(restTemplate.exchange(any(URI.class),
                        eq(HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(HttpClientErrorException.NotFound.class);

        String mockParticipant = """
                {
                    "id": "http://10",
                    "name": "http://10",
                    "publicKey": "{\\"kty\\":\\"RSA\\",\\"e\\":\\"AQAB\\",\\"alg\\":\\"PS256\\",\\"n\\":\\"dummy\\"}",
                    "selfDescription":"{\\"@id\\": \\"http://example.edu/verifiablePresentation/self-description1\\", \\"proof\\": {\\"created\\": \\"2023-04-24T12:51:18Z\\", \\"jws\\": \\"dummy\\", \\"proofPurpose\\": \\"assertionMethod\\", \\"type\\": \\"JsonWebSignature2020\\", \\"verificationMethod\\": \\"did:web:compliance.lab.gaia-x.eu\\"}, \\"type\\": [\\"VerifiablePresentation\\"], \\"@context\\": [\\"https://www.w3.org/2018/credentials/v1\\"], \\"verifiableCredential\\": {\\"credentialSubject\\": {\\"merlot:registrationNumber\\": {\\"@value\\": \\"1234\\", \\"@type\\": \\"xsd:string\\"}, \\"merlot:termsAndConditionsLink\\": {\\"@value\\": \\"\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"merlot:MerlotOrganization\\", \\"merlot:legalName\\": {\\"@value\\": \\"Gaia-X European Association for Data and Cloud AISBL\\", \\"@type\\": \\"xsd:string\\"}, \\"merlot:merlotID\\": {\\"@value\\": \\"10\\", \\"@type\\": \\"xsd:string\\"}, \\"merlot:orgaName\\": {\\"@value\\": \\"Gaia-X AISBL\\", \\"@type\\": \\"xsd:string\\"}, \\"@id\\": \\"http://10\\", \\"@context\\": {\\"cc\\": \\"http://creativecommons.org/ns#\\", \\"cred\\": \\"https://www.w3.org/2018/credentials#\\", \\"schema\\": \\"http://schema.org/\\", \\"void\\": \\"http://rdfs.org/ns/void#\\", \\"owl\\": \\"http://www.w3.org/2002/07/owl#\\", \\"xsd\\": \\"http://www.w3.org/2001/XMLSchema#\\", \\"gax-validation\\": \\"http://w3id.org/gaia-x/validation#\\", \\"skos\\": \\"http://www.w3.org/2004/02/skos/core#\\", \\"voaf\\": \\"http://purl.org/vocommons/voaf#\\", \\"rdfs\\": \\"http://www.w3.org/2000/01/rdf-schema#\\", \\"vcard\\": \\"http://www.w3.org/2006/vcard/ns#\\", \\"gax-core\\": \\"http://w3id.org/gaia-x/core#\\", \\"merlot\\": \\"https://w3id.org/gaia-x/merlot#\\", \\"dct\\": \\"http://purl.org/dc/terms/\\", \\"gax-trust-framework\\": \\"https://w3id.org/gaia-x/gax-trust-framework#\\", \\"rdf\\": \\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\\", \\"sh\\": \\"http://www.w3.org/ns/shacl#\\", \\"ids\\": \\"https://w3id.org/idsa/core/\\", \\"dcat\\": \\"http://www.w3.org/ns/dcat#\\", \\"vann\\": \\"http://purl.org/vocab/vann/\\", \\"did\\": \\"https://www.w3.org/TR/did-core/#\\", \\"foaf\\": \\"http://xmlns.com/foaf/0.1/\\"}, \\"merlot:addressCode\\": {\\"@value\\": \\"BE-BRU\\", \\"@type\\": \\"xsd:string\\"}, \\"merlot:legalAddress\\": {\\"vcard:country-name\\": {\\"@value\\": \\"BE\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"vcard:Address\\", \\"vcard:street-address\\": {\\"@value\\": \\"Avenue des Arts 6-9\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:locality\\": {\\"@value\\": \\"Br\\üssel\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:postal-code\\": {\\"@value\\": \\"1210\\", \\"@type\\": \\"xsd:string\\"}}}, \\"issuanceDate\\": \\"2022-10-19T18:48:09Z\\", \\"@type\\": [\\"VerifiableCredential\\"], \\"@id\\": \\"https://www.example.org/legalPerson.json\\", \\"proof\\": {\\"created\\": \\"2023-04-24T12:51:18Z\\", \\"jws\\": \\"dummy\\", \\"proofPurpose\\": \\"assertionMethod\\", \\"type\\": \\"JsonWebSignature2020\\", \\"verificationMethod\\": \\"did:web:compliance.lab.gaia-x.eu\\"}, \\"@context\\": [\\"https://www.w3.org/2018/credentials/v1\\"], \\"issuer\\": \\"http://10\\"}}"
                }
                """;


        String mockUserResponse = """
                {
                    "totalCount": 1,
                    "items": [
                        {
                            "id": "http://10",
                            "name": "http://10",
                            "publicKey": "{\\"kty\\":\\"RSA\\",\\"e\\":\\"AQAB\\",\\"alg\\":\\"PS256\\",\\"n\\":\\"dummy\\"}",
                            "selfDescription":"{\\"@id\\": \\"http://example.edu/verifiablePresentation/self-description1\\", \\"proof\\": {\\"created\\": \\"2023-04-24T12:51:18Z\\", \\"jws\\": \\"dummy\\", \\"proofPurpose\\": \\"assertionMethod\\", \\"type\\": \\"JsonWebSignature2020\\", \\"verificationMethod\\": \\"did:web:compliance.lab.gaia-x.eu\\"}, \\"type\\": [\\"VerifiablePresentation\\"], \\"@context\\": [\\"https://www.w3.org/2018/credentials/v1\\"], \\"verifiableCredential\\": {\\"credentialSubject\\": {\\"merlot:registrationNumber\\": {\\"@value\\": \\"1234\\", \\"@type\\": \\"xsd:string\\"}, \\"merlot:termsAndConditionsLink\\": {\\"@value\\": \\"\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"merlot:MerlotOrganization\\", \\"merlot:legalName\\": {\\"@value\\": \\"Gaia-X European Association for Data and Cloud AISBL\\", \\"@type\\": \\"xsd:string\\"}, \\"merlot:merlotID\\": {\\"@value\\": \\"10\\", \\"@type\\": \\"xsd:string\\"}, \\"merlot:orgaName\\": {\\"@value\\": \\"Gaia-X AISBL\\", \\"@type\\": \\"xsd:string\\"}, \\"@id\\": \\"http://10\\", \\"@context\\": {\\"cc\\": \\"http://creativecommons.org/ns#\\", \\"cred\\": \\"https://www.w3.org/2018/credentials#\\", \\"schema\\": \\"http://schema.org/\\", \\"void\\": \\"http://rdfs.org/ns/void#\\", \\"owl\\": \\"http://www.w3.org/2002/07/owl#\\", \\"xsd\\": \\"http://www.w3.org/2001/XMLSchema#\\", \\"gax-validation\\": \\"http://w3id.org/gaia-x/validation#\\", \\"skos\\": \\"http://www.w3.org/2004/02/skos/core#\\", \\"voaf\\": \\"http://purl.org/vocommons/voaf#\\", \\"rdfs\\": \\"http://www.w3.org/2000/01/rdf-schema#\\", \\"vcard\\": \\"http://www.w3.org/2006/vcard/ns#\\", \\"gax-core\\": \\"http://w3id.org/gaia-x/core#\\", \\"merlot\\": \\"https://w3id.org/gaia-x/merlot#\\", \\"dct\\": \\"http://purl.org/dc/terms/\\", \\"gax-trust-framework\\": \\"https://w3id.org/gaia-x/gax-trust-framework#\\", \\"rdf\\": \\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\\", \\"sh\\": \\"http://www.w3.org/ns/shacl#\\", \\"ids\\": \\"https://w3id.org/idsa/core/\\", \\"dcat\\": \\"http://www.w3.org/ns/dcat#\\", \\"vann\\": \\"http://purl.org/vocab/vann/\\", \\"did\\": \\"https://www.w3.org/TR/did-core/#\\", \\"foaf\\": \\"http://xmlns.com/foaf/0.1/\\"}, \\"merlot:addressCode\\": {\\"@value\\": \\"BE-BRU\\", \\"@type\\": \\"xsd:string\\"}, \\"merlot:legalAddress\\": {\\"vcard:country-name\\": {\\"@value\\": \\"BE\\", \\"@type\\": \\"xsd:string\\"}, \\"@type\\": \\"vcard:Address\\", \\"vcard:street-address\\": {\\"@value\\": \\"Avenue des Arts 6-9\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:locality\\": {\\"@value\\": \\"Br\\üssel\\", \\"@type\\": \\"xsd:string\\"}, \\"vcard:postal-code\\": {\\"@value\\": \\"1210\\", \\"@type\\": \\"xsd:string\\"}}}, \\"issuanceDate\\": \\"2022-10-19T18:48:09Z\\", \\"@type\\": [\\"VerifiableCredential\\"], \\"@id\\": \\"https://www.example.org/legalPerson.json\\", \\"proof\\": {\\"created\\": \\"2023-04-24T12:51:18Z\\", \\"jws\\": \\"dummy\\", \\"proofPurpose\\": \\"assertionMethod\\", \\"type\\": \\"JsonWebSignature2020\\", \\"verificationMethod\\": \\"did:web:compliance.lab.gaia-x.eu\\"}, \\"@context\\": [\\"https://www.w3.org/2018/credentials/v1\\"], \\"issuer\\": \\"http://10\\"}}"
                        }
                    ]
                }
                """;
        // for participant endpoint return a dummy list of one item
        lenient().when(restTemplate.exchange(eq(gxfscatalogParticipantsUri),
                        eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(mockUserResponse, HttpStatus.OK));
        lenient().when(restTemplate.exchange(eq(URI.create(gxfscatalogParticipantsUri + "/http%3A%2F%2F10")),
                eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(mockParticipant, HttpStatus.OK));

    }

    @Test
    public void getAllParticipants() throws Exception {

        List<OrganizationModel> organizations = gxfsCatalogRestService.getParticipants();
        assertThat(organizations, isA(List.class));
        assertThat(organizations, not(empty()));

    }

    @Test
    public void getParticipantById() throws Exception {
        OrganizationModel organization = gxfsCatalogRestService.getParticipantById("10");
        assertThat(organization, isA(OrganizationModel.class));
        assertEquals("10", organization.getMerlotId());
        assertEquals("Gaia-X European Association for Data and Cloud AISBL", organization.getOrganizationLegalName());
    }

    @Test
    public void getParticipantByInvalidId() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> gxfsCatalogRestService.getParticipantById("asdf"));
    }

    @Test
    public void getParticipantByNonexistentId() throws Exception {
        assertThrows(HttpClientErrorException.NotFound.class, () -> gxfsCatalogRestService.getParticipantById("11"));
    }

}

