package eu.merloteducation.organisationsorchestrator.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import eu.merloteducation.gxfscataloglibrary.models.exception.CredentialPresentationException;
import eu.merloteducation.gxfscataloglibrary.models.exception.CredentialSignatureException;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.api.organization.OrganizationConnectorDto;
import eu.merloteducation.modelslib.api.organization.PostOrganisationConnectorModel;
import eu.merloteducation.organisationsorchestrator.models.RegistrationFormContent;
import eu.merloteducation.organisationsorchestrator.service.ParticipantConnectorsService;
import eu.merloteducation.organisationsorchestrator.service.ParticipantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class InitialDataLoader implements CommandLineRunner {

    private final Logger logger = LoggerFactory.getLogger(InitialDataLoader.class);

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private ParticipantConnectorsService participantConnectorsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("classpath:initial-orgas.json")
    private Resource initialOrgasResource;

    @Value("classpath:initial-orga-connectors.json")
    private Resource initialOrgaConnectorsResource;

    @Value("${edc-tokens.edc1:#{null}}")
    private String poolEdc1Token;

    @Value("${edc-tokens.edc2:#{null}}")
    private String poolEdc2Token;


    @Override
    public void run(String... args) throws IOException, CredentialSignatureException, CredentialPresentationException {
        try {
            if (!participantService.getParticipants(Pageable.ofSize(1)).getContent().isEmpty()) {
                logger.info("Database will not be reinitialised since organisations exist.");
                return;
            }
            logger.info("Initializing database since no organisations were found.");

            ArrayNode initialOrgas = (ArrayNode) objectMapper.readTree(initialOrgasResource.getFile()); // TODO replace this with the pdf reader
            JsonNode initialOrgaConnectors = objectMapper.readTree(initialOrgaConnectorsResource.getFile());

            for (JsonNode orga : initialOrgas) {
                RegistrationFormContent content = new RegistrationFormContent();

                content.setOrganizationName(orga.get("organizationName").textValue());
                content.setOrganizationLegalName(orga.get("organizationLegalName").textValue());
                content.setRegistrationNumberLocal(orga.get("registrationNumber").textValue());
                content.setMailAddress(orga.get("mailAddress").textValue());

                content.setStreet(orga.get("legalAddress").get("street").textValue());
                content.setCity(orga.get("legalAddress").get("city").textValue());
                content.setPostalCode(orga.get("legalAddress").get("postalCode").textValue());
                content.setCountryCode(orga.get("legalAddress").get("countryCode").textValue());

                content.setProviderTncLink(orga.get("termsAndConditionsLink").textValue());
                content.setProviderTncHash(orga.get("termsAndConditionsHash").textValue());
                MerlotParticipantDto participant = participantService.createParticipant(content);

                // check if we need to add connectors as well
                List<OrganizationConnectorDto> existingConnectors =
                        participantConnectorsService.getAllConnectors(participant.getId());

                // collect buckets of this orga
                ArrayNode orgaBucketsNode = (ArrayNode) initialOrgaConnectors.get(content.getOrganizationLegalName());
                List<String> orgaBuckets = new ArrayList<>();
                for (JsonNode bucket : orgaBucketsNode) {
                    orgaBuckets.add(bucket.textValue());
                }

                // only add if we have no existing connectors and we have buckets
                if (!existingConnectors.isEmpty() || orgaBuckets.isEmpty()) {
                    continue;
                }


                // add pool edcs with the found buckets
                PostOrganisationConnectorModel connector1 = new PostOrganisationConnectorModel();
                connector1.setConnectorId("edc1");
                connector1.setConnectorEndpoint("http://edc-1.merlot.svc.cluster.local");
                connector1.setConnectorAccessToken(poolEdc1Token);
                connector1.setBucketNames(orgaBuckets);

                participantConnectorsService.postConnector(participant.getId(), connector1);

                PostOrganisationConnectorModel connector2 = new PostOrganisationConnectorModel();
                connector2.setConnectorId("edc2");
                connector2.setConnectorEndpoint("http://edc-2.merlot.svc.cluster.local");
                connector2.setConnectorAccessToken(poolEdc2Token);
                connector2.setBucketNames(orgaBuckets);

                participantConnectorsService.postConnector(participant.getId(), connector2);
            }

        } catch (Exception e) {
            logger.warn("Failed to import initial participant dataset. {}", e.getMessage());
        }
    }
}