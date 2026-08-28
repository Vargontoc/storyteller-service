package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import es.vargontoc.storyteller.application.ports.out.external.ChatterboxPort;
import es.vargontoc.storyteller.infrastructure.config.ChatterboxProperties;

public abstract class ChatterboxAdapter implements ChatterboxPort {

    protected final RestClient restClient;
    protected final ChatterboxProperties properties;

    public ChatterboxAdapter(ChatterboxProperties properties) {
        // Cliente JDK sin pool de conexiones de Reactor Netty: evita reusar
        // conexiones keep-alive que el servidor ya cerró por su lado
        // (PrematureCloseException) en el ping de disponibilidad.
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
        this.restClient = RestClient.builder()
            .baseUrl(properties.baseUrl())
            .requestFactory(new JdkClientHttpRequestFactory(httpClient))
            .build();
        this.properties = properties;
    }

    @Override
    public boolean isAvailableService() {
        try {
                restClient.get().uri("/ping").retrieve().toBodilessEntity();
                return true;
        }catch(Exception e) {
                return false;
        }
    }
}
