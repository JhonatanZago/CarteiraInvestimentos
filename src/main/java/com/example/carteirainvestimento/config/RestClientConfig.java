package com.example.carteirainvestimento.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    ClientHttpRequestFactory integrationRequestFactory(IntegrationHttpProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        return requestFactory;
    }

    @Bean
    RestClient.Builder restClientBuilder(ClientHttpRequestFactory integrationRequestFactory) {
        ClientHttpRequestInterceptor retryTransientGet = (request, body, execution) -> {
            int attempts = 0;
            while (true) {
                try {
                    var response = execution.execute(request, body);
                    if (request.getMethod() == org.springframework.http.HttpMethod.GET
                            && (response.getStatusCode().is5xxServerError()
                                || response.getStatusCode().value() == 429)
                            && attempts++ < 2) {
                        response.close();
                        Thread.sleep(attempts == 1 ? 100L : 250L);
                        continue;
                    }
                    return response;
                } catch (java.io.IOException exception) {
                    if (request.getMethod() != org.springframework.http.HttpMethod.GET || attempts++ >= 2) throw exception;
                    try { Thread.sleep(attempts == 1 ? 100L : 250L); }
                    catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw exception; }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new java.io.IOException("Integração interrompida", exception);
                }
            }
        };
        return RestClient.builder().requestFactory(integrationRequestFactory).requestInterceptor(retryTransientGet);
    }
}
