package eu.merloteducation.organisationsorchestrator.service;

import eu.merloteducation.organisationsorchestrator.models.messagequeue.ConnectorDetailsRequest;
import eu.merloteducation.organisationsorchestrator.models.dto.MerlotParticipantDto;
import eu.merloteducation.organisationsorchestrator.models.entities.OrganisationConnectorExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import eu.merloteducation.organisationsorchestrator.config.MessageQueueConfig;

@Service
public class MessageQueueService {
    @Autowired
    GXFSCatalogRestService gxfsCatalogRestService;

    @Autowired
    OrganisationConnectorsService organisationConnectorsService;

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
            return gxfsCatalogRestService.getParticipantById(orgaId);
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
    public OrganisationConnectorExtension organizationConnectorRequest(ConnectorDetailsRequest connectorDetailsRequest) {
        logger.info("Organization Connector request message: {}", connectorDetailsRequest.getOrgaId());
        try {
            return organisationConnectorsService.getConnector(connectorDetailsRequest.getOrgaId(), connectorDetailsRequest.getConnectorId());
        } catch (Exception e) {
            logger.error("Failed to find participant with this id, error: {}", e.getMessage());
            return null;
        }
    }
}
