package com.inappstory.sdk.stories.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import com.inappstory.sdk.AppearanceManager;
import com.inappstory.sdk.InAppStoryManager;
import com.inappstory.sdk.InAppStoryService;
import com.inappstory.sdk.stories.outerevents.ShowStory;
import com.inappstory.sdk.stories.ui.reader.StoriesActivity;
import com.inappstory.sdk.stories.ui.reader.StoriesDialogFragment;
import com.inappstory.sdk.stories.ui.reader.StoriesFixedActivity;
import com.inappstory.sdk.stories.utils.Sizes;

import java.util.ArrayList;

import static com.inappstory.sdk.AppearanceManager.CS_CLOSE_POSITION;
import static com.inappstory.sdk.AppearanceManager.CS_NAVBAR_COLOR;
import static com.inappstory.sdk.AppearanceManager.CS_READER_OPEN_ANIM;
import static com.inappstory.sdk.AppearanceManager.CS_STORY_READER_ANIMATION;

public class ScreensManager {

    private ScreensManager() {

    }

    private static volatile ScreensManager INSTANCE;

    public static ScreensManager getInstance() {
        if (INSTANCE == null) {
            synchronized (ScreensManager.class) {
                if (INSTANCE == null)
                    INSTANCE = new ScreensManager();
            }
        }
        return INSTANCE;
    }

    public void openStoriesReader(Context outerContext, AppearanceManager manager,
                                  ArrayList<Integer> storiesIds, int index, int source) {
        if (Sizes.isTablet() && outerContext != null && outerContext instanceof AppCompatActivity) {
            DialogFragment settingsDialogFragment = new StoriesDialogFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("index", index);
            bundle.putInt("source", source);
            bundle.putIntegerArrayList("stories_ids", storiesIds);
            if (manager != null) {
                bundle.putInt(CS_CLOSE_POSITION, manager.csClosePosition());
                bundle.putInt(CS_STORY_READER_ANIMATION, manager.csStoryReaderAnimation());
                bundle.putInt(CS_READER_OPEN_ANIM, manager.csStoryReaderAnimation());
            }
            settingsDialogFragment.setArguments(bundle);
            settingsDialogFragment.show(
                    ((AppCompatActivity) outerContext).getSupportFragmentManager(),
                    "DialogFragment");
        } else {
            if (StoriesActivity.destroyed == -1) return;
            StoriesActivity.destroyed = -1;
            Intent intent2 = new Intent(InAppStoryManager.getInstance().getContext(),
                    (AppearanceManager.getInstance() == null || AppearanceManager.getInstance().csIsDraggable()) ?
                            StoriesActivity.class : StoriesFixedActivity.class);
            intent2.putExtra("index", index);
            intent2.putExtra("source", source);
            intent2.putIntegerArrayListExtra("stories_ids", storiesIds);

            Context ctx = (InAppStoryManager.getInstance() != null ?
                    InAppStoryManager.getInstance().getContext() : outerContext);
            if (manager != null) {
                int nightModeFlags =
                        ctx.getResources().getConfiguration().uiMode &
                                Configuration.UI_MODE_NIGHT_MASK;
                intent2.putExtra(CS_NAVBAR_COLOR, nightModeFlags == Configuration.UI_MODE_NIGHT_YES ?
                        manager.csNightNavBarColor() : manager.csNavBarColor());
                intent2.putExtra(CS_CLOSE_POSITION, manager.csClosePosition());
                intent2.putExtra(CS_STORY_READER_ANIMATION, manager.csStoryReaderAnimation());
                intent2.putExtra(CS_NAVBAR_COLOR, manager.csNavBarColor());
            }
            if (outerContext == null) {
                intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                InAppStoryManager.getInstance().getContext().startActivity(intent2);
            } else {
                outerContext.startActivity(intent2);
            }
        }
    }
}
