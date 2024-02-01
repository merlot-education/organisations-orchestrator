package eu.merloteducation.organisationsorchestrator.service;

import eu.merloteducation.modelslib.api.organization.MembershipClass;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantMetaDto;
import eu.merloteducation.organisationsorchestrator.mappers.OrganizationMapper;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganizationMetadata;
import eu.merloteducation.organisationsorchestrator.repositories.OrganizationMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrganizationMetadataService {
    @Autowired
    private OrganizationMetadataRepository repository;

    @Autowired
    private OrganizationMapper mapper;

    public MerlotParticipantMetaDto getMerlotParticipantMetaDto(String merlotId) {

        merlotId = merlotId.replace("Participant:", "");

        OrganizationMetadata dbMeta = repository.findByOrgaId(merlotId).orElse(null);

        return mapper.organizationMetadataToMerlotParticipantMetaDto(dbMeta);
    }

    public MerlotParticipantMetaDto saveMerlotParticipantMeta(MerlotParticipantMetaDto metaDto) {

        OrganizationMetadata metadata = mapper.merlotParticipantMetaDtoToOrganizationMetadata(metaDto);
        return mapper.organizationMetadataToMerlotParticipantMetaDto(repository.save(metadata));
    }

    public MerlotParticipantMetaDto updateMerlotParticipantMeta(MerlotParticipantMetaDto metaDtoWithEdits) {

        String orgaId = metaDtoWithEdits.getOrgaId().replace("Participant:", "");

        OrganizationMetadata dbMetadata = repository.findByOrgaId(orgaId).orElse(null);

        if (dbMetadata == null) {
            return null;
        }

        mapper.updateOrganizationMetadataWithMerlotParticipantMetaDto(metaDtoWithEdits, dbMetadata);

        return mapper.organizationMetadataToMerlotParticipantMetaDto(repository.save(dbMetadata));
    }

    public List<MerlotParticipantMetaDto> getParticipantsByMembershipClass(MembershipClass membershipClass) {

        List<OrganizationMetadata> orgaMetadataList = repository.findByMembershipClass(membershipClass);
        return orgaMetadataList.stream()
            .map(orgaMetadata -> mapper.organizationMetadataToMerlotParticipantMetaDto(orgaMetadata)).toList();
    }

    public List<String> getInactiveParticipants() {

        return repository.findByActive(false);
    }

}