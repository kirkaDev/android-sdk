package com.inappstory.sdk.stories.cache;

import androidx.annotation.NonNull;

public class UrlWithAlter {

    public boolean isSkippable() {
        return skippable;
    }

    private boolean skippable = false;
    public UrlWithAlter(@NonNull String url) {
        this.url = url;
    }

    private String url;

    public UrlWithAlter(@NonNull String url, String alter) {
        this.url = url;
        this.alter = alter;
        skippable = false;
    }

    private String alter;

    public String getUrl() {
        return url;
    }

    public String getAlter() {
        return alter;
    }
}
