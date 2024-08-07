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

package eu.merloteducation.organisationsorchestrator.service;

import eu.merloteducation.modelslib.api.organization.MembershipClass;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantMetaDto;
import eu.merloteducation.modelslib.api.organization.OrganizationConnectorTransferDto;
import eu.merloteducation.organisationsorchestrator.mappers.OrganizationMapper;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganisationConnectorExtension;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganizationMetadata;
import eu.merloteducation.organisationsorchestrator.models.exceptions.ParticipantConflictException;
import eu.merloteducation.organisationsorchestrator.repositories.OrganizationMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrganizationMetadataService {
    private final OrganizationMetadataRepository repository;
    private final OrganizationMapper mapper;

    public OrganizationMetadataService(@Autowired OrganizationMetadataRepository repository,
                                       @Autowired OrganizationMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * Given a participant's id, return its metadata.
     *
     * @param orgaId the id of the participant
     * @return metadata of the participant
     */
    public MerlotParticipantMetaDto getMerlotParticipantMetaDto(String orgaId) {

        OrganizationMetadata dbMeta = repository.findById(orgaId).orElse(null);

        return mapper.organizationMetadataToMerlotParticipantMetaDto(dbMeta);
    }

    /**
     * Given the metadata for a new participant, attempt to save the metadata in the database.
     *
     * @param metaDto dto with metadata of the new participant
     * @return metadata of the new participant
     */
    public MerlotParticipantMetaDto saveMerlotParticipantMeta(MerlotParticipantMetaDto metaDto) {

        OrganizationMetadata dbMeta = repository.findById(metaDto.getOrgaId()).orElse(null);
        if (dbMeta != null) {
            throw new ParticipantConflictException("Participant with this id already exists");
        }
        OrganizationMetadata metadata = mapper.merlotParticipantMetaDtoToOrganizationMetadata(metaDto);

        return mapper.organizationMetadataToMerlotParticipantMetaDto(repository.save(metadata));
    }

    /**
     * Given the edited metadata of a participant, attempt to update the metadata in the database.
     *
     * @param metaDtoWithEdits dto with updated fields
     * @return metadata of the participant
     */
    public MerlotParticipantMetaDto updateMerlotParticipantMeta(MerlotParticipantMetaDto metaDtoWithEdits) {

        String orgaId = metaDtoWithEdits.getOrgaId();

        OrganizationMetadata dbMetadata = repository.findById(orgaId).orElse(null);

        if (dbMetadata == null) {
            return null;
        }

        mapper.updateOrganizationMetadataWithMerlotParticipantMetaDto(metaDtoWithEdits, dbMetadata);

        return mapper.organizationMetadataToMerlotParticipantMetaDto(repository.save(dbMetadata));
    }

    /**
     * Given a membership class, return the participants with that membership class.
     *
     * @param membershipClass membership class
     * @return list of participants
     */
    public List<MerlotParticipantMetaDto> getParticipantsByMembershipClass(MembershipClass membershipClass) {

        List<OrganizationMetadata> orgaMetadataList = repository.findByMembershipClass(membershipClass);
        return orgaMetadataList.stream()
            .map(mapper::organizationMetadataToMerlotParticipantMetaDto).toList();
    }

    /**
     * Return the ids of all inactive participants.
     *
     * @return list of ids
     */
    public List<String> getInactiveParticipantsIds() {
        return repository.getOrgaIdByActive(false);
    }

    /**
     * Given a participant's id and the connector id, return the connector.
     *
     * @param orgaId the id of the participant
     * @param connectorId the connector id
     * @return connector
     */
    public OrganizationConnectorTransferDto getConnectorForParticipant(String orgaId, String connectorId) {

        OrganizationMetadata dbMeta = repository.findById(orgaId).orElse(null);
        OrganisationConnectorExtension connector = null;

        if (dbMeta != null) {
            connector = dbMeta.getConnectors().stream()
                .filter(con -> con.getConnectorId().equals(connectorId))
                .findFirst().orElse(null);
        }

        return connector != null ? mapper.connectorExtensionToOrganizationConnectorTransferDto(connector) : null;
    }

    /**
     * Given a membership class, return the ids of the participants with that membership class.
     *
     * @param membershipClass membership class
     * @return list of participant ids
     */
    public List<String> getParticipantIdsByMembershipClass(MembershipClass membershipClass) {

        return repository.getOrgaIdByMembershipClass(membershipClass);
    }

}