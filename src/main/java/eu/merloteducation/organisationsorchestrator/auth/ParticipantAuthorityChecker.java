/*
 *  Copyright 2023-2024 Dataport AöR
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

    public boolean isFedAdminOfDifferentParticipant(OrganizationRoleGrantedAuthority activeRole, MerlotParticipantDto dto) {
        return activeRole.isFedAdmin() && !roleIdEqualsDtoId(activeRole, dto);
    }

    private boolean roleIdEqualsDtoId(OrganizationRoleGrantedAuthority activeRole, MerlotParticipantDto dto) {
        return activeRole.getOrganizationId()
                .equals(dto.getSelfDescription()
                        .findFirstCredentialSubjectByType(GxLegalParticipantCredentialSubject.class).getId())
                && activeRole.getOrganizationId().equals(dto.getId());
    }
}
