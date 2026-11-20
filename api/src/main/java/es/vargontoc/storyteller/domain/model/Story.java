package es.vargontoc.storyteller.domain.model;

import java.util.List;

import es.vargontoc.storyteller.domain.enums.StorySize;
import es.vargontoc.storyteller.domain.enums.StoryStatus;
import es.vargontoc.storyteller.shared.BaseModel;

public class Story  extends BaseModel {
    
    private Long id;
    private String title;
    private List<Actor> characters;
    private List<StoryPage> pages;
    private String synopsis;
    private String agentSummary;
    private StorySize size;
    private StoryStatus status;
    private Integer scriptVersion;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public List<Actor> getCharacters() {
        return characters;
    }
    public void setCharacters(List<Actor> characters) {
        this.characters = characters;
    }
    public String getSynopsis() {
        return synopsis;
    }
    public void setSynopsis(String summary) {
        this.synopsis = summary;
    }
    public StorySize getSize() {
        return size;
    }
    public void setSize(StorySize size) {
        this.size = size;
    }
    public List<StoryPage> getPages() {
        return pages;
    }
    public void setPages(List<StoryPage> pages) {
        this.pages = pages;
    }
    /** Version de guion; null en cuentos antiguos. No reinterpreta las listas antiguas de actores. */
    public Integer getScriptVersion() {
        return scriptVersion;
    }
    public void setScriptVersion(Integer scriptVersion) {
        this.scriptVersion = scriptVersion;
    }
    public boolean hasOutline() {
        return StoryScriptVersion.hasOutline(scriptVersion);
    }
    public StoryStatus getStatus() {
        return status;
    }
    public void setStatus(StoryStatus status) {
        this.status = status;
    }
    public String getAgentSummary() {
        return agentSummary;
    }
    public void setAgentSummary(String agentSummary) {
        this.agentSummary = agentSummary;
    }
    
}
