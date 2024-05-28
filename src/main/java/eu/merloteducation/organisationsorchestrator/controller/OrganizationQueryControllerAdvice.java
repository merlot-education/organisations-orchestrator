package eu.merloteducation.organisationsorchestrator.controller;

import eu.merloteducation.authorizationlibrary.authorization.AuthorityChecker;
import eu.merloteducation.authorizationlibrary.authorization.OrganizationRoleGrantedAuthority;
import eu.merloteducation.gxfscataloglibrary.models.query.GXFSQueryLegalNameItem;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.GXFSCatalogListResponse;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.participants.LegalParticipantCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotLegalParticipantCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.service.GxfsCatalogService;
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

    private final AuthorityChecker authorityChecker;

    private final GxfsCatalogService gxfsCatalogService;

    public OrganizationQueryControllerAdvice(@Autowired AuthorityChecker authorityChecker,
                                             @Autowired GxfsCatalogService gxfsCatalogService) {
        this.authorityChecker = authorityChecker;
        this.gxfsCatalogService = gxfsCatalogService;
    }

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
            boolean representsOrganization = authorityChecker.representsOrganization(authentication, participantDto.getId());
            if (!isFedAdmin && !representsOrganization) {
                // hide email address if we are not a federator admin and also not representing
                participantDto.getMetadata().setMailAddress(null);
            }

            if (!representsOrganization) {
                // hide connector data and signer config and certificates if we are not representing
                participantDto.getMetadata().setConnectors(null);
                participantDto.getMetadata().setOrganisationSignerConfigDto(null);
                participantDto.getMetadata().setDapsCertificates(null);
            }

            // try to also set the signedBy field
            setSignerLegalNameFromCatalog(participantDto);
            return;
        }

        try {
            Page<MerlotParticipantDto> participantDtos = (Page<MerlotParticipantDto>) bodyContainer.getValue();
            for (MerlotParticipantDto p : participantDtos) {
                boolean representsOrganization = authorityChecker.representsOrganization(authentication, p.getId());
                if (!isFedAdmin && !representsOrganization &&
                        !p.getMetadata().getMembershipClass().equals(MembershipClass.FEDERATOR)) {
                    // hide email address if we are not a federator admin and also not representing, unless it's a federator
                    p.getMetadata().setMailAddress(null);
                }

                if (!representsOrganization) {
                    // hide connector data if we are not representing
                    p.getMetadata().setConnectors(null);
                }

                // try to also set the signedBy field
                setSignerLegalNameFromCatalog(p);

                // always hide signer config and certificates in page/list view
                p.getMetadata().setOrganisationSignerConfigDto(null);
                p.getMetadata().setDapsCertificates(null);
            }
        } catch (ClassCastException ignored) {
            // if it's the wrong class, we don't want to modify it anyway
        }
    }

    private void setSignerLegalNameFromCatalog(MerlotParticipantDto dto) {
        try {
            String proofVerificationMethod = dto.getSelfDescription().getLdProof().getVerificationMethod().toString();

            String signerId = proofVerificationMethod.replaceFirst("#.*", "");

            GXFSCatalogListResponse<GXFSQueryLegalNameItem>
                    response = gxfsCatalogService.getParticipantLegalNameByUri(MerlotLegalParticipantCredentialSubject.getTypeNoPrefix(), signerId);


            // if we do not get exactly one item, we did not find the signer participant and the corresponding legal name
            if (response.getTotalCount() == 1) {
                dto.getMetadata().setSignedBy(response.getItems().get(0).getLegalName());
            }
        } catch (Exception ignored) {
            // if something fails, we just leave the signedBy as null (not resolvable)
        }
    }
}
