package es.vargontoc.storyteller.application.ports.in.persistence;

import java.io.File;
import java.util.List;

import org.springframework.data.domain.PageRequest;

import es.vargontoc.storyteller.domain.model.ActorSummary;
import es.vargontoc.storyteller.domain.model.PageSummary;
import es.vargontoc.storyteller.domain.model.PaginatedResponse;
import es.vargontoc.storyteller.domain.model.StorySummary;


public interface StorytellerUseCase {

    StorySummary getStory(Long id);

    File downloadStory(Long storyId);

    PaginatedResponse<StorySummary> getStories(PageRequest request);

    List<ActorSummary> getActors(Long storyId);

    ActorSummary getActor(Long storyId, Long actorId);

    List<PageSummary> getPages(Long storyId);

    PageSummary getPage(Long storyId, Long pageId);

    void deleteStory(Long id);

    byte[] getResource(String path);

}
