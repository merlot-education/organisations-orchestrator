package eu.merloteducation.organisationsorchestrator.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component("authorityChecker")
public class AuthorityChecker {
    public boolean representsOrganization(Authentication authentication, String orgaId) {
        String numOrgaId = orgaId.replace("Participant:", "");
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority instanceof OrganizationRoleGrantedAuthority orgaRoleAuthority
                    && (orgaRoleAuthority.getOrganizationId().equals(numOrgaId))) {
                    return true;
            }
        }
        return false;
    }
}
