package eu.merloteducation.organisationsorchestrator.controller;

import com.fasterxml.jackson.annotation.JsonView;
import eu.merloteducation.organisationsorchestrator.models.OrganiationViews;
import eu.merloteducation.organisationsorchestrator.models.OrganizationModel;
import eu.merloteducation.organisationsorchestrator.service.GXFSCatalogRestService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/organization/{orgaId}/Connectors/")
public class OrganisationConnectorsController {

    @Autowired
    //private OrganisationConnectorsService connectorsService;

    @GetMapping("")
    @JsonView(OrganiationViews.PublicView.class)
    public List<OrganizationModel> getAllConnectors(Principal principal,
                                                       @PathVariable(value="orgaId") String orgaId,
                                                       HttpServletResponse response) throws Exception {
        return null;
    }

    @GetMapping("Connector/{connectorId}")
    @JsonView(OrganiationViews.PublicView.class)
    public List<OrganizationModel> getConnector(Principal principal,
                                                 @PathVariable(value="orgaId") String orgaId,
                                                 HttpServletResponse response) throws Exception {
        return null;
    }

    @PostMapping("Connector")
    @JsonView(OrganiationViews.PublicView.class)
    public List<OrganizationModel> postConnector(Principal principal,
                                                 @PathVariable(value="orgaId") String orgaId,
                                                 HttpServletResponse response) throws Exception {
        return null;
    }

    @PatchMapping("Connector/{connectorId}")
    @JsonView(OrganiationViews.PublicView.class)
    public List<OrganizationModel> updateConnector(Principal principal,
                                                  @PathVariable(value="orgaId") String orgaId,
                                                  HttpServletResponse response) throws Exception {
        return null;
    }

    @DeleteMapping("Connector/{connectorId}")
    @JsonView(OrganiationViews.PublicView.class)
    public List<OrganizationModel> deleteConnector(Principal principal,
                                                  @PathVariable(value="orgaId") String orgaId,
                                                  @PathVariable(value="connectorId") String connectorId,
                                                  HttpServletResponse response) throws Exception {
        return null;
    }
}
