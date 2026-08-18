package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import es.vargontoc.storyteller.application.ports.out.external.VoiceLibraryPort;
import es.vargontoc.storyteller.domain.model.VoiceInfo;
import es.vargontoc.storyteller.domain.response.VoiceLibraryResponse;
import es.vargontoc.storyteller.infrastructure.config.ChatterboxProperties;

@Component
public class ChatterboxVoiceClient extends ChatterboxAdapter implements  VoiceLibraryPort {

    public ChatterboxVoiceClient(ChatterboxProperties properties) {
        super(properties);
    }

    @Override
    public List<VoiceInfo> listVoices() {
        if(!isAvailableService())
            return List.of();
        
        VoiceLibraryResponse response = restClient.get().uri("/voices").retrieve().body(VoiceLibraryResponse.class);
        return response  != null ? response.voices() : List.of();
    }

    @Override
    public void uploadVoice(String voiceName, byte[] audioBytes, String filename, String language) {
        if(!isAvailableService())
            return;

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("voice_name", voiceName);
        form.add("language", language);
        form.add("voice_file", new ByteArrayResource(audioBytes) {
            @Override
            public @Nullable String getFilename() {
                return filename;
            }
        });

        restClient.post().uri("/voices").contentType(MediaType.MULTIPART_FORM_DATA).body(form).retrieve().toBodilessEntity();
    }

    @Override
    public void setDefaultVoice(String voiceName) {
        if(!isAvailableService())
            return;

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("voice_name", voiceName);
        restClient.post().uri("/voice/default").contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().toBodilessEntity();
    }

    @Override
    public void deleteVoice(String voiceName) {
        if(!isAvailableService())
            return;

        restClient.delete().uri("/voices/{name}", voiceName).retrieve().toBodilessEntity();
    }

    @Override
    public void renameVoice(String oldName, String name) {
        if(!isAvailableService())
            return;

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("new_name", name);
        restClient.put().uri("/voices/{name}", oldName).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().toBodilessEntity();
    }
    
}
