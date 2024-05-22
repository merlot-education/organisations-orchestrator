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
    @GetMapping("/merlot/participant")
    public String getMerlotParticipantShape() {
        return gxfsWizardApiService.getShapeByName("merlot", "Merlotlegalparticipant.json");
    }

    @GetMapping("/gx/participant")
    public String getGxParticipantShape() {
        return gxfsWizardApiService.getShapeByName("gx", "Legalparticipant.json");
    }

    @GetMapping("/gx/registrationnumber")
    public String getGxRegistrationNumberShape() {
        return gxfsWizardApiService.getShapeByName("gx", "Legalregistrationnumber.json");
    }
}
