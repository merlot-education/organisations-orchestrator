package eu.merloteducation.organisationsorchestrator.auth;

import eu.merloteducation.authorizationlibrary.authorization.OrganizationRoleGrantedAuthority;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.participants.GxLegalParticipantCredentialSubject;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import org.springframework.stereotype.Component;

@Component("participantAuthorityChecker")
public class ParticipantAuthorityChecker {

    public boolean representsParticipantFromDto(OrganizationRoleGrantedAuthority activeRole, MerlotParticipantDto dto) {
        return activeRole.isRepresentative() && roleIdEqualsDtoId(activeRole, dto);
    }

    public boolean isFedAdminForDifferentParticipant(OrganizationRoleGrantedAuthority activeRole, MerlotParticipantDto dto) {
        return activeRole.isFedAdmin() && !roleIdEqualsDtoId(activeRole, dto);
    }

    private boolean roleIdEqualsDtoId(OrganizationRoleGrantedAuthority activeRole, MerlotParticipantDto dto) {
        return activeRole.getOrganizationId()
                .equals(dto.getSelfDescription()
                        .findFirstCredentialSubjectByType(GxLegalParticipantCredentialSubject.class).getId())
                && activeRole.getOrganizationId().equals(dto.getId());
    }
}
