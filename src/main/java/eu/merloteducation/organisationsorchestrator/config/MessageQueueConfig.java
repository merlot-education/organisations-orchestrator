package eu.merloteducation.organisationsorchestrator.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageQueueConfig {

    public static final String ORCHESTRATOR_EXCHANGE = "orchestrator.exchange";
    public static final String ORGANIZATION_REQUEST_KEY = "request.organization";
    public static final String ORGANIZATIONCONNECTOR_REQUEST_KEY = "request.organizationconnector";
    public static final String ORGANIZATION_REQUEST_QUEUE = "organization.request.organization.queue";
    public static final String ORGANIZATIONCONNECTOR_REQUEST_QUEUE = "organizationconnector.request.organizationconnector.queue";
    public static final String ORGANIZATION_REVOKED_KEY = "revoked.organization";

    public static final String DID_SERVICE_EXCHANGE = "service.exchange";
    public static final String DID_PRIVATE_KEY_REQUEST_KEY = "request.did_privatekey";

    @Bean
    DirectExchange orchestratorExchange() {
        return new DirectExchange(ORCHESTRATOR_EXCHANGE);
    }

    @Bean
    Binding requestedOrgaBinding(Queue orgaRequestedQueue, DirectExchange orchestratorExchange) {
        return BindingBuilder.bind(orgaRequestedQueue).to(orchestratorExchange).with(ORGANIZATION_REQUEST_KEY);
    }

    @Bean
    Binding requestedOrgaConnectorBinding(Queue orgaConnectorRequestedQueue, DirectExchange orchestratorExchange) {
        return BindingBuilder.bind(orgaConnectorRequestedQueue).to(orchestratorExchange).with(ORGANIZATIONCONNECTOR_REQUEST_KEY);
    }

    @Bean
    public Queue orgaRequestedQueue() {
        return new Queue(ORGANIZATION_REQUEST_QUEUE, false);
    }

    @Bean
    public Queue orgaConnectorRequestedQueue() {
        return new Queue(ORGANIZATIONCONNECTOR_REQUEST_QUEUE, false);
    }

    @Bean
    DirectExchange didServiceExchange() {
        return new DirectExchange(DID_SERVICE_EXCHANGE);
    }

    @Bean
    public MessageConverter converter(){
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory){
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }
}
