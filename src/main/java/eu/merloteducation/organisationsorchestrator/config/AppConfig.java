package eu.merloteducation.organisationsorchestrator.config;

import eu.merloteducation.organisationsorchestrator.service.MerlotDidServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

public class AppConfig {
    @Value("${merlot-did-service.base-uri}")
    private String merlotDidServiceBaseUri;

    @Bean
    public MerlotDidServiceClient merlotDidServiceClient() {
        WebClient webClient = WebClient.builder()
                .baseUrl(merlotDidServiceBaseUri)
                .build();
        HttpServiceProxyFactory httpServiceProxyFactory = HttpServiceProxyFactory
                .builder(WebClientAdapter.forClient(webClient))
                .build();
        return httpServiceProxyFactory.createClient(MerlotDidServiceClient.class);
    }
}
