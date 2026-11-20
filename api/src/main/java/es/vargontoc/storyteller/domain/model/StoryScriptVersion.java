package es.vargontoc.storyteller.domain.model;

/**
 * Marcador de version de guion. Un cuento antiguo no tiene marcador (null) y ningun backfill;
 * un cuento nuevo lleva {@link #OUTLINE}.
 */
public final class StoryScriptVersion {

    /** Version de guion con escaleta contractual. */
    public static final int OUTLINE = 1;

    private StoryScriptVersion() { }

    public static boolean hasOutline(Integer scriptVersion) {
        return scriptVersion != null && scriptVersion >= OUTLINE;
    }
}
