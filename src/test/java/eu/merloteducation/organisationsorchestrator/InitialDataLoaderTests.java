package eu.merloteducation.organisationsorchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.modelslib.api.organization.MembershipClass;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantMetaDto;
import eu.merloteducation.organisationsorchestrator.config.InitialDataLoader;
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
import java.io.IOException;
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
    private ParticipantService participantService;

    @Value("${init-data.organisations:#{null}}")
    private File initialOrgasResource;

    @Value("${init-data.connectors:#{null}}")
    private File initialOrgaConnectorsResource;


    @Test
    void noParticipantsExist() throws IOException {
        when(participantService.getParticipants(any(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList(), Pageable.ofSize(1), 0));
        MerlotParticipantDto dto = new MerlotParticipantDto();
        dto.setId("did:web:example.com#someid");
        dto.setMetadata(new MerlotParticipantMetaDto());
        dto.getMetadata().setMembershipClass(MembershipClass.PARTICIPANT);
        when(participantService.createParticipant(any()))
                .thenReturn(dto);
        when(participantService.updateParticipant(any(), any()))
                .thenReturn(dto);
        InitialDataLoader dataLoader = new InitialDataLoader(
                participantService,
                new ObjectMapper(),
                initialOrgasResource,
                initialOrgaConnectorsResource,
                "1234",
                "5678",
                "example.com");
        dataLoader.run();

        verify(participantService, times(1)).createParticipant(any());
        verify(participantService, times(2)).updateParticipant(any(), any());
    }

    @Test
    void participantsAlreadyExist() throws IOException {
        MerlotParticipantDto dto = new MerlotParticipantDto();
        when(participantService.getParticipants(any(), any()))
                .thenReturn(new PageImpl<>(List.of(dto), Pageable.ofSize(1), 1));
        InitialDataLoader dataLoader = new InitialDataLoader(
                participantService,
                new ObjectMapper(),
                initialOrgasResource,
                initialOrgaConnectorsResource,
                "1234",
                "5678",
                "example.com");
        dataLoader.run();
        verify(participantService, never()).createParticipant(any());
        verify(participantService, never()).updateParticipant(any(), any());
    }

}
