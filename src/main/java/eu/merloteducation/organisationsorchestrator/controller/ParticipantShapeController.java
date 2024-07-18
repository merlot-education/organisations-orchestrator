/*
 *  Copyright 2024 Dataport. All rights reserved. Developed as part of the MERLOT project.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.merloteducation.organisationsorchestrator.controller;

import com.fasterxml.jackson.databind.JsonNode;
import eu.merloteducation.gxfscataloglibrary.service.GxdchService;
import eu.merloteducation.gxfscataloglibrary.service.GxfsWizardApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shapes")
public class ParticipantShapeController {

    private static final String ECOSYSTEM_MERLOT = "merlot";
    private static final String ECOSYSTEM_GAIAX = "gx";

    private final GxfsWizardApiService gxfsWizardApiService;

    private final GxdchService gxdchService;

    public ParticipantShapeController(@Autowired GxfsWizardApiService gxfsWizardApiService,
                                      @Autowired GxdchService gxdchService) {
        this.gxfsWizardApiService = gxfsWizardApiService;
        this.gxdchService = gxdchService;
    }

    /**
     * GET request for retrieving the merlot participant shape for the catalog.
     *
     * @return merlot participant shape
     */
    @GetMapping("/merlot/participant")
    public String getMerlotParticipantShape() {
        return gxfsWizardApiService.getShapeByName(ECOSYSTEM_MERLOT, "Merlotlegalparticipant.json");
    }

    @GetMapping("/gx/participant")
    public String getGxParticipantShape() {
        return gxfsWizardApiService.getShapeByName(ECOSYSTEM_GAIAX, "Legalparticipant.json");
    }

    @GetMapping("/gx/registrationnumber")
    public String getGxRegistrationNumberShape() {
        return gxfsWizardApiService.getShapeByName(ECOSYSTEM_GAIAX, "Legalregistrationnumber.json");
    }

    @GetMapping("/gx/tnc")
    public JsonNode getGxTermsAndConditions() {
        return gxdchService.getGxTnCs();
    }
}
