package com.inappstory.sdk.stories.uidomain.reader.views.storiesdisplay;

public class SlideContentState {
    public SlideContentState(String layout, String page) {
        this.layout = layout;
        this.page = page;
    }

    public String getLayout() {
        return layout;
    }

    public String getPage() {
        return page;
    }

    private String layout;
    private String page;
}