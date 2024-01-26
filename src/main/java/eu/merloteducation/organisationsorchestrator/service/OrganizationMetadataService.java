package eu.merloteducation.organisationsorchestrator.service;

import eu.merloteducation.modelslib.api.organization.MembershipClass;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantMetaDto;
import eu.merloteducation.organisationsorchestrator.mappers.OrganizationMetadataMapper;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganizationMetadata;
import eu.merloteducation.organisationsorchestrator.repositories.OrganizationMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrganizationMetadataService {
    @Autowired
    private OrganizationMetadataRepository metadataRepository;

    @Autowired
    private OrganizationMetadataMapper metadataMapper;

    public MerlotParticipantMetaDto getMerlotParticipantMetaDto(String merlotId) {

        merlotId = merlotId.replace("Participant:", "");

        OrganizationMetadata dbMeta = metadataRepository.findByOrgaId(merlotId).orElse(null);

        return metadataMapper.organizationMetadataToMerlotParticipantMetaDto(dbMeta);
    }

    public MerlotParticipantMetaDto saveMerlotParticipantMeta(MerlotParticipantMetaDto metaDto) {
        OrganizationMetadata metadata = metadataMapper.merlotParticipantMetaDtoToOrganizationMetadata(metaDto);
        return metadataMapper.organizationMetadataToMerlotParticipantMetaDto(metadataRepository.save(metadata));
    }

    public MerlotParticipantMetaDto updateMerlotParticipantMeta(MerlotParticipantMetaDto metaDtoWithEdits) {

        String orgaId = metaDtoWithEdits.getOrgaId().replace("Participant:", "");

        OrganizationMetadata dbMetadata = metadataRepository.findByOrgaId(orgaId).orElse(null);

        if (dbMetadata == null) {
            return null;
        }

        metadataMapper.updateOrganizationMetadataWithMerlotParticipantMetaDto(metaDtoWithEdits, dbMetadata);

        return metadataMapper.organizationMetadataToMerlotParticipantMetaDto(metadataRepository.save(dbMetadata));
    }

    public List<MerlotParticipantMetaDto> getParticipantsByMembershipClass(MembershipClass membershipClass){
        List<OrganizationMetadata> orgaMetadataList = metadataRepository.findByMembershipClass(membershipClass);
        return orgaMetadataList.stream().map(orgaMetadata -> metadataMapper.organizationMetadataToMerlotParticipantMetaDto(orgaMetadata)).toList();
    }

}