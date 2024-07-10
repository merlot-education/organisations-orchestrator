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
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.gxfscataloglibrary.models.credentials.CastableCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.credentials.ExtendedVerifiableCredential;
import eu.merloteducation.gxfscataloglibrary.models.credentials.ExtendedVerifiablePresentation;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.PojoCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.datatypes.GxVcard;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.datatypes.NodeKindIRITypeId;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.participants.GxLegalParticipantCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.gx.participants.GxLegalRegistrationNumberCredentialSubject;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.datatypes.ParticipantTermsAndConditions;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotLegalParticipantCredentialSubject;
import eu.merloteducation.modelslib.api.organization.MembershipClass;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantMetaDto;
import eu.merloteducation.organisationsorchestrator.config.InitialDataLoader;
import eu.merloteducation.organisationsorchestrator.controller.OrganizationQueryController;
import eu.merloteducation.organisationsorchestrator.service.ParticipantService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.io.File;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@EnableConfigurationProperties
class InitialDataLoaderTests {

    @MockBean
    private InitialDataLoader initialDataLoader;  // mock the spring-created data loader as we create it manually

    @MockBean
    private OrganizationQueryController organizationQueryController;

    @MockBean
    private ParticipantService participantService;

    @Value("${init-data.organisations:#{null}}")
    private File initialOrgasResource;

    @Value("${init-data.connectors:#{null}}")
    private File initialOrgaConnectorsResource;

    private ExtendedVerifiableCredential createExtendedVerifiableCredentialFromPojoCs(PojoCredentialSubject cs) throws JsonProcessingException {
        VerifiableCredential vc = VerifiableCredential
                .builder()
                .id(URI.create(cs.getId() + "#" + cs.getType()))
                .issuanceDate(Date.from(Instant.now()))
                .credentialSubject(CastableCredentialSubject.fromPojo(cs))
                .issuer(URI.create("did:web:someissuer"))
                .build();
        return ExtendedVerifiableCredential.fromMap(vc.getJsonObject());
    }


    @Test
    void noParticipantsExist() throws Exception {
        when(organizationQueryController.getAllOrganizations(anyInt(), anyInt(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList(), Pageable.ofSize(1), 0));
        MerlotParticipantDto dto = new MerlotParticipantDto();

        ExtendedVerifiablePresentation vp = new ExtendedVerifiablePresentation();
        GxVcard address = new GxVcard();
        address.setCountrySubdivisionCode("DE-BE");
        address.setCountryCode("DE");
        address.setLocality("Berlin");
        address.setStreetAddress("Some Street 3");
        address.setPostalCode("12345");
        GxLegalParticipantCredentialSubject gxParticipantCs = new GxLegalParticipantCredentialSubject();
        gxParticipantCs.setName("MERLOT Federation");
        gxParticipantCs.setId("did:web:example.com:participant:someid");
        gxParticipantCs.setLegalAddress(address);
        gxParticipantCs.setHeadquarterAddress(address);
        gxParticipantCs.setLegalRegistrationNumber(List.of(new NodeKindIRITypeId("did:web:example.com:participant:someid-regId")));
        GxLegalRegistrationNumberCredentialSubject gxRegistrationNumberCs = new GxLegalRegistrationNumberCredentialSubject();
        gxRegistrationNumberCs.setId("did:web:example.com:participant:someid-regId");
        gxRegistrationNumberCs.setLeiCode("894500MQZ65CN32S9A66");
        MerlotLegalParticipantCredentialSubject merlotParticipantCs = new MerlotLegalParticipantCredentialSubject();
        merlotParticipantCs.setLegalName("MERLOT Federation");
        merlotParticipantCs.setLegalForm("LLC");
        ParticipantTermsAndConditions tnc = new ParticipantTermsAndConditions();
        tnc.setUrl("http://example.com");
        tnc.setHash("1234");
        merlotParticipantCs.setTermsAndConditions(tnc);
        gxParticipantCs.setId("did:web:example.com:participant:someid");

        vp.setVerifiableCredentials(List.of(
                createExtendedVerifiableCredentialFromPojoCs(gxParticipantCs),
                createExtendedVerifiableCredentialFromPojoCs(gxRegistrationNumberCs),
                createExtendedVerifiableCredentialFromPojoCs(merlotParticipantCs)
        ));

        dto.setSelfDescription(vp);
        dto.setMetadata(new MerlotParticipantMetaDto());
        dto.getMetadata().setMembershipClass(MembershipClass.PARTICIPANT);
        when(organizationQueryController.createOrganization(any(), any()))
                .thenReturn(dto);
        when(organizationQueryController.updateOrganization(any(), any()))
                .thenReturn(dto);
        when(participantService.updateParticipant(any(), any()))
                .thenReturn(dto);
        InitialDataLoader dataLoader = new InitialDataLoader(
                organizationQueryController,
                participantService,
                initialOrgasResource,
                0,
                "did:web:example.com:participant:123");
        dataLoader.run();

        // create MERLOT fed, create example, create example 2
        verify(organizationQueryController, times(3)).createOrganization(any(), any());
        // update example for adding connectors, update again for federator role
        // update example2 for adding connectors, update again for MERLOT signature
        verify(organizationQueryController, times(2)).updateOrganization(any(), any());
    }

    @Test
    void participantsAlreadyExist() throws Exception {
        MerlotParticipantDto dto = new MerlotParticipantDto();
        when(organizationQueryController.getAllOrganizations(anyInt(), anyInt(), any()))
                .thenReturn(new PageImpl<>(List.of(dto), Pageable.ofSize(1), 1));
        InitialDataLoader dataLoader = new InitialDataLoader(
                organizationQueryController,
                participantService,
                initialOrgasResource,
                0,
                "did:web:example.com:participant:123");
        dataLoader.run();
        verify(organizationQueryController, never()).createOrganization(any(), any());
        verify(organizationQueryController, never()).updateOrganization(any(), any());
    }

}
