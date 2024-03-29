package eu.merloteducation.organisationsorchestrator.service;

import eu.merloteducation.modelslib.api.did.ParticipantDidPrivateKeyCreateRequest;
import eu.merloteducation.modelslib.api.did.ParticipantDidPrivateKeyDto;
import eu.merloteducation.organisationsorchestrator.config.MessageQueueConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

@Service
public class OutgoingMessageService {

    @Autowired
    RabbitTemplate rabbitTemplate;

    private final Logger logger = LoggerFactory.getLogger(OutgoingMessageService.class);

    /**
     * Send an organization membership revoked message to the message bus.
     *
     * @param orgaId id of the organization whose membership has been revoked
     */
    public void sendOrganizationMembershipRevokedMessage(String orgaId) {
        logger.info("Sending organization membership revocation message for organization with id {}", orgaId);

        rabbitTemplate.convertAndSend(
            MessageQueueConfig.ORCHESTRATOR_EXCHANGE,
            MessageQueueConfig.ORGANIZATION_REVOKED_KEY,
            orgaId
        );
    }

    public ParticipantDidPrivateKeyDto requestNewDidPrivateKey(ParticipantDidPrivateKeyCreateRequest request) {
        logger.info("Requesting a new DID and private key for subject {}", request.getSubject());

        return rabbitTemplate.convertSendAndReceiveAsType(
                MessageQueueConfig.DID_SERVICE_EXCHANGE,
                MessageQueueConfig.DID_PRIVATE_KEY_REQUEST_KEY,
                request,
                new ParameterizedTypeReference<>() {
                }
        );
    }
}
