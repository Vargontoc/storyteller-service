package es.vargontoc.storyteller.application.ports.in.persistence;

import es.vargontoc.storyteller.domain.model.StoryPage;

public interface StoryPageUseCase {
    
    StoryPage getPage(Long idPage);

}
