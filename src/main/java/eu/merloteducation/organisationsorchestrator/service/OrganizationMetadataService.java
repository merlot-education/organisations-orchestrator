package eu.merloteducation.organisationsorchestrator.service;

import eu.merloteducation.modelslib.api.organization.MembershipClass;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantMetaDto;
import eu.merloteducation.organisationsorchestrator.mappers.OrganizationMapper;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganizationMetadata;
import eu.merloteducation.organisationsorchestrator.models.exceptions.ParticipantConflictException;
import eu.merloteducation.organisationsorchestrator.repositories.OrganisationConnectorsExtensionRepository;
import eu.merloteducation.organisationsorchestrator.repositories.OrganizationMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrganizationMetadataService {
    @Autowired
    private OrganizationMetadataRepository repository;

    @Autowired
    private OrganisationConnectorsExtensionRepository connectorRepository;

    @Autowired
    private OrganizationMapper mapper;

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

        dbMetadata.getConnectors().forEach(connector -> connectorRepository.save(connector));

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
            .map(orgaMetadata -> mapper.organizationMetadataToMerlotParticipantMetaDto(orgaMetadata)).toList();
    }

    /**
     * Return the ids of all inactive participants.
     *
     * @return list of ids
     */
    public List<String> getInactiveParticipantsIds() {

        return repository.findByActive(false);
    }

}