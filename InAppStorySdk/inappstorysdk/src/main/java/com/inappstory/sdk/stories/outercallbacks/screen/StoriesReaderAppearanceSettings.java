package com.inappstory.sdk.stories.outercallbacks.screen;


import android.content.Context;
import android.content.res.Configuration;

import com.inappstory.sdk.AppearanceManager;
import com.inappstory.sdk.stories.ui.reader.StoriesGradientObject;
import com.inappstory.sdk.stories.utils.Sizes;

public class StoriesReaderAppearanceSettings implements SerializableWithKey {
    public static String SERIALIZABLE_KEY = "storiesReaderAppearanceSettings";

    public int csClosePosition() {
        return csClosePosition;
    }

    public int csStoryReaderAnimation() {
        return csStoryReaderAnimation;
    }

    public boolean csCloseOnOverscroll() {
        return csCloseOnOverscroll;
    }

    public boolean csCloseOnSwipe() {
        return csCloseOnSwipe;
    }

    public boolean csHasLike() {
        return csHasLike;
    }

    public boolean csHasFavorite() {
        return csHasFavorite;
    }

    public boolean csHasShare() {
        return csHasShare;
    }

    public int csCloseIcon() {
        return csCloseIcon;
    }

    public int csRefreshIcon() {
        return csRefreshIcon;
    }

    public int csSoundIcon() {
        return csSoundIcon;
    }

    public int csFavoriteIcon() {
        return csFavoriteIcon;
    }

    public int csLikeIcon() {
        return csLikeIcon;
    }

    public int csDislikeIcon() {
        return csDislikeIcon;
    }

    public int csShareIcon() {
        return csShareIcon;
    }

    public int csReaderRadius() {
        return csReaderRadius;
    }

    public boolean csTimerGradientEnable() {
        return csTimerGradientEnable;
    }

    public int csReaderBackgroundColor() {
        return csReaderBackgroundColor;
    }

    public StoriesGradientObject csTimerGradient() {
        return csTimerGradient;
    }

    public boolean csIsDraggable() {
        return csIsDraggable;
    }

    public int csNavBarColor() {
        return csNavBarColor;
    }

    private final int csClosePosition;
    private final int csStoryReaderAnimation;
    private final boolean csCloseOnOverscroll;
    private final boolean csCloseOnSwipe;
    private final boolean csHasLike;
    private final boolean csHasFavorite;
    private final boolean csHasShare;
    private final int csCloseIcon;
    private final int csRefreshIcon;
    private final int csSoundIcon;
    private final int csFavoriteIcon;
    private final int csLikeIcon;
    private final int csDislikeIcon;
    private final int csShareIcon;
    private final int csReaderRadius;
    private final boolean csTimerGradientEnable;
    private final int csReaderBackgroundColor;
    private final StoriesGradientObject csTimerGradient;
    private final boolean csIsDraggable;
    private final int csNavBarColor;

    public StoriesReaderAppearanceSettings(
            AppearanceManager manager,
            Context context
    ) {
        csClosePosition = manager.csClosePosition();
        csStoryReaderAnimation = manager.csStoryReaderAnimation();
        csCloseOnOverscroll = manager.csCloseOnOverscroll();
        csCloseOnSwipe = manager.csCloseOnSwipe();
        csHasLike = manager.csHasLike();
        csIsDraggable = manager.csIsDraggable();
        csHasFavorite = manager.csHasFavorite();
        csHasShare = manager.csHasShare();
        csCloseIcon = manager.csCloseIcon();
        csRefreshIcon = manager.csRefreshIcon();
        csSoundIcon = manager.csSoundIcon();
        csFavoriteIcon = manager.csFavoriteIcon();
        csLikeIcon = manager.csLikeIcon();
        csDislikeIcon = manager.csDislikeIcon();
        csShareIcon = manager.csShareIcon();
        csReaderRadius = manager.csReaderRadius(context);
        csTimerGradientEnable = manager.csTimerGradientEnable();
        csReaderBackgroundColor = manager.csReaderBackgroundColor();
        int nightModeFlags =
                context.getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK;
        csNavBarColor = nightModeFlags == Configuration.UI_MODE_NIGHT_YES ?
                manager.csNightNavBarColor() : manager.csNavBarColor();
        if (manager.csTimerGradient() != null) {
            csTimerGradient = manager.csTimerGradient();
        } else {
            csTimerGradient = new StoriesGradientObject()
                    .csGradientHeight(Sizes.getScreenSize(context).y);
        }
    }

    @Override
    public String getSerializableKey() {
        return SERIALIZABLE_KEY;
    }
}
