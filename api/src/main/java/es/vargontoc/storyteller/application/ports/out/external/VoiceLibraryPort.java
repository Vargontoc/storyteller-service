package es.vargontoc.storyteller.application.ports.out.external;

import java.util.List;

import es.vargontoc.storyteller.domain.model.VoiceInfo;

public interface VoiceLibraryPort {
    
    List<VoiceInfo> listVoices();

    void uploadVoice(String voiceName, byte[] audioBytes, String filename, String language);

    void setDefaultVoice(String voiceName);

    void deleteVoice(String voiceName);

    void renameVoice(String oldName, String name);
}
