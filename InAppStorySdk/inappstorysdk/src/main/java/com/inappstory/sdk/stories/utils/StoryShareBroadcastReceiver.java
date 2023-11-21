package com.inappstory.sdk.stories.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.inappstory.sdk.stories.ui.ScreensManager;

public class StoryShareBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String shareId = null;
        if (ScreensManager.getInstance().getTempShareId() != null) {
            shareId = ScreensManager.getInstance().getTempShareId();
        } else if (ScreensManager.getInstance().getOldTempShareId() != null) {
            shareId = ScreensManager.getInstance().getOldTempShareId();
        }
        if (shareId != null) {
            if (ScreensManager.getInstance().currentGameScreen != null) {
                ScreensManager.getInstance().currentGameScreen.shareComplete(
                        shareId, true);
            } else {
                if (ScreensManager.getInstance().currentStoriesReaderScreen != null)
                    ScreensManager.getInstance().currentStoriesReaderScreen.shareComplete(true);
            }
        }
    }
}
