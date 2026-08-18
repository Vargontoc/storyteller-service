package es.vargontoc.storyteller.application.ports.out.external;

public interface OllamaPort {

    boolean isAvailableService();
    
    boolean isAvailable(String model);

    void stopAllServices();

}
