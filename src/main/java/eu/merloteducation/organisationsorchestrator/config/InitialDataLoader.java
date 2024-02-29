package eu.merloteducation.organisationsorchestrator.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.authorizationlibrary.authorization.OrganizationRoleGrantedAuthority;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gax.participants.GaxTrustLegalPersonCredentialSubject;
import eu.merloteducation.modelslib.api.organization.MembershipClass;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.api.organization.OrganizationConnectorDto;
import eu.merloteducation.organisationsorchestrator.controller.OrganizationQueryController;
import eu.merloteducation.organisationsorchestrator.models.exceptions.NoInitDataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class InitialDataLoader implements CommandLineRunner {

    private final Logger logger = LoggerFactory.getLogger(InitialDataLoader.class);
    private final OrganizationQueryController organizationQueryController;
    private final ObjectMapper objectMapper;
    private final File initialOrgasFolder;
    private final File initialOrgaConnectorsResource;
    private final String merlotDomain;


    public InitialDataLoader(@Autowired OrganizationQueryController organizationQueryController,
                             @Autowired ObjectMapper objectMapper,
                             @Value("${init-data.organisations:#{null}}") File initialOrgasFolder,
                             @Value("${init-data.connectors:#{null}}") File initialOrgaConnectorsResource,
                             @Value("${merlot-domain}") String merlotDomain) {
        this.organizationQueryController = organizationQueryController;
        this.objectMapper = objectMapper;
        this.initialOrgasFolder = initialOrgasFolder;
        this.initialOrgaConnectorsResource = initialOrgaConnectorsResource;
        this.merlotDomain = merlotDomain;
    }


    @Override
    @Transactional
    public void run(String... args) {
        try {
            // MERLOT federation
            OrganizationRoleGrantedAuthority internalRoleFedAdmin
                    = new OrganizationRoleGrantedAuthority(
                            "FedAdmin_did:web:" + merlotDomain + "#df15587a-0760-32b5-9c42-bb7be66e8076");

            if (!organizationQueryController.getAllOrganizations(0, 1, internalRoleFedAdmin).getContent().isEmpty()) {
                logger.info("Database will not be reinitialised since organisations exist.");
                return;
            }
            logger.info("Initializing database since no organisations were found.");

            List<MultipartFile> orgaPdfs = new ArrayList<>();
            for (File orgaPdf : initialOrgasFolder.listFiles()) {
                try (FileInputStream input = new FileInputStream(orgaPdf)) {
                    orgaPdfs.add(new MockMultipartFile("formular", input.readAllBytes()));
                }
            }

            if (orgaPdfs.isEmpty()) {
                throw new NoInitDataException(("Failed to find any valid PDF files in " + initialOrgasFolder.getPath()));
            }

            Map<String, Set<OrganizationConnectorDto>> initialOrgaConnectors =
                    objectMapper.readValue(initialOrgaConnectorsResource, new TypeReference<>(){});

            for (MultipartFile orgaPdf : orgaPdfs) {
                MerlotParticipantDto participant = organizationQueryController
                        .createOrganization(new MultipartFile[]{orgaPdf}, internalRoleFedAdmin);

                // set federator role for initial organisations
                participant.getMetadata().setMembershipClass(MembershipClass.FEDERATOR);
                organizationQueryController.updateOrganization(participant, internalRoleFedAdmin);

                // add initial connectors as well
                String orgaLegalName = ((GaxTrustLegalPersonCredentialSubject) participant.getSelfDescription()
                        .getVerifiableCredential().getCredentialSubject()).getLegalName();
                if (initialOrgaConnectors.containsKey(orgaLegalName)) {
                    // if connectors exist, add them on behalf of the participant
                    participant.getMetadata().getConnectors().addAll(initialOrgaConnectors.get(orgaLegalName));
                    OrganizationRoleGrantedAuthority internalRoleOrgLegRep = new OrganizationRoleGrantedAuthority(
                            "OrgLegRep_" + participant.getId());
                    organizationQueryController.updateOrganization(participant, internalRoleOrgLegRep);
                }
            }

        } catch (Exception e) {
            logger.warn("Failed to import initial participant dataset. {}", e.getMessage());
        }
    }
}