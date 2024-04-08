package eu.merloteducation.organisationsorchestrator.config;

import eu.merloteducation.organisationsorchestrator.service.OmejdnConnectorApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class AppConfig {

    @Value("${daps-connector-api.base-uri}")
    private String dapsConnectorApiBaseUri;

    @Bean
    public OmejdnConnectorApiClient dapsConnectorApiClient() {
        WebClient webClient = WebClient.builder()
                .baseUrl(dapsConnectorApiBaseUri)
                .build();
        HttpServiceProxyFactory httpServiceProxyFactory = HttpServiceProxyFactory
                .builder()
                .exchangeAdapter(WebClientAdapter.create(webClient))
                .build();
        return httpServiceProxyFactory.createClient(OmejdnConnectorApiClient.class);
    }
}
