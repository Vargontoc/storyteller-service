package es.vargontoc.storyteller.domain.model;

import es.vargontoc.storyteller.domain.enums.PageStatus;
import es.vargontoc.storyteller.shared.BaseModel;

public class StoryPage  extends BaseModel {
    
    private Long id;
    private Long storyId;
    private String coverText;
    private int page;
    private String text;
    private String scene;
    private String imageAsset;
    private String audioAsset;
    private SceneComposition composition;
    /** Solo cuentos con escaleta; null en los antiguos. */
    private PageStatus pageStatus;
    /** Beat vinculado a la página N; null en la portada y en los cuentos antiguos. */
    private Long beatId;
    /** Imagen importada del contenido aceptado; null si no tiene. */
    private String importedImage;

    /** Se llama pageStatus porque StoryPageReview, que extiende esta clase, ya tiene getStatus(). */
    public PageStatus getPageStatus() {
        return pageStatus;
    }
    public void setPageStatus(PageStatus pageStatus) {
        this.pageStatus = pageStatus;
    }
    public Long getBeatId() {
        return beatId;
    }
    public void setBeatId(Long beatId) {
        this.beatId = beatId;
    }
    public String getImportedImage() {
        return importedImage;
    }
    public void setImportedImage(String importedImage) {
        this.importedImage = importedImage;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public int getPage() {
        return page;
    }
    public void setPage(int page) {
        this.page = page;
    }
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
    public String getScene() {
        return scene;
    }
    public void setScene(String scene) {
        this.scene = scene;
    }
    public String getImageAsset() {
        return imageAsset;
    }
    public void setImageAsset(String imageAsset) {
        this.imageAsset = imageAsset;
    }
    public String getAudioAsset() {
        return audioAsset;
    }
    public void setAudioAsset(String audioAsset) {
        this.audioAsset = audioAsset;
    }
    public Long getStoryId() {
        return storyId;
    }
    public void setStoryId(Long storyId) {
        this.storyId = storyId;
    }
    public String getCoverText() {
        return coverText;
    }
    public void setCoverText(String coverText) {
        this.coverText = coverText;
    }
    public SceneComposition getComposition() {
        return composition;
    }
    public void setComposition(SceneComposition composition) {
        this.composition = composition;
    }

    
}
