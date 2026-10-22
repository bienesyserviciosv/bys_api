package app.bys.bys_api.config;

import org.springframework.web.client.RestClient;

/**
 * RestClient de Bancamiga con un timeout de lectura mas largo, solo para el cobro C2P
 * (el banco puede tardar en confirmar). Es un tipo propio para no chocar con el bean bancamigaRestClient.
 */
public record BancamigaC2pRestClient(RestClient client) {
}
