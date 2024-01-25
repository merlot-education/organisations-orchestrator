package eu.merloteducation.organisationsorchestrator.controller;

import eu.merloteducation.gxfscataloglibrary.service.GxfsWizardApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shapes")
public class ParticipantShapeController {

    @Autowired
    private GxfsWizardApiService gxfsWizardApiService;

    /**
     * GET request for retrieving the merlot participant shape for the catalog.
     *
     * @return merlot participant shape
     */
    @GetMapping("/merlotParticipant")
    public String getShapeJson() {
        return gxfsWizardApiService.getShapeByName("Merlot Organization.json");
    }
}
