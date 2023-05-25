package eu.merloteducation.organisationsorchestrator.controller;

import eu.merloteducation.organisationsorchestrator.models.OrganizationModel;
import eu.merloteducation.organisationsorchestrator.models.ParticipantItem;
import eu.merloteducation.organisationsorchestrator.service.GXFSCatalogRestService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@CrossOrigin
@RequestMapping("/")
public class OrganizationQueryController {

    @Autowired
    private GXFSCatalogRestService gxfsCatalogRestService;

    @GetMapping("health")
    public void getHealth() {
        // always return code 200
    }


    @GetMapping("")
    public List<OrganizationModel> getAllOrganizations(Principal principal,
                                                       HttpServletResponse response) throws Exception {
        return gxfsCatalogRestService.getParticipants();
    }

    @GetMapping("/organization/{orgaId}")
    public OrganizationModel getOrganizationById(Principal principal,
                                            @PathVariable(value="orgaId") String orgaId,
                                            HttpServletResponse response) throws Exception {
        try {
            return gxfsCatalogRestService.getParticipantById(orgaId);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(NOT_FOUND, "No participant with this id was found.");
        }

    }


}

