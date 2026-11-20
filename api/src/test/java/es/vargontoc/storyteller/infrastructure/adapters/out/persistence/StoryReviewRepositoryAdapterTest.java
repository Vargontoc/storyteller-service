package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import es.vargontoc.storyteller.application.ports.out.persistence.StoryOutlineRepository;
import es.vargontoc.storyteller.domain.enums.RevisionStatus;
import es.vargontoc.storyteller.domain.model.EpisodicCastMember;
import es.vargontoc.storyteller.domain.model.StoryBeat;
import es.vargontoc.storyteller.domain.model.StoryOutline;
import es.vargontoc.storyteller.domain.model.StoryReview;
import es.vargontoc.storyteller.infrastructure.mappers.StoryReviewMapper;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryReviewJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryReviewJpaRepository;

class StoryReviewRepositoryAdapterTest {

    private final StoryReviewJpaRepository reviews = mock(StoryReviewJpaRepository.class);
    private final StoryJpaRepository stories = mock(StoryJpaRepository.class);
    private final StoryOutlineRepository outlines = mock(StoryOutlineRepository.class);
    private final StoryReviewRepositoryAdapter adapter =
            new StoryReviewRepositoryAdapter(reviews, stories, new StoryReviewMapper(), outlines);

    @Test
    void storesAndReadsBackTheFullCandidateWithChangedBeats() {
        when(stories.findById(5L)).thenReturn(Optional.of(new StoryJpaEntity()));
        when(outlines.find(5L)).thenReturn(Optional.of(outline("a", "b", "c")));
        when(reviews.save(any(StoryReviewJpaEntity.class))).thenAnswer(i -> i.getArgument(0));

        StoryReview review = adapter.createOutlineReview(5L, "mas aventura", true, null, "Titulo", "Sinopsis",
                outline("a", "B", "c"));

        assertEquals(RevisionStatus.PENDING, review.getStatus());
        assertTrue(review.isHintAccepted());
        assertEquals(outline("a", "B", "c"), review.getCandidateOutline());
        assertEquals(List.of(2), review.getChangedBeats());
        assertEquals("Titulo", review.getPreviewStory().getTitle());
    }

    @Test
    void emptyDiffWhenCandidateEqualsCurrent() {
        when(stories.findById(5L)).thenReturn(Optional.of(new StoryJpaEntity()));
        when(outlines.find(5L)).thenReturn(Optional.of(outline("a", "b")));
        when(reviews.save(any(StoryReviewJpaEntity.class))).thenAnswer(i -> i.getArgument(0));

        StoryReview review = adapter.createOutlineReview(5L, "h", true, null, "t", "s", outline("a", "b"));

        assertTrue(review.getChangedBeats().isEmpty());
    }

    @Test
    void rejectedHintIsDiscardedWithReasonAndNoOutline() {
        when(stories.findById(5L)).thenReturn(Optional.of(new StoryJpaEntity()));
        when(reviews.save(any(StoryReviewJpaEntity.class))).thenAnswer(i -> i.getArgument(0));

        StoryReview review = adapter.createOutlineReview(5L, "h", false, "no apto", null, null, null);

        assertEquals(RevisionStatus.DISCARDED, review.getStatus());
        assertEquals("no apto", review.getRejectedReason());
        assertNull(review.getCandidateOutline());
    }

    @Test
    void incompleteAcceptedCandidateIsRejected() {
        when(stories.findById(5L)).thenReturn(Optional.of(new StoryJpaEntity()));

        assertThrows(IllegalArgumentException.class,
                () -> adapter.createOutlineReview(5L, "h", true, null, "t", "s", new StoryOutline(List.of(), List.of())));
        verify(reviews, never()).save(any());
    }

    @Test
    void confirmingPublishesTheCandidateAsCurrentOutline() {
        StoryReviewJpaEntity pending = pendingWithCandidate(outline("a", "B"));
        when(reviews.getActiveStoryReviewByStoryId(5L)).thenReturn(Optional.of(pending));

        adapter.changeStatus(5L, RevisionStatus.CONFIRMED);

        verify(outlines).save(5L, outline("a", "B"));
        assertEquals(RevisionStatus.CONFIRMED, pending.getStatus());
    }

    @Test
    void discardingKeepsTheCurrentOutlineAndLeavesNoPendingReview() {
        StoryReviewJpaEntity pending = pendingWithCandidate(outline("a", "B"));
        when(reviews.getActiveStoryReviewByStoryId(5L)).thenReturn(Optional.of(pending));

        adapter.changeStatus(5L, RevisionStatus.DISCARDED);
        when(reviews.getActiveStoryReviewByStoryId(5L)).thenReturn(Optional.empty());

        verify(outlines, never()).save(eq(5L), any());
        assertEquals(RevisionStatus.DISCARDED, pending.getStatus());
        assertNull(adapter.getPendingReview(5L));
    }

    @Test
    void confirmingAfterConfirmationHasNoPendingReviewToPublish() {
        when(reviews.getActiveStoryReviewByStoryId(5L)).thenReturn(Optional.empty());

        adapter.changeStatus(5L, RevisionStatus.CONFIRMED);

        verify(outlines, never()).save(eq(5L), any());
    }

    private StoryReviewJpaEntity pendingWithCandidate(StoryOutline candidate) {
        StoryReviewJpaEntity entity = new StoryReviewJpaEntity();
        entity.setStatus(RevisionStatus.PENDING);
        new StoryReviewMapper().fillOutline(entity, candidate, List.of(2));
        return entity;
    }

    private StoryOutline outline(String... events) {
        return new StoryOutline(List.of(new EpisodicCastMember("Bruno", "amigo")),
                java.util.Arrays.stream(events)
                        .map(e -> new StoryBeat(e, "funcion", List.of("Luna", "Bruno"), "alegre", "bosque"))
                        .toList());
    }
}
