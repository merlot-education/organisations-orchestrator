package eu.merloteducation.organisationsorchestrator.service;

import eu.merloteducation.organisationsorchestrator.models.OrganizationModel;
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

    private final Logger logger = LoggerFactory.getLogger(MessageQueueService.class);

    @RabbitListener(queues = MessageQueueConfig.ORGANIZATION_REQUEST_QUEUE)
    public OrganizationModel organizationRequest(String orgaId) throws Exception {
        logger.info("Organization request message: {}", orgaId);
        try {
            return gxfsCatalogRestService.getParticipantById(orgaId);
        } catch (Exception e) {
            logger.error("Failed to find participant with this id, error: {}", e.getMessage());
            return null;
        }
    }
}
