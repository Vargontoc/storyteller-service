package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import es.vargontoc.storyteller.application.ports.in.character.CharacterCatalogUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CharacterCatalogControllerTest {
    @Test
    void delegatesPortraitCommandsAndResolvesAssetsByCharacterAndCandidateIds() {
        CharacterCatalogUseCase catalog = Mockito.mock(CharacterCatalogUseCase.class);
        byte[] image = new byte[] {1, 2};
        when(catalog.candidateAsset(7L, 11L)).thenReturn(image);
        CharacterCatalogController controller = new CharacterCatalogController(catalog);

        controller.generate(7L);
        controller.select(7L, 11L);

        assertArrayEquals(image, controller.candidateAsset(7L, 11L).getBody());
        verify(catalog).generatePortraits(7L);
        verify(catalog).selectPortrait(7L, 11L);
        verify(catalog).candidateAsset(7L, 11L);
    }
    @Test void resolvesApprovedPortraitOnlyByCatalogId() {
        CharacterCatalogUseCase catalog = Mockito.mock(CharacterCatalogUseCase.class);
        when(catalog.approvedPortraitAsset(7L)).thenReturn(new byte[] {3});
        assertArrayEquals(new byte[] {3}, new CharacterCatalogController(catalog).approvedPortraitAsset(7L).getBody());
        verify(catalog).approvedPortraitAsset(7L);
    }
}
