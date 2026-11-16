package es.vargontoc.storyteller.application.ports.out.external;

import java.util.List;

public interface PortraitGeneratorPort {
    List<byte[]> generatePortraits(String visualDescription);
}
