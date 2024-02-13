package eu.merloteducation.organisationsorchestrator.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import eu.merloteducation.authorizationlibrary.authorization.OrganizationRoleGrantedAuthority;
import eu.merloteducation.gxfscataloglibrary.service.GxfsSignerService;
import eu.merloteducation.modelslib.api.organization.MembershipClass;
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
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class InitialDataLoader implements CommandLineRunner {

    private final Logger logger = LoggerFactory.getLogger(InitialDataLoader.class);

    private final ParticipantService participantService;
    private final ParticipantConnectorsService participantConnectorsService;
    private final ObjectMapper objectMapper;
    private final File initialOrgasResource;
    private final File initialOrgaConnectorsResource;
    private final String poolEdc1Token;
    private final String poolEdc2Token;
    private final String merlotDomain;

    private static final String LEGAL_ADDRESS = "legalAddress";


    public InitialDataLoader(@Autowired ParticipantService participantService,
                             @Autowired ParticipantConnectorsService participantConnectorsService,
                             @Autowired ObjectMapper objectMapper,
                             @Value("${init-data.organisations:#{null}}") File initialOrgasResource,
                             @Value("${init-data.connectors:#{null}}") File initialOrgaConnectorsResource,
                             @Value("${edc-tokens.edc1:#{null}}") String poolEdc1Token,
                             @Value("${edc-tokens.edc2:#{null}}") String poolEdc2Token,
                             @Value("${merlot-domain}") String merlotDomain) {
        this.participantService = participantService;
        this.participantConnectorsService = participantConnectorsService;
        this.objectMapper = objectMapper;
        this.initialOrgasResource = initialOrgasResource;
        this.initialOrgaConnectorsResource = initialOrgaConnectorsResource;
        this.poolEdc1Token = poolEdc1Token;
        this.poolEdc2Token = poolEdc2Token;
        this.merlotDomain = merlotDomain;
    }


    @Override
    @Transactional
    public void run(String... args) {
        try {
            // MERLOT federation
            OrganizationRoleGrantedAuthority internalRole
                    = new OrganizationRoleGrantedAuthority(
                            "FedAdmin_did:web:" + merlotDomain + "#df15587a-0760-32b5-9c42-bb7be66e8076");
            if (!participantService.getParticipants(Pageable.ofSize(1), internalRole).getContent().isEmpty()) {
                logger.info("Database will not be reinitialised since organisations exist.");
                return;
            }
            logger.info("Initializing database since no organisations were found.");

            ArrayNode initialOrgas;
            JsonNode initialOrgaConnectors;
            try {
                initialOrgas = (ArrayNode) objectMapper.readTree(initialOrgasResource); // TODO replace this with the pdf reader
                initialOrgaConnectors = objectMapper.readTree(initialOrgaConnectorsResource);
            } catch (Exception e) {
                logger.warn("Failed to load initial dataset, loading example dataset instead... {}", e.getMessage());
                initialOrgas = (ArrayNode) objectMapper.readTree(
                        InitialDataLoader.class.getClassLoader().getResourceAsStream("initial-orgas.json"));
                initialOrgaConnectors = objectMapper.readTree(
                        InitialDataLoader.class.getClassLoader().getResourceAsStream("initial-orga-connectors.json"));
            }

            for (JsonNode orga : initialOrgas) {
                RegistrationFormContent content = new RegistrationFormContent();

                content.setOrganizationName(orga.get("organizationName").textValue());
                content.setOrganizationLegalName(orga.get("organizationLegalName").textValue());
                content.setRegistrationNumberLocal(orga.get("registrationNumber").textValue());
                content.setMailAddress(orga.get("mailAddress").textValue());

                content.setStreet(orga.get(LEGAL_ADDRESS).get("street").textValue());
                content.setCity(orga.get(LEGAL_ADDRESS).get("city").textValue());
                content.setPostalCode(orga.get(LEGAL_ADDRESS).get("postalCode").textValue());
                content.setCountryCode(orga.get(LEGAL_ADDRESS).get("countryCode").textValue());

                content.setProviderTncLink(orga.get("termsAndConditionsLink").textValue());
                content.setProviderTncHash(orga.get("termsAndConditionsHash").textValue());
                MerlotParticipantDto participant = participantService.createParticipant(content);

                // set federator role for initial organisations
                participant.getMetadata().setMembershipClass(MembershipClass.FEDERATOR);
                participant = participantService.updateParticipant(participant, internalRole);

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