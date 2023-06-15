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
    public static final String ORGANIZATION_REQUEST_QUEUE = "organization.request.organization.queue";
    @Bean
    DirectExchange orchestratorExchange() {
        return new DirectExchange(ORCHESTRATOR_EXCHANGE);
    }

    @Bean
    Binding requestedOrgaBinding(Queue orgaRequestedQueue, DirectExchange orchestratorExchange) {
        return BindingBuilder.bind(orgaRequestedQueue).to(orchestratorExchange).with(ORGANIZATION_REQUEST_KEY);
    }

    @Bean
    public Queue orgaRequestedQueue() {
        return new Queue(ORGANIZATION_REQUEST_QUEUE, false);
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
