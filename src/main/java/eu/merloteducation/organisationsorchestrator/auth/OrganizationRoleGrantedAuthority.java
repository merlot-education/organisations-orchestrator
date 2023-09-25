package eu.merloteducation.organisationsorchestrator.auth;

import com.google.common.base.Joiner;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

@Getter
public class OrganizationRoleGrantedAuthority implements GrantedAuthority {

    private final String organizationRole;
    private final String organizationId;

    public OrganizationRoleGrantedAuthority(String authorityString) {
        String[] roleSplit = authorityString.split("_");
        if (roleSplit[0].equals("OrgRep") || roleSplit[0].equals("OrgLegRep")) {
            this.organizationRole = roleSplit[0];
            this.organizationId = authorityString.replace(this.organizationRole + "_", "");
        } else {
            throw new IllegalArgumentException("Unknown organization role authority " + authorityString);
        }
    }

    @Override
    public String getAuthority() {
        return "ROLE_" + organizationRole + "_" + organizationId;
    }
}
