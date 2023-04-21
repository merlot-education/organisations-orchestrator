package eu.merloteducation.organisationsorchestrator.controller;

import eu.merloteducation.organisationsorchestrator.models.OrganizationModel;
import eu.merloteducation.organisationsorchestrator.models.ParticipantItem;
import eu.merloteducation.organisationsorchestrator.service.GXFSCatalogRestService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/organizations")
public class OrganizationQueryController {

    @Autowired
    private GXFSCatalogRestService gxfsCatalogRestService;


    @GetMapping("")
    public List<OrganizationModel> getAllOrganizations(Principal principal,
                                                       HttpServletResponse response) throws Exception {
        return gxfsCatalogRestService.getParticipants();
    }

    @GetMapping("/organization/{orgaId}")
    public List<OrganizationModel> getOrganizationById(Principal principal,
                                            @PathVariable(value="orgaId") String orgaId,
                                            HttpServletResponse response) throws Exception {
        return gxfsCatalogRestService.getParticipants();
    }


}

