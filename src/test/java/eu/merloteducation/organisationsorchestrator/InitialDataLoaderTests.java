package eu.merloteducation.organisationsorchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescription;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.SelfDescriptionVerifiableCredential;
import eu.merloteducation.gxfscataloglibrary.models.selfdescriptions.merlot.participants.MerlotOrganizationCredentialSubject;
import eu.merloteducation.modelslib.api.organization.MembershipClass;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantMetaDto;
import eu.merloteducation.organisationsorchestrator.config.InitialDataLoader;
import eu.merloteducation.organisationsorchestrator.controller.OrganizationQueryController;
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
import java.util.Collections;
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

    @Value("${init-data.organisations:#{null}}")
    private File initialOrgasResource;

    @Value("${init-data.connectors:#{null}}")
    private File initialOrgaConnectorsResource;


    @Test
    void noParticipantsExist() throws Exception {
        when(organizationQueryController.getAllOrganizations(anyInt(), anyInt(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList(), Pageable.ofSize(1), 0));
        MerlotParticipantDto dto = new MerlotParticipantDto();
        dto.setSelfDescription(new SelfDescription());
        dto.getSelfDescription().setVerifiableCredential(new SelfDescriptionVerifiableCredential());
        MerlotOrganizationCredentialSubject credentialSubject = new MerlotOrganizationCredentialSubject();
        credentialSubject.setLegalName("MERLOT Federation");
        dto.getSelfDescription().getVerifiableCredential().setCredentialSubject(credentialSubject);
        dto.setId("did:web:example.com:participant:someid");
        dto.setMetadata(new MerlotParticipantMetaDto());
        dto.getMetadata().setMembershipClass(MembershipClass.PARTICIPANT);
        when(organizationQueryController.createOrganization(any(), any()))
                .thenReturn(dto);
        when(organizationQueryController.updateOrganization(any(), any()))
                .thenReturn(dto);
        InitialDataLoader dataLoader = new InitialDataLoader(
                organizationQueryController,
                new ObjectMapper(),
                initialOrgasResource,
                initialOrgaConnectorsResource,
                "example.com");
        dataLoader.run();

        // create MERLOT fed, create example
        verify(organizationQueryController, times(2)).createOrganization(any(), any());
        // update example for adding connectors, update again for federator role
        verify(organizationQueryController, times(2)).updateOrganization(any(), any());
    }

    @Test
    void participantsAlreadyExist() throws Exception {
        MerlotParticipantDto dto = new MerlotParticipantDto();
        when(organizationQueryController.getAllOrganizations(anyInt(), anyInt(), any()))
                .thenReturn(new PageImpl<>(List.of(dto), Pageable.ofSize(1), 1));
        InitialDataLoader dataLoader = new InitialDataLoader(
                organizationQueryController,
                new ObjectMapper(),
                initialOrgasResource,
                initialOrgaConnectorsResource,
                "example.com");
        dataLoader.run();
        verify(organizationQueryController, never()).createOrganization(any(), any());
        verify(organizationQueryController, never()).updateOrganization(any(), any());
    }

}
