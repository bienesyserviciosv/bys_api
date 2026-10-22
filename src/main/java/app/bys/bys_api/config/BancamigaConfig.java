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

    @Value("${bancamiga.c2p.read-timeout-ms}")
    private int c2pReadTimeoutMs;

    @Bean
    public RestClient bancamigaRestClient() {
        // baseUrl puede quedar vacio mientras la interconexion de red no este lista;
        // el bean se crea igual, solo falla al invocarlo (ver BancamigaClientImpl).
        return buildClient(readTimeoutMs);
    }

    @Bean
    public BancamigaC2pRestClient bancamigaC2pRestClient() {
        return new BancamigaC2pRestClient(buildClient(c2pReadTimeoutMs));
    }

    private RestClient buildClient(int readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
