package eu.merloteducation.organisationsorchestrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.merloteducation.gxfscataloglibrary.models.exception.CredentialPresentationException;
import eu.merloteducation.gxfscataloglibrary.models.exception.CredentialSignatureException;
import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.organisationsorchestrator.config.InitialDataLoader;
import eu.merloteducation.organisationsorchestrator.service.ParticipantConnectorsService;
import eu.merloteducation.organisationsorchestrator.service.ParticipantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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

    @MockBean
    private ParticipantConnectorsService participantConnectorsService;

    @Value("classpath:initial-orgas.json")
    Resource initialOrgasResource;

    @Value("classpath:initial-orga-connectors.json")
    private Resource initialOrgaConnectorsResource;


    @Test
    void noParticipantsExist() throws IOException, CredentialSignatureException, CredentialPresentationException {
        when(participantService.getParticipants(any()))
                .thenReturn(new PageImpl<>(Collections.emptyList(), Pageable.ofSize(1), 0));
        MerlotParticipantDto dto = new MerlotParticipantDto();
        dto.setId("did:web:example.com#someid");
        when(participantService.createParticipant(any()))
                .thenReturn(dto);
        InitialDataLoader dataLoader = new InitialDataLoader(
                participantService,
                participantConnectorsService,
                new ObjectMapper(),
                initialOrgasResource,
                initialOrgaConnectorsResource,
                "1234",
                "5678");
        dataLoader.run();

        verify(participantService, times(1)).createParticipant(any());
        verify(participantConnectorsService, times(2)).postConnector(any(), any());
    }

    @Test
    void participantsAlreadyExist() throws IOException, CredentialSignatureException, CredentialPresentationException {
        MerlotParticipantDto dto = new MerlotParticipantDto();
        when(participantService.getParticipants(any()))
                .thenReturn(new PageImpl<>(List.of(dto), Pageable.ofSize(1), 1));
        InitialDataLoader dataLoader = new InitialDataLoader(
                participantService,
                participantConnectorsService,
                new ObjectMapper(),
                initialOrgasResource,
                initialOrgaConnectorsResource,
                "1234",
                "5678");
        dataLoader.run();
        verify(participantService, never()).createParticipant(any());
        verify(participantConnectorsService, never()).postConnector(any(), any());
    }

}
