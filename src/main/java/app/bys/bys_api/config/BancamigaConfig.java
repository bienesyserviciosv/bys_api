package app.bys.bys_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class BancamigaConfig {

    @Value("${bancamiga.api.base-url}")
    private String baseUrl;

    @Value("${bancamiga.api.connect-timeout-ms}")
    private int connectTimeoutMs;

    @Value("${bancamiga.api.read-timeout-ms}")
    private int readTimeoutMs;

    @Bean
    public RestClient bancamigaRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);

        // baseUrl puede quedar vacio mientras la interconexion de red no este lista;
        // el bean se crea igual, solo falla al invocarlo (ver BancamigaClientImpl).
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
