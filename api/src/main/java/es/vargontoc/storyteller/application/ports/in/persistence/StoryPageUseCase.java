package es.vargontoc.storyteller.application.ports.in.persistence;

import java.util.List;

import es.vargontoc.storyteller.domain.model.StoryPage;

public interface StoryPageUseCase {
    
    List<StoryPage> getPages(Long idStory);
}
