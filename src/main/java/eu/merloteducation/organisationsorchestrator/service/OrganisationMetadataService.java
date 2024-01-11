package eu.merloteducation.organisationsorchestrator.service;

import eu.merloteducation.modelslib.api.organization.MerlotParticipantMetaDto;
import eu.merloteducation.organisationsorchestrator.mappers.OrganizationMetadataMapper;
import eu.merloteducation.organisationsorchestrator.models.entities.MembershipClass;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganizationMetadata;
import eu.merloteducation.organisationsorchestrator.repositories.OrganizationMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrganisationMetadataService {
    @Autowired
    private OrganizationMetadataRepository metadataRepository;

    @Autowired
    private OrganizationMetadataMapper metadataMapper;

    public MerlotParticipantMetaDto getMerlotParticipantMetaDto(String merlotId) {

        merlotId = merlotId.replace("Participant:", "");

        OrganizationMetadata dbMeta = metadataRepository.findByMerlotId(merlotId).orElse(null);

        return metadataMapper.organizationMetadataToMerlotParticipantMetaDto(dbMeta);
    }

    public MerlotParticipantMetaDto saveMerlotParticipantMeta(OrganizationMetadata meta) {

        return metadataMapper.organizationMetadataToMerlotParticipantMetaDto(metadataRepository.save(meta));
    }

    public MerlotParticipantMetaDto updateMerlotParticipantMeta(MerlotParticipantMetaDto metaDtoWithEdits) {

        String merlotId = metaDtoWithEdits.getOrgaId().replace("Participant:", "");

        OrganizationMetadata dbMetadata = metadataRepository.findByMerlotId(merlotId).orElse(null);

        if (dbMetadata == null) {
            return null;
        }

        metadataMapper.updateOrganizationMetadataWithMerlotParticipantMetaDto(metaDtoWithEdits, dbMetadata);

        return metadataMapper.organizationMetadataToMerlotParticipantMetaDto(metadataRepository.save(dbMetadata));
    }

}
