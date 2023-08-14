package eu.merloteducation.organisationsorchestrator.controller;

import com.fasterxml.jackson.annotation.JsonView;
import eu.merloteducation.organisationsorchestrator.models.OrganiationViews;
import eu.merloteducation.organisationsorchestrator.models.OrganizationModel;
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
@RequestMapping("/")
public class OrganizationQueryController {

    @Autowired
    private GXFSCatalogRestService gxfsCatalogRestService;

    /**
     * GET health endpoint.
     */
    @GetMapping("health")
    public void getHealth() {
        // always return code 200
    }


    /**
     * GET endpoint for retrieving all enrolled organizations.
     *
     * @return list of all enrolled organizations
     * @throws Exception exception during participant retrieval
     */
    @GetMapping("")
    @JsonView(OrganiationViews.PublicView.class)
    public List<OrganizationModel> getAllOrganizations() throws Exception {
        return gxfsCatalogRestService.getParticipants();
    }

    /**
     * GET endpoint for retrieving a specific organization by its id.
     *
     * @param orgaId organization id
     * @return organization data
     * @throws Exception exception during participant retrieval
     */
    @GetMapping("/organization/{orgaId}")
    @JsonView(OrganiationViews.PublicView.class)
    public OrganizationModel getOrganizationById(@PathVariable(value = "orgaId") String orgaId) throws Exception {
        try {
            return gxfsCatalogRestService.getParticipantById(orgaId);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(NOT_FOUND, "No participant with this id was found.");
        }

    }


}

