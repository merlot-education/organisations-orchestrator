package eu.merloteducation.organisationsorchestrator.controller;

import com.fasterxml.jackson.annotation.JsonView;
import eu.merloteducation.organisationsorchestrator.models.OrganiationViews;
import eu.merloteducation.organisationsorchestrator.models.OrganizationModel;
import eu.merloteducation.organisationsorchestrator.service.GXFSCatalogRestService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/")
public class OrganizationQueryController {

    @Autowired
    private GXFSCatalogRestService gxfsCatalogRestService;

    @GetMapping("health")
    public void getHealth() {
        // always return code 200
    }


    @GetMapping("")
    @JsonView(OrganiationViews.PublicView.class)
    public List<OrganizationModel> getAllOrganizations(@RequestParam(value = "page", defaultValue = "0") int page,
                                                       @RequestParam(value = "size", defaultValue = "9") int size,
                                                       Principal principal,
                                                       HttpServletResponse response) throws Exception {
        return gxfsCatalogRestService.getParticipants(PageRequest.of(page, size));
    }

    @GetMapping("/organization/{orgaId}")
    @JsonView(OrganiationViews.PublicView.class)
    public OrganizationModel getOrganizationById(Principal principal,
                                                 @PathVariable(value = "orgaId") String orgaId,
                                                 HttpServletResponse response) throws Exception {
        try {
            return gxfsCatalogRestService.getParticipantById(orgaId);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(NOT_FOUND, "No participant with this id was found.");
        }

    }


}

