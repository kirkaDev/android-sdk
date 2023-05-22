package com.inappstory.sdk.game.reader;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;

import com.inappstory.sdk.AppearanceManager;
import com.inappstory.sdk.InAppStoryManager;
import com.inappstory.sdk.InAppStoryService;
import com.inappstory.sdk.eventbus.CsEventBus;
import com.inappstory.sdk.network.JsonParser;
import com.inappstory.sdk.network.NetworkCallback;
import com.inappstory.sdk.network.NetworkClient;
import com.inappstory.sdk.network.Response;
import com.inappstory.sdk.network.jsapiclient.JsApiClient;
import com.inappstory.sdk.network.jsapiclient.JsApiResponseCallback;
import com.inappstory.sdk.share.IASShareData;
import com.inappstory.sdk.stories.api.models.Session;
import com.inappstory.sdk.stories.api.models.Story;
import com.inappstory.sdk.stories.api.models.WebResource;
import com.inappstory.sdk.stories.callbacks.CallbackManager;
import com.inappstory.sdk.stories.outercallbacks.common.reader.ClickAction;
import com.inappstory.sdk.stories.outerevents.CallToAction;
import com.inappstory.sdk.stories.outerevents.ClickOnButton;
import com.inappstory.sdk.stories.outerevents.FinishGame;
import com.inappstory.sdk.stories.statistic.StatisticManager;
import com.inappstory.sdk.stories.ui.ScreensManager;
import com.inappstory.sdk.stories.utils.KeyValueStorage;
import com.inappstory.sdk.utils.StringsUtils;
import com.inappstory.sdk.utils.ZipLoadCallback;
import com.inappstory.sdk.utils.ZipLoader;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class GameManager {
    String storyId;
    String path;
    String feedId;
    String resources;
    String observableId;
    String loaderPath;
    String title;
    String tags;
    int index;
    int slidesCount;

    boolean gameLoaded;
    String gameConfig;

    ZipLoadCallback callback;

    public GameManager(GameActivity host) {
        this.host = host;
    }

    void loadGame() {
        ArrayList<WebResource> resourceList = new ArrayList<>();

        if (resources != null) {
            resourceList = JsonParser.listFromJson(resources, WebResource.class);
        }

        String[] urlParts = ZipLoader.urlParts(path);
        ZipLoader.getInstance().downloadAndUnzip(resourceList, path, urlParts[0], callback, "game");
    }


    void storySetData(String data, boolean sendToServer) {
        if (InAppStoryService.isNull()) return;
        KeyValueStorage.saveString("story" + storyId
                + "__" + InAppStoryService.getInstance().getUserId(), data);

        if (!InAppStoryService.getInstance().getSendStatistic()) return;
        if (sendToServer) {
            NetworkClient.getApi().sendStoryData(storyId, data, Session.getInstance().id)
                    .enqueue(new NetworkCallback<Response>() {
                        @Override
                        public void onSuccess(Response response) {

                        }

                        @Override
                        public Type getType() {
                            return null;
                        }
                    });
        }
    }

    GameActivity host;

    void showGoods(String skusString, String widgetId) {
        host.showGoods(skusString, widgetId);
    }


    void setAudioManagerMode(String mode) {
        host.setAudioManagerMode(mode);
    }

    void sendGameStat(String name, String data) {
        StatisticManager.getInstance().sendGameEvent(name, data, feedId);
    }

    void gameCompletedWithObject(String gameState, GameFinishOptions options, String eventData) {
        CsEventBus.getDefault().post(new FinishGame(Integer.parseInt(storyId), title, tags,
                slidesCount, index, eventData));
        if (CallbackManager.getInstance().getGameCallback() != null) {
            CallbackManager.getInstance().getGameCallback().finishGame(
                    Integer.parseInt(storyId), title, tags,
                    slidesCount, index, eventData);
        }
        if (options.openStory != null
                && options.openStory.id != null
                && !options.openStory.id.isEmpty()) {
            InAppStoryManager.getInstance().showStoryCustom(
                    options.openStory.id,
                    host,
                    AppearanceManager.getCommonInstance()
            );
        }
        host.gameCompleted(gameState, null);

    }

    void gameCompleted(String gameState, String urlOrOptions, String eventData) {
        GameFinishOptions options = JsonParser.fromJson(urlOrOptions, GameFinishOptions.class);
        if (options == null) {
            gameCompletedWithUrl(gameState, urlOrOptions, eventData);
        } else if (options.openUrl != null) {
            gameCompletedWithUrl(gameState, options.openUrl, eventData);
        } else {
            gameCompletedWithObject(gameState, options, eventData);
        }
    }

    void gameCompletedWithUrl(String gameState, String link, String eventData) {
        CsEventBus.getDefault().post(new FinishGame(Integer.parseInt(storyId), title, tags,
                slidesCount, index, eventData));
        if (CallbackManager.getInstance().getGameCallback() != null) {
            CallbackManager.getInstance().getGameCallback().finishGame(
                    Integer.parseInt(storyId), StringsUtils.getNonNull(title), StringsUtils.getNonNull(tags),
                    slidesCount, index, StringsUtils.getNonNull(eventData));
        }
        host.gameCompleted(gameState, link);
    }

    void sendApiRequest(String data) {
        new JsApiClient(host).sendApiRequest(data, new JsApiResponseCallback() {
            @Override
            public void onJsApiResponse(String result, String cb) {
                host.loadJsApiResponse(modifyJsResult(result), cb);
            }
        });
    }

    private String modifyJsResult(String data) {
        if (data == null) return "";
        data.replaceAll("'", "\\'");
        return data;
    }

    void tapOnLink(String link) {
        if (InAppStoryService.isNull()) return;
        Story story = InAppStoryService.getInstance().getDownloadManager().getStoryById(
                Integer.parseInt(storyId), Story.StoryType.COMMON);
        CsEventBus.getDefault().post(new ClickOnButton(story.id, story.statTitle,
                story.tags, story.getSlidesCount(), story.lastIndex,
                link));
        int cta = CallToAction.GAME;
        CsEventBus.getDefault().post(new CallToAction(story.id, story.statTitle,
                story.tags, story.getSlidesCount(), story.lastIndex,
                link, cta));
        if (CallbackManager.getInstance().getCallToActionCallback() != null) {
            CallbackManager.getInstance().getCallToActionCallback().callToAction(story.id, StringsUtils.getNonNull(story.statTitle),
                    StringsUtils.getNonNull(story.tags), story.getSlidesCount(), story.lastIndex,
                    StringsUtils.getNonNull(link), ClickAction.GAME);
        }
        // OldStatisticManager.getInstance().addLinkOpenStatistic();
        if (CallbackManager.getInstance().getUrlClickCallback() != null) {
            CallbackManager.getInstance().getUrlClickCallback().onUrlClick(
                    StringsUtils.getNonNull(link)
            );
        } else {
            host.tapOnLinkDefault(StringsUtils.getNonNull(link));
        }
    }

    int pausePlaybackOtherApp() {
        AudioManager am = (AudioManager) host.getSystemService(Context.AUDIO_SERVICE);
        return am.requestAudioFocus(host.audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
    }

    void gameLoaded(String data) {
        GameLoadedConfig config = JsonParser.fromJson(data, GameLoadedConfig.class);
        host.gameReaderGestureBack = config.backGesture;
        host.showClose = config.showClose;
        gameLoaded = true;
        host.updateUI();
    }


    void onResume() {
        String shareId = null;
        if (ScreensManager.getInstance().getTempShareId() != null) {
            shareId = ScreensManager.getInstance().getTempShareId();
        } else if (ScreensManager.getInstance().getOldTempShareId() != null) {
            shareId = ScreensManager.getInstance().getOldTempShareId();
        }
        if (shareId != null) {
            host.shareComplete(shareId, false);
        }
        ScreensManager.getInstance().clearShareIds();
    }

    void shareData(String id, String data) {
        IASShareData shareObj = JsonParser.fromJson(data, IASShareData.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            ScreensManager.getInstance().setTempShareId(id);
            ScreensManager.getInstance().setTempShareStoryId(-1);
        } else {
            ScreensManager.getInstance().setOldTempShareId(id);
            ScreensManager.getInstance().setOldTempShareStoryId(-1);
        }
        if (CallbackManager.getInstance().getShareCallback() != null) {
            host.shareCustom(shareObj);
        } else {
            host.shareDefault(shareObj);
        }
    }
}
