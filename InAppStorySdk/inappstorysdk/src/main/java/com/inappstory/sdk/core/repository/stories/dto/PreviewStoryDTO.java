package com.inappstory.sdk.core.repository.stories.dto;

import androidx.annotation.Nullable;

import com.inappstory.sdk.stories.api.models.Image;
import com.inappstory.sdk.stories.api.models.Story;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class PreviewStoryDTO implements IPreviewStoryDTO {
    public PreviewStoryDTO(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getStatTitle() {
        return statTitle;
    }

    public String getTags() {
        return tags;
    }

    public String getDeeplink() {
        return deeplink;
    }

    public String getGameInstanceId() {
        return gameInstanceId;
    }

    public boolean isHideInReader() {
        return hideInReader;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    @Override
    public List<Image> getImages() {
        return images;
    }

    public String getTitleColor() {
        return titleColor;
    }

    public boolean hasAudio() {
        return hasAudio;
    }

    public boolean isOpened() {
        return isOpened;
    }

    @Override
    public void open() {
        isOpened = true;
    }
    public String getImageUrl(int coverQuality) {
        Image proper = Image.getProperImage(images, coverQuality);
        if (proper != null) return proper.getUrl();
        return null;
    }

    public int getSlidesCount() {
        return slidesCount;
    }

    public HashMap<String, Object> getPayload() {
        return payload;
    }

    public boolean isHasAudio() {
        return hasAudio;
    }


    private int id;
    private String title;

    private String statTitle;
    private String tags;
    private String deeplink;
    private String gameInstanceId;
    private boolean hideInReader;
    private List<Image> images;
    private String videoUrl;
    private String backgroundColor;
    private String titleColor;
    private boolean hasAudio;
    private boolean isOpened;

    private HashMap<String, Object> payload;
    int slidesCount;

    public PreviewStoryDTO(Story story) {
        this.id = story.id;
        this.slidesCount = story.getSlidesCount();
        this.tags = story.tags;
        this.backgroundColor = story.getBackgroundColor();
        this.titleColor = story.getTitleColor();
        this.statTitle = story.statTitle;
        this.title = story.getTitle();
        this.deeplink = story.getDeeplink();
        this.gameInstanceId = story.getGameInstanceId();
        this.hasAudio = story.hasAudio();
        this.isOpened = story.isOpened();
        this.hideInReader = story.isHideInReader();
        this.videoUrl = story.getVideoUrl();
        this.payload = story.payload;
        this.images = story.getImage();
    }


    @Override
    public boolean equals(@Nullable Object o) {
        if (o == this)
            return true;
        if (!(o instanceof PreviewStoryDTO))
            return false;
        PreviewStoryDTO other = (PreviewStoryDTO)o;
        return (this.getId() == other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
