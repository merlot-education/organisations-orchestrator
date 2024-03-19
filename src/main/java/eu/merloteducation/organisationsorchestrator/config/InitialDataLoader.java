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
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

@Component
public class InitialDataLoader implements CommandLineRunner {

    private static final String MERLOT_FED_DOC_FILENAME = "merlotRegistrationForm_MERLOT.pdf";
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
    public void run(String... args) {
        try {
            // MERLOT federation
            OrganizationRoleGrantedAuthority merlotFederationRole
                    = new OrganizationRoleGrantedAuthority(
                            "FedAdmin_did:web:" + merlotDomain + ":participant:df15587a-0760-32b5-9c42-bb7be66e8076");

            if (!organizationQueryController.getAllOrganizations(0, 1, merlotFederationRole).getContent().isEmpty()) {
                logger.info("Database will not be reinitialised since organisations exist.");
                return;
            }
            logger.info("Initializing database since no organisations were found.");

            // onboard MERLOT federation first to get key for further signatures
            onboardMerlotFederation(merlotFederationRole);

            // onboard other organisations as role of MERLOT Federation
            onboardOtherOrganisations(merlotFederationRole);

        } catch (Exception e) {
            logger.warn("Failed to import initial participant dataset. {}", e.getMessage());
        }
    }

    private void onboardMerlotFederation(OrganizationRoleGrantedAuthority merlotFederationRole) throws Exception {
        MultipartFile merlotFederationPdf = getMerlotFederationDocument();
        MerlotParticipantDto participant = organizationQueryController
                .createOrganization(new MultipartFile[]{merlotFederationPdf}, merlotFederationRole);
        // set federator role for MERLOT federation
        participant.getMetadata().setMembershipClass(MembershipClass.FEDERATOR);
        organizationQueryController.updateOrganization(participant, merlotFederationRole);
    }

    private void onboardOtherOrganisations(OrganizationRoleGrantedAuthority merlotFederationRole) throws Exception {
        List<MultipartFile> orgaPdfs = getOrganisationDocuments();

        // check if we have initial data for connectors
        Map<String, Set<OrganizationConnectorDto>> initialOrgaConnectors = Collections.emptyMap();
        if (initialOrgaConnectorsResource != null) {
            initialOrgaConnectors =
                    objectMapper.readValue(initialOrgaConnectorsResource, new TypeReference<>(){});
        }

        for (MultipartFile orgaPdf : orgaPdfs) {
            MerlotParticipantDto participant = organizationQueryController
                    .createOrganization(new MultipartFile[]{orgaPdf}, merlotFederationRole);

            // add initial connectors as well
            String orgaLegalName = ((GaxTrustLegalPersonCredentialSubject) participant.getSelfDescription()
                    .getVerifiableCredential().getCredentialSubject()).getLegalName();
            if (initialOrgaConnectors.containsKey(orgaLegalName)) {
                // if connectors exist, add them on behalf of the participant
                participant.getMetadata().getConnectors().addAll(initialOrgaConnectors.get(orgaLegalName));
                OrganizationRoleGrantedAuthority internalRoleOrgLegRep = new OrganizationRoleGrantedAuthority(
                        "OrgLegRep_" + participant.getId());
                // update with the role of the participant in order to update the connector data
                organizationQueryController.updateOrganization(participant, internalRoleOrgLegRep);
            }

            // set federator role for initial organisations and reset signature to MERLOT
            participant.getMetadata().setMembershipClass(MembershipClass.FEDERATOR);
            organizationQueryController.updateOrganization(participant, merlotFederationRole);
        }
    }

    private MultipartFile getMerlotFederationDocument() {
        File merlotFederationPdfFile = Arrays.stream(initialOrgasFolder.listFiles())
                .filter(f -> f.getName().equals(MERLOT_FED_DOC_FILENAME)).findFirst().orElse(null);
        try (FileInputStream input = new FileInputStream(merlotFederationPdfFile)) {
            return new MockMultipartFile("formular", input.readAllBytes());
        } catch (IOException | NullPointerException e) {
            throw new NoInitDataException(("Failed to find merlot federation PDF in " + initialOrgasFolder.getPath()));
        }
    }

    private List<MultipartFile> getOrganisationDocuments() {
        List<MultipartFile> orgaPdfs = new ArrayList<>();
        for (File orgaPdf : initialOrgasFolder.listFiles()) {
            // skip merlot federation
            if (!orgaPdf.getName().equals("merlotRegistrationForm_MERLOT.pdf")) {
                try (FileInputStream input = new FileInputStream(orgaPdf)) {

                    MultipartFile file = new MockMultipartFile("formular", input.readAllBytes());
                    orgaPdfs.add(file);
                } catch (IOException e) {
                    logger.warn("Failed to read file {}: {}", orgaPdf.getName(), e.getMessage());
                }
            }
        }

        if (orgaPdfs.isEmpty()) {
            throw new NoInitDataException(("Failed to find any valid PDF files in " + initialOrgasFolder.getPath()));
        }

        return orgaPdfs;
    }
}