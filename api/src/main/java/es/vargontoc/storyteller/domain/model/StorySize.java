package es.vargontoc.storyteller.domain.model;

public enum StorySize {
    
    S(8),
    M(16),
    L(32);

    private int pages;

    private StorySize(int pages){
        this.pages = pages;
    }

    public int getPages(){ return pages; }

}
