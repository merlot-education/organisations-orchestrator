package eu.merloteducation.organisationsorchestrator.controller;

import eu.merloteducation.organisationsorchestrator.service.GXFSCatalogRestService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/organizations")
public class OrganizationQueryController {

    @Autowired
    private GXFSCatalogRestService gxfsCatalogRestService;


    @GetMapping("/getAll")
    public List<String> getAllOrganizations(Principal principal,
                                              HttpServletResponse response) {
        return new ArrayList<>();
    }

    @GetMapping("/get/{orgaId}")
    public String getOrganizationById(Principal principal,
                                            @PathVariable(value="orgaId") String orgaId,
                                            HttpServletResponse response) {
        return (String) gxfsCatalogRestService.getParticipants();
    }


}

