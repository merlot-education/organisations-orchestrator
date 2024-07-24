/*
 *  Copyright 2024 Dataport. All rights reserved. Developed as part of the MERLOT project.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.merloteducation.organisationsorchestrator.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.merloteducation.authorizationlibrary.authorization.OrganizationRole;
import eu.merloteducation.authorizationlibrary.authorization.OrganizationRoleGrantedAuthority;
import eu.merloteducation.modelslib.api.organization.MembershipClass;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.organisationsorchestrator.controller.OrganizationQueryController;
import eu.merloteducation.organisationsorchestrator.models.exceptions.NoInitDataException;
import eu.merloteducation.organisationsorchestrator.service.ParticipantService;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class InitialDataLoader implements CommandLineRunner {

    private static final String MERLOT_FED_DOC_FILENAME = "merlotRegistrationForm_MERLOT.pdf";
    private final OrganizationQueryController organizationQueryController;
    private final ParticipantService participantService;
    private File initialFederatorsFolder;
    private File initialParticipantsFolder;
    private File merlotFederationRegistrationFormFile;
    private final String merlotFederationDid;
    private final int delayUpdateTime;
    private static final String DELAY_UPDATE_MSG = "Delaying update to avoid clearing house rate limiting...";


    public InitialDataLoader(@Autowired OrganizationQueryController organizationQueryController,
                             @Autowired ParticipantService participantService,
                             @Value("${init-data.organisations:#{null}}") File initialOrgasFolder,
                             @Value("${init-data.gxdch-delay:#{0}}") int delayUpdateTime,
                             @Value("${merlot-federation-did}") String merlotFederationDid) {
        this.organizationQueryController = organizationQueryController;
        this.participantService = participantService;
        for (File file : initialOrgasFolder.listFiles()) {
            if (!file.isFile() && file.getName().equals("federators")) {
                this.initialFederatorsFolder = file;
            } else if (!file.isFile() && file.getName().equals("participants")) {
                this.initialParticipantsFolder = file;
            } else if (file.isFile() && file.getName().equals(MERLOT_FED_DOC_FILENAME)) {
                this.merlotFederationRegistrationFormFile = file;
            }
        }
        this.delayUpdateTime = delayUpdateTime;
        this.merlotFederationDid = merlotFederationDid;
    }


    @Override
    public void run(String... args) {
        try {
            // MERLOT federation
            OrganizationRoleGrantedAuthority merlotFederationRole
                    = new OrganizationRoleGrantedAuthority(OrganizationRole.FED_ADMIN, merlotFederationDid);

            if (!organizationQueryController.getAllOrganizations(0, 1, merlotFederationRole).getContent().isEmpty()) {
                log.info("Database will not be reinitialised since organisations exist.");
                return;
            }
            log.info("Initializing database since no organisations were found.");

            // onboard MERLOT federation first to get key for further signatures
            onboardMerlotFederation(merlotFederationRole);

            // onboard other organisations as role of MERLOT Federation
            onboardOtherOrganisations(merlotFederationRole);

        } catch (Exception e) {
            log.warn("Failed to import initial participant dataset. {}", e.getMessage());
        }
    }

    private void onboardMerlotFederation(OrganizationRoleGrantedAuthority merlotFederationRole) throws JsonProcessingException {
        MultipartFile merlotFederationPdf = getMerlotFederationDocument();
        // onboard merlot as merlot federation
        MerlotParticipantDto participant = organizationQueryController
                .createOrganization(new MultipartFile[]{merlotFederationPdf}, merlotFederationRole);
        delayClearingHouse();

        // set federator role for MERLOT federation
        participant.getMetadata().setMembershipClass(MembershipClass.FEDERATOR);
        // update membership class for this initial orga using service as federators cannot edit themselves
        participantService.updateParticipant(participant, merlotFederationRole);
        delayClearingHouse();
    }

    private void onboardOtherOrganisations(OrganizationRoleGrantedAuthority merlotFederationRole) throws JsonProcessingException {
        List<MultipartFile> participantPdfs = getOrganisationDocuments(initialParticipantsFolder);
        List<MultipartFile> federatorPdfs = getOrganisationDocuments(initialFederatorsFolder);

        for (MultipartFile orgaPdf : participantPdfs) {
            importOrganisation(merlotFederationRole, orgaPdf, false);
        }

        for (MultipartFile orgaPdf : federatorPdfs) {
            importOrganisation(merlotFederationRole, orgaPdf, true);
        }
    }

    private void importOrganisation(OrganizationRoleGrantedAuthority merlotFederationRole,
                                    MultipartFile orgaPdf,
                                    boolean isFederator) throws JsonProcessingException {
        MerlotParticipantDto participant = organizationQueryController
                .createOrganization(new MultipartFile[]{orgaPdf}, merlotFederationRole);

        delayClearingHouse();

        if (isFederator) {
            // set federator role for initial organisations
            participant.getMetadata().setMembershipClass(MembershipClass.FEDERATOR);
        }

        // reset signature to MERLOT Federation again
        organizationQueryController.updateOrganization(participant, merlotFederationRole);
        delayClearingHouse();
    }

    private MultipartFile getMerlotFederationDocument() {
        try (FileInputStream input = new FileInputStream(merlotFederationRegistrationFormFile)) {
            return new MockMultipartFile("formular", input.readAllBytes());
        } catch (IOException | NullPointerException e) {
            throw new NoInitDataException(("Failed to find merlot federation PDF at "
                    + merlotFederationRegistrationFormFile.getPath()));
        }
    }

    private List<MultipartFile> getOrganisationDocuments(File folder) {
        List<MultipartFile> orgaPdfs = new ArrayList<>();
        for (File orgaPdf : folder.listFiles()) {
            try (FileInputStream input = new FileInputStream(orgaPdf)) {

                MultipartFile file = new MockMultipartFile("formular", input.readAllBytes());
                orgaPdfs.add(file);
            } catch (IOException e) {
                log.warn("Failed to read file {}: {}", orgaPdf.getName(), e.getMessage());
            }
        }

        if (orgaPdfs.isEmpty()) {
            throw new NoInitDataException(("Failed to find any valid PDF files in " + folder.getPath()));
        }

        return orgaPdfs;
    }

    private void delayClearingHouse() {
        try {
            log.info(DELAY_UPDATE_MSG);
            Thread.sleep(delayUpdateTime);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}