package eu.merloteducation.organisationsorchestrator.service;

import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.api.organization.OrganizationConnectorDto;
import eu.merloteducation.modelslib.queue.ConnectorDetailsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import eu.merloteducation.organisationsorchestrator.config.MessageQueueConfig;

@Service
public class MessageQueueService {
    @Autowired
    ParticipantService participantService;

    @Autowired
    ParticipantConnectorsService participantConnectorsService;

    private final Logger logger = LoggerFactory.getLogger(MessageQueueService.class);

    /**
     * Listen to requests of organization details on the message bus and return the organization.
     *
     * @param orgaId id of the organization to request details for
     * @return organization details
     */
    @RabbitListener(queues = MessageQueueConfig.ORGANIZATION_REQUEST_QUEUE)
    public MerlotParticipantDto organizationRequest(String orgaId) throws Exception {
        logger.info("Organization request message: {}", orgaId);
        try {
            return participantService.getParticipantById(orgaId);
        } catch (Exception e) {
            logger.error("Failed to find participant with this id, error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Listen to requests of organization connector details on the message bus and return the organization connector.
     *
     * @param connectorDetailsRequest request of organization connector details
     * @return connector details
     */
    @RabbitListener(queues = MessageQueueConfig.ORGANIZATIONCONNECTOR_REQUEST_QUEUE)
    public OrganizationConnectorDto organizationConnectorRequest(ConnectorDetailsRequest connectorDetailsRequest) {
        logger.info("Organization Connector request message: {}", connectorDetailsRequest.getOrgaId());
        try {
            return participantConnectorsService.getConnector(connectorDetailsRequest.getOrgaId(), connectorDetailsRequest.getConnectorId());
        } catch (Exception e) {
            logger.error("Failed to find participant with this id, error: {}", e.getMessage());
            return null;
        }
    }
}
