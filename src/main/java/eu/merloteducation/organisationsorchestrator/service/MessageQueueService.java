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

package eu.merloteducation.organisationsorchestrator.service;

import eu.merloteducation.modelslib.api.organization.MerlotParticipantDto;
import eu.merloteducation.modelslib.api.organization.OrganizationConnectorTransferDto;
import eu.merloteducation.modelslib.queue.ConnectorDetailsRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import eu.merloteducation.organisationsorchestrator.config.MessageQueueConfig;

@Service
@Slf4j
public class MessageQueueService {
    private final ParticipantService participantService;
    private final OrganizationMetadataService organizationMetadataService;

    public MessageQueueService(@Autowired ParticipantService participantService,
                               @Autowired OrganizationMetadataService organizationMetadataService) {
        this.participantService = participantService;
        this.organizationMetadataService = organizationMetadataService;
    }

    /**
     * Listen to requests of organization details on the message bus and return the organization.
     *
     * @param orgaId id of the organization to request details for
     * @return organization details
     */
    @RabbitListener(queues = MessageQueueConfig.ORGANIZATION_REQUEST_QUEUE)
    public MerlotParticipantDto organizationRequest(String orgaId) {

        log.info("Organization request message: {}", orgaId);
        try {
            return participantService.getParticipantById(orgaId);
        } catch (Exception e) {
            log.error("Failed to find participant with this id, error: {}", e.getMessage());
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
    public OrganizationConnectorTransferDto organizationConnectorRequest(ConnectorDetailsRequest connectorDetailsRequest) {

        log.info("Organization Connector request message: {}", connectorDetailsRequest.getOrgaId());

        OrganizationConnectorTransferDto connectorDto = organizationMetadataService.getConnectorForParticipant(
            connectorDetailsRequest.getOrgaId(), connectorDetailsRequest.getConnectorId());

        if (connectorDto == null) {
            log.error("Connector for Participant with id {} could not be found",
                connectorDetailsRequest.getOrgaId());
        }

        return connectorDto;
    }
}

