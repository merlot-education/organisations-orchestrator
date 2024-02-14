package eu.merloteducation.organisationsorchestrator.controller;

import eu.merloteducation.authorizationlibrary.authorization.AuthorityChecker;
import eu.merloteducation.authorizationlibrary.authorization.OrganizationRoleGrantedAuthority;
import eu.merloteducation.modelslib.api.organization.MembershipClass;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.AbstractMappingJacksonResponseBodyAdvice;

@ControllerAdvice(assignableTypes = OrganizationQueryController.class)
public class OrganizationQueryControllerAdvice extends AbstractMappingJacksonResponseBodyAdvice {

    @Autowired
    private AuthorityChecker authorityChecker;

    @Override
    protected void beforeBodyWriteInternal(MappingJacksonValue bodyContainer, @NotNull MediaType contentType,
                                           @NotNull MethodParameter returnType, @NotNull ServerHttpRequest req,
                                           @NotNull ServerHttpResponse res) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        ServletServerHttpRequest request = (ServletServerHttpRequest) req;
        String activeRoleString = request.getHeaders().getFirst("Active-Role");
        boolean isFedAdmin = activeRoleString != null
                && new OrganizationRoleGrantedAuthority(activeRoleString).isFedAdmin();

        if (bodyContainer.getValue() instanceof MerlotParticipantDto participantDto) {
            if (!isFedAdmin &&
                    !authorityChecker.representsOrganization(authentication, participantDto.getId())) {
                participantDto.setMetadata(null); // for single objects hide metadata if we are not representing
            }

            if (participantDto.getMetadata() != null && !authorityChecker.representsOrganization(authentication, participantDto.getId())) {
                // hide connector data if we are allowed to see the metadata but are not representing
                participantDto.getMetadata().setConnectors(null);
            }
            return;
        }

        try {
            Page<MerlotParticipantDto> participantDtos = (Page<MerlotParticipantDto>) bodyContainer.getValue();
            for (MerlotParticipantDto p : participantDtos) {
                if (!isFedAdmin &&
                        !authorityChecker.representsOrganization(authentication, p.getId()) &&
                        !p.getMetadata().getMembershipClass().equals(MembershipClass.FEDERATOR)) {
                    p.setMetadata(null); // for lists hide metadata if we are not representing, or it's a federator
                }

                if (p.getMetadata() != null && !authorityChecker.representsOrganization(authentication, p.getId())) {
                    // hide connector data if we are allowed to see the metadata but are not representing
                    p.getMetadata().setConnectors(null);
                }
            }
        } catch (ClassCastException ignored) {
            // if it's the wrong class, we don't want to modify it anyway
        }
    }
}
