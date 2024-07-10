/*
 *  Copyright 2023-2024 Dataport AöR
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

package eu.merloteducation.organisationsorchestrator;

import com.danubetech.verifiablecredentials.VerifiableCredential;
import com.fasterxml.jackson.core.JsonProcessingException;
import eu.merloteducation.gxfscataloglibrary.models.credentials.CastableCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.credentials.ExtendedVerifiableCredential;
import eu.merloteducation.gxfscataloglibrary.models.credentials.ExtendedVerifiablePresentation;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.PojoCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.datatypes.GxDataAccountExport;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.datatypes.GxSOTermsAndConditions;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.datatypes.GxVcard;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.datatypes.NodeKindIRITypeId;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.participants.GxLegalParticipantCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.participants.GxLegalRegistrationNumberCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.serviceofferings.GxServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.AllowedUserCount;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.DataExchangeCount;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.OfferingRuntime;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.ParticipantTermsAndConditions;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotLegalParticipantCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotCoopContractServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotDataDeliveryServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotSaasServiceOfferingCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.serviceofferings.MerlotServiceOfferingCredentialSubject;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class SelfDescriptionDemoData {

    static ExtendedVerifiablePresentation createVpFromCsList(List<PojoCredentialSubject> csList, String issuer) throws JsonProcessingException {
        ExtendedVerifiablePresentation vp = new ExtendedVerifiablePresentation();
        List<ExtendedVerifiableCredential> vcList = new ArrayList<>();
        for (PojoCredentialSubject cs : csList) {
            CastableCredentialSubject ccs = CastableCredentialSubject.fromPojo(cs);
            String vcId = cs.getId();  // set vc id to cs id
            if (!vcId.contains("#")) {
                vcId += "#" + cs.getType(); // add type if not existent yet
            }
            VerifiableCredential vc = VerifiableCredential
                    .builder()
                    .id(URI.create(vcId))
                    .issuanceDate(Date.from(Instant.now()))
                    .credentialSubject(ccs)
                    .issuer(URI.create(issuer))
                    .build();
            vcList.add(ExtendedVerifiableCredential.fromMap(vc.getJsonObject()));
        }
        vp.setVerifiableCredentials(vcList);
        return vp;
    }

    static MerlotLegalParticipantCredentialSubject getMerlotParticipantCs(String id) {
        MerlotLegalParticipantCredentialSubject expected = new MerlotLegalParticipantCredentialSubject();
        expected.setId(id);
        expected.setLegalName("MyOrga");
        expected.setLegalForm("LLC");

        ParticipantTermsAndConditions termsAndConditions = new ParticipantTermsAndConditions();
        termsAndConditions.setUrl("http://example.com");
        termsAndConditions.setHash("1234");
        expected.setTermsAndConditions(termsAndConditions);

        return expected;
    }

    static GxLegalParticipantCredentialSubject getGxParticipantCs(String id) {

        GxLegalParticipantCredentialSubject expected = new GxLegalParticipantCredentialSubject();
        expected.setId(id);
        expected.setName("MyOrga");

        GxVcard vCard = new GxVcard();
        vCard.setLocality("Berlin");
        vCard.setPostalCode("12345");
        vCard.setCountryCode("DE");
        vCard.setCountrySubdivisionCode("DE-BE");
        vCard.setStreetAddress("Some Street 3");
        expected.setLegalAddress(vCard);
        expected.setHeadquarterAddress(vCard);
        expected.setLegalRegistrationNumber(List.of(new NodeKindIRITypeId(id + "-regId")));

        return expected;
    }

    static GxLegalRegistrationNumberCredentialSubject getGxRegistrationNumberCs(String id) {

        GxLegalRegistrationNumberCredentialSubject expected = new GxLegalRegistrationNumberCredentialSubject();
        expected.setId(id + "-regId");
        expected.setLeiCode("0110");

        return expected;
    }
}
