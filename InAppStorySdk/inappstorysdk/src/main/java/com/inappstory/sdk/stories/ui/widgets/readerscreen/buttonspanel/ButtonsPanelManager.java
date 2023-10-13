package com.inappstory.sdk.stories.ui.widgets.readerscreen.buttonspanel;

import android.os.Build;

import com.inappstory.sdk.InAppStoryManager;
import com.inappstory.sdk.InAppStoryService;
import com.inappstory.sdk.inner.share.InnerShareData;
import com.inappstory.sdk.network.NetworkClient;
import com.inappstory.sdk.network.callbacks.NetworkCallback;
import com.inappstory.sdk.network.models.Response;
import com.inappstory.sdk.stories.api.models.ShareObject;
import com.inappstory.sdk.stories.api.models.Story;
import com.inappstory.sdk.stories.callbacks.CallbackManager;
import com.inappstory.sdk.stories.callbacks.FavoriteCallback;
import com.inappstory.sdk.stories.outercallbacks.common.objects.SlideData;
import com.inappstory.sdk.stories.outercallbacks.common.objects.StoryData;
import com.inappstory.sdk.stories.statistic.ProfilingManager;
import com.inappstory.sdk.stories.statistic.StatisticManager;
import com.inappstory.sdk.stories.ui.ScreensManager;
import com.inappstory.sdk.stories.ui.widgets.readerscreen.storiespager.ReaderPageManager;
import com.inappstory.sdk.usecase.callbacks.IUseCaseCallback;
import com.inappstory.sdk.usecase.callbacks.UseCaseCallbackLikeDislikeStory;
import com.inappstory.sdk.usecase.callbacks.UseCaseCallbackShareClick;
import com.inappstory.sdk.utils.StringsUtils;

import java.lang.reflect.Type;

public class ButtonsPanelManager {
    public void setStoryIdAndType(int storyId, Story.StoryType type) {
        this.storyId = storyId;
        this.type = type;
    }

    int storyId;

    Story.StoryType type;

    public ReaderPageManager getParentManager() {
        return parentManager;
    }

    public void setParentManager(ReaderPageManager parentManager) {
        this.parentManager = parentManager;
    }

    public void unlockShareButton() {
        if (panel != null && panel.share != null) {
            panel.share.setClickable(true);
            panel.share.setEnabled(true);
        }
    }

    public ButtonsPanelManager(ButtonsPanel panel) {
        this.panel = panel;
    }

    ReaderPageManager parentManager;

    public void likeClick(ButtonClickCallback callback) {
        likeDislikeClick(callback, true);
    }

    public void dislikeClick(ButtonClickCallback callback) {
        likeDislikeClick(callback, false);
    }

    private void likeDislikeClick(final ButtonClickCallback callback, boolean like) {
        InAppStoryService inAppStoryService = InAppStoryService.getInstance();
        if (inAppStoryService == null) return;
        NetworkClient networkClient = InAppStoryManager.getNetworkClient();
        if (networkClient == null) {
            return;
        }
        Story story = inAppStoryService.getDownloadManager().getStoryById(storyId, parentManager.getStoryType());
        if (story == null) return;
        final int val;
        SlideData slideData = new SlideData(
                new StoryData(
                        story.id,
                        StringsUtils.getNonNull(story.statTitle),
                        StringsUtils.getNonNull(story.tags),
                        story.getSlidesCount(),
                        getParentManager().getFeedId(),
                        getParentManager().getSourceType()

                ),
                story.lastIndex
        );
        IUseCaseCallback likeDislikeUseCaseCallback = new UseCaseCallbackLikeDislikeStory(
                slideData,
                like,
                like ? story.liked() : story.disliked()
        );
        likeDislikeUseCaseCallback.invoke();
        if (like) {
            if (story.liked()) {
                val = 0;
            } else {
                StatisticManager.getInstance().sendLikeStory(story.id, story.lastIndex,
                        parentManager != null ? parentManager.getFeedId() : null);
                val = 1;
            }
        } else {
            if (story.disliked()) {
                val = 0;
            } else {
                StatisticManager.getInstance().sendDislikeStory(story.id, story.lastIndex,
                        parentManager != null ? parentManager.getFeedId() : null);
                val = -1;
            }
        }
        final String likeUID =
                ProfilingManager.getInstance().addTask("api_like");
        networkClient.enqueue(
                networkClient.getApi().storyLike(
                        Integer.toString(storyId),
                        val
                ),
                new NetworkCallback<Response>() {
                    @Override
                    public void onSuccess(Response response) {
                        ProfilingManager.getInstance().setReady(likeUID);
                        Story story = InAppStoryService.getInstance().getDownloadManager().getStoryById(
                                storyId,
                                parentManager.getStoryType()
                        );
                        if (story != null)
                            story.like = val;
                        if (callback != null)
                            callback.onSuccess(val);
                    }


                    @Override
                    public void errorDefault(String message) {

                        ProfilingManager.getInstance().setReady(likeUID);
                        if (callback != null)
                            callback.onError();
                    }

                    @Override
                    public Type getType() {
                        return null;
                    }
                });
    }

    public void removeStoryFromFavorite() {
        if (panel != null)
            panel.forceRemoveFromFavorite();
    }

    public void favoriteClick(final ButtonClickCallback callback) {
        InAppStoryService inAppStoryService = InAppStoryService.getInstance();
        if (inAppStoryService == null) return;
        Story story = inAppStoryService.getDownloadManager().getStoryById(storyId, parentManager.getStoryType());
        if (story == null) return;
        SlideData slideData = new SlideData(
                new StoryData(
                        story.id,
                        StringsUtils.getNonNull(story.statTitle),
                        StringsUtils.getNonNull(story.tags),
                        story.getSlidesCount(),
                        getParentManager().getFeedId(),
                        getParentManager().getSourceType()
                ),
                story.lastIndex
        );
        inAppStoryService.favoriteStory(
                storyId,
                parentManager.getStoryType(),
                parentManager.parentManager.listID,
                slideData,
                new FavoriteCallback() {
                    @Override
                    public void addedToFavorite(Story story) {
                        if (callback != null)
                            callback.onSuccess(1);
                    }

                    @Override
                    public void removedFromFavorite() {
                        if (callback != null)
                            callback.onSuccess(0);
                    }

                    @Override
                    public void onError() {

                    }
                }
        );
    }

    ButtonsPanel panel;

    public void soundClick() {
        parentManager.changeSoundStatus();
    }

    public void refreshSoundStatus() {
        if (panel != null)
            panel.refreshSoundStatus();
    }

    public abstract static class ShareButtonClickCallback implements ButtonClickCallback {
        abstract void onClick();
    }

    public void shareClick(final ShareButtonClickCallback callback) {
        InAppStoryService inAppStoryService = InAppStoryService.getInstance();
        NetworkClient networkClient = InAppStoryManager.getNetworkClient();
        if (networkClient == null) {
            return;
        }
        if (inAppStoryService == null || inAppStoryService.isShareProcess())
            return;
        final Story story = inAppStoryService.getDownloadManager().getStoryById(storyId, parentManager.getStoryType());
        if (story == null) return;
        final int slideIndex = story.lastIndex;
        StatisticManager.getInstance().sendShareStory(story.id, slideIndex,
                story.shareType(slideIndex),
                parentManager != null ? parentManager.getFeedId() : null);
        IUseCaseCallback shareClick = new UseCaseCallbackShareClick(
                new SlideData(
                        new StoryData(
                                story.id,
                                StringsUtils.getNonNull(story.statTitle),
                                StringsUtils.getNonNull(story.tags),
                                story.getSlidesCount(),
                                getParentManager().getFeedId(),
                                getParentManager().getSourceType()
                        ),
                        story.lastIndex
                )
        );
        shareClick.invoke();
        if (story.isScreenshotShare(slideIndex)) {
            parentManager.screenshotShare();
            return;
        }
        inAppStoryService.isShareProcess(true);
        if (callback != null)
            callback.onClick();
        final String shareUID = ProfilingManager.getInstance().addTask("api_share");
        networkClient.enqueue(
                networkClient.getApi().share(
                        Integer.toString(storyId),
                        null
                ),
                new NetworkCallback<ShareObject>() {
                    @Override
                    public void onSuccess(ShareObject response) {
                        ProfilingManager.getInstance().setReady(shareUID);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                            ScreensManager.getInstance().setTempShareId(null);
                            ScreensManager.getInstance().setTempShareStoryId(storyId);
                        } else {
                            ScreensManager.getInstance().setOldTempShareId(null);
                            ScreensManager.getInstance().setOldTempShareStoryId(storyId);
                        }
                        InnerShareData shareData = new InnerShareData();
                        shareData.text = response.getUrl();
                        shareData.payload = story.getSlideEventPayload(slideIndex);
                        if (parentManager != null) {
                            parentManager.showShareView(shareData);
                        }
                    }

                    @Override
                    public void errorDefault(String message) {
                        if (callback != null)
                            callback.onError();
                        InAppStoryService service = InAppStoryService.getInstance();
                        if (service != null) service.isShareProcess(false);
                    }

                    @Override
                    public Type getType() {
                        return ShareObject.class;
                    }
                }
        );
    }
}
