package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import es.vargontoc.storyteller.application.ports.out.persistence.VoiceRepository;
import es.vargontoc.storyteller.application.utils.AudioAssembler;
import es.vargontoc.storyteller.domain.model.Voice;
import es.vargontoc.storyteller.infrastructure.config.SystemVoiceProperties;

class SystemVoiceProviderContextTest {

    @Configuration
    @EnableConfigurationProperties(SystemVoiceProperties.class)
    static class PropertiesConfig {
    }

    @TempDir
    File dir;

    private final VoiceRepository voices = mock(VoiceRepository.class);

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withUserConfiguration(PropertiesConfig.class)
                .withBean(VoiceAudioInspector.class)
                .withBean(VoiceRepository.class, () -> voices)
                .withBean(SystemVoiceProvider.class);
    }

    private String sample(String fileName, int seconds) throws Exception {
        File file = new File(dir, fileName);
        Files.write(file.toPath(), AudioAssembler.silence(Duration.ofSeconds(seconds)));
        return file.getAbsolutePath();
    }

    @Test
    void validConfigurationIsAvailableWithThatName() throws Exception {
        String file = sample("a.wav", 10);

        runner().withPropertyValues("app.system-voice.name=Voz de prueba", "app.system-voice.sample-file=" + file)
                .run(context -> {
                    SystemVoiceProvider provider = context.getBean(SystemVoiceProvider.class);
                    assertTrue(provider.isAvailable());
                    assertEquals("Voz de prueba", provider.name());
                    assertNull(provider.unavailableReason());
                    assertFalse(provider.collidesWithUploadedVoice());
                    assertTrue(provider.sample().isPresent());
                });
    }

    @Test
    void changedConfigurationAfterRestartShowsNewName() throws Exception {
        String first = sample("a.wav", 10);
        String second = sample("b.wav", 12);

        runner().withPropertyValues("app.system-voice.name=Primera", "app.system-voice.sample-file=" + first)
                .run(context -> assertEquals("Primera", context.getBean(SystemVoiceProvider.class).name()));

        runner().withPropertyValues("app.system-voice.name=Segunda", "app.system-voice.sample-file=" + second)
                .run(context -> {
                    SystemVoiceProvider provider = context.getBean(SystemVoiceProvider.class);
                    assertTrue(provider.isAvailable());
                    assertEquals("Segunda", provider.name());
                });
    }

    @Test
    void notConfiguredIsUnavailableAndContextStarts() {
        runner().run(context -> {
            assertTrue(context.isRunning());
            SystemVoiceProvider provider = context.getBean(SystemVoiceProvider.class);
            assertFalse(provider.isAvailable());
            assertNotNull(provider.unavailableReason());
            assertNull(provider.name());
            assertTrue(provider.sample().isEmpty());
        });
    }

    @Test
    void emptyValuesAreUnavailableAndContextStarts() {
        runner().withPropertyValues("app.system-voice.name=", "app.system-voice.sample-file=").run(context -> {
            assertTrue(context.isRunning());
            assertFalse(context.getBean(SystemVoiceProvider.class).isAvailable());
        });
    }

    @Test
    void missingSampleFileIsUnavailableAndContextStarts() {
        runner().withPropertyValues("app.system-voice.name=Voz",
                "app.system-voice.sample-file=" + new File(dir, "no-existe.wav").getAbsolutePath()).run(context -> {
                    assertTrue(context.isRunning());
                    SystemVoiceProvider provider = context.getBean(SystemVoiceProvider.class);
                    assertFalse(provider.isAvailable());
                    assertTrue(provider.unavailableReason().contains("no existe"));
                });
    }

    @Test
    void invalidSampleIsUnavailableAndContextStarts() throws Exception {
        File text = new File(dir, "fake.wav");
        Files.write(text.toPath(), "esto no es audio".getBytes(StandardCharsets.UTF_8));
        String tooShort = sample("short.wav", 3);

        for (String file : new String[] { text.getAbsolutePath(), tooShort }) {
            runner().withPropertyValues("app.system-voice.name=Voz", "app.system-voice.sample-file=" + file)
                    .run(context -> {
                        assertTrue(context.isRunning());
                        SystemVoiceProvider provider = context.getBean(SystemVoiceProvider.class);
                        assertFalse(provider.isAvailable());
                        assertNotNull(provider.unavailableReason());
                    });
        }
    }

    @Test
    void nameMatchingUploadedVoiceIsDetected() throws Exception {
        String file = sample("a.wav", 10);
        when(voices.findByName("Abuela")).thenReturn(Optional.of(new Voice("Abuela", "es", true)));

        runner().withPropertyValues("app.system-voice.name=Abuela", "app.system-voice.sample-file=" + file)
                .run(context -> {
                    SystemVoiceProvider provider = context.getBean(SystemVoiceProvider.class);
                    assertTrue(context.isRunning());
                    assertTrue(provider.isAvailable());
                    assertTrue(provider.collidesWithUploadedVoice());
                });
    }

    @Test
    void repositoryFailureDoesNotPreventStartup() throws Exception {
        String file = sample("a.wav", 10);
        when(voices.findByName("Voz")).thenThrow(new IllegalStateException("db caida"));

        runner().withPropertyValues("app.system-voice.name=Voz", "app.system-voice.sample-file=" + file)
                .run(context -> {
                    assertTrue(context.isRunning());
                    assertFalse(context.getBean(SystemVoiceProvider.class).collidesWithUploadedVoice());
                });
    }
}
