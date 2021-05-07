package com.inappstory.sdk.stories.ui.widgets.readerscreen.storiespager;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.WebSettings;

import com.inappstory.sdk.InAppStoryManager;
import com.inappstory.sdk.InAppStoryService;
import com.inappstory.sdk.eventbus.CsEventBus;
import com.inappstory.sdk.game.reader.GameActivity;
import com.inappstory.sdk.network.JsonParser;
import com.inappstory.sdk.network.NetworkCallback;
import com.inappstory.sdk.network.NetworkClient;
import com.inappstory.sdk.network.Response;
import com.inappstory.sdk.stories.api.models.ShareObject;
import com.inappstory.sdk.stories.api.models.StatisticSession;
import com.inappstory.sdk.stories.api.models.Story;
import com.inappstory.sdk.stories.api.models.slidestructure.SlideStructure;
import com.inappstory.sdk.stories.cache.Downloader;
import com.inappstory.sdk.stories.cache.FileCache;
import com.inappstory.sdk.stories.cache.FileType;
import com.inappstory.sdk.stories.callbacks.CallbackManager;
import com.inappstory.sdk.stories.events.NoConnectionEvent;
import com.inappstory.sdk.stories.events.PageTaskToLoadEvent;
import com.inappstory.sdk.stories.events.StoryPageStartedEvent;
import com.inappstory.sdk.stories.events.StoryReaderTapEvent;
import com.inappstory.sdk.stories.outerevents.ShowSlide;
import com.inappstory.sdk.stories.outerevents.StartGame;
import com.inappstory.sdk.stories.ui.dialog.ContactDialog;
import com.inappstory.sdk.stories.ui.widgets.CoreProgressBar;
import com.inappstory.sdk.stories.ui.widgets.readerscreen.generated.SimpleStoriesGeneratedView;
import com.inappstory.sdk.stories.ui.widgets.readerscreen.webview.SimpleStoriesWebView;
import com.inappstory.sdk.stories.utils.KeyValueStorage;
import com.inappstory.sdk.stories.utils.StoryShareBroadcastReceiver;
import com.inappstory.sdk.stories.utils.WebPageConverter;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.inappstory.sdk.InAppStoryManager.testGenerated;
import static com.inappstory.sdk.game.reader.GameActivity.GAME_READER_REQUEST;
import static com.inappstory.sdk.stories.cache.HtmlParser.fromHtml;

public class StoriesViewManager {
    public int index = -1;

    public int loadedIndex = -1;
    public int loadedId = -1;

    public void setIndex(int index) {
        this.index = index;
    }

    public void setStoryId(int storyId) {
        this.storyId = storyId;
    }

    public int storyId;
    boolean slideInCache = false;

    public float getClickCoordinate() {
        return storiesView.getCoordinate();//storiesWebView.coordinate1;
    }


    public static final Pattern FONT_SRC = Pattern.compile("@font-face [^}]*src: url\\(['\"](http[^'\"]*)['\"]\\)");

    boolean lock = true;

    public void storyLoaded(int oId, int oInd) {
        if (storyId != oId || index != oInd) return;
        this.index = oInd;
        loadedIndex = oInd;
        loadedId = oId;
        Story story = InAppStoryService.getInstance().getDownloadManager().getStoryById(storyId);
        innerLoad(story);
    }

    public StoriesViewManager() {
    }

    Context context;

    public StoriesViewManager(Context context) {
        this.context = context;
    }

    void innerLoad(Story story) {
        if (InAppStoryService.isConnected()) {
            if (testGenerated) {
                initViews(story.slidesStructure.get(index));
            } else {
                String innerWebData = story.pages.get(index);
                String layout = getLayoutWithFonts(story.getLayout());
                setWebViewSettings(innerWebData, layout);
            }
        }
    }

    public void loadWebData(String layout, String webdata) {
        if (!(storiesView instanceof SimpleStoriesWebView)) return;
        ((SimpleStoriesWebView) storiesView).loadWebData(layout, webdata);
    }

    private void initViews(SlideStructure slideStructure) {

        ((SimpleStoriesGeneratedView) storiesView).initViews(slideStructure);
    }

    public void loadStory(final int id, final int index) {
        if (loadedId == id && loadedIndex == index) return;
        if (InAppStoryManager.getInstance() == null)
            return;
        if (!InAppStoryService.isConnected()) {
            CsEventBus.getDefault().post(new NoConnectionEvent(NoConnectionEvent.READER));
            return;
        }
        final Story story = InAppStoryService.getInstance().getDownloadManager().getStoryById(id);
        if (story == null || story.checkIfEmpty()) {
            return;
        }
        if (story.slidesCount <= index) return;
        storyId = id;
        this.index = index;
        loadedIndex = index;
        loadedId = id;
        slideInCache = InAppStoryService.getInstance().getDownloadManager().checkIfPageLoaded(id, index);
        if (!slideInCache) {
            CsEventBus.getDefault().post(new PageTaskToLoadEvent(storyId, index, false)); //animation
        } else {
            innerLoad(story);
        }
    }

    void setWebViewSettings(String innerWebData, String layout) {
        if (storiesView == null || !(storiesView instanceof SimpleStoriesWebView)) return;
        if (innerWebData.contains("<video")) {
            isVideo = true;
            ((SimpleStoriesWebView) storiesView).setLayerType(View.LAYER_TYPE_HARDWARE, null);
            WebPageConverter.replaceVideoAndLoad(innerWebData, storyId, index, layout);
        } else {
            isVideo = false;
            ((SimpleStoriesWebView) storiesView).setLayerType(View.LAYER_TYPE_HARDWARE, null);
            WebPageConverter.replaceImagesAndLoad(innerWebData, storyId, index, layout);
        }
    }

    String getLayoutWithFonts(String layout) {
        List<String> fonturls = new ArrayList<>();

        Matcher urlMatcher = FONT_SRC.matcher(layout);
        while (urlMatcher.find()) {
            if (urlMatcher.groupCount() == 1) {
                fonturls.add(fromHtml(urlMatcher.group(1)).toString());
            }
        }
        for (String fonturl : fonturls) {
            String fileLink = Downloader.getFontFile(storiesView.getContext(), fonturl);
            if (fileLink != null)
                layout = layout.replaceFirst(fonturl, "file://" + fileLink);
        }
        return layout;
    }


    boolean isVideo = false;

    void loadStoryInner(final int id, final int index, Story story) {

    }

    public void setStoriesView(SimpleStoriesView storiesWebView) {
        this.storiesView = storiesWebView;
        storiesWebView.checkIfClientIsSet();
    }


    public CoreProgressBar getProgressBar() {
        return progressBar;
    }

    public File getCurrentFile(String img) {
        Context con = InAppStoryManager.getInstance().getContext();
        FileCache cache = FileCache.INSTANCE;
        return cache.getStoredFile(con, img, FileType.STORY_FILE, Integer.toString(storyId), null);
    }

    private CoreProgressBar progressBar;

    public void setProgressBar(CoreProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    SimpleStoriesView storiesView;

    public void storyShowTextInput(String id, String data) {
        ContactDialog alert = new ContactDialog(storyId, id, data,
                new ContactDialog.SendListener() {
                    @Override
                    public void onSend(final String id, final String data) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                storiesView.sendDialog(id, data);
                            }
                        });
                    }
                },
                new ContactDialog.CancelListener() {
                    @Override
                    public void onCancel(final String id) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                storiesView.cancelDialog(id);
                            }
                        });
                    }
                });
        alert.showDialog((Activity) storiesView.getContext());
    }

    public void storyClick(String payload) {
        if (payload == null || payload.isEmpty() || payload.equals("test")) {
            if (InAppStoryService.isConnected()) {
                CsEventBus.getDefault().post(new StoryReaderTapEvent((int) getClickCoordinate()));
            } else {
                CsEventBus.getDefault().post(new NoConnectionEvent(NoConnectionEvent.READER));
            }
        } else if (payload.equals("forbidden")) {
            if (InAppStoryService.isConnected()) {
                CsEventBus.getDefault().post(new StoryReaderTapEvent((int) getClickCoordinate(), true));
            } else {
                CsEventBus.getDefault().post(new NoConnectionEvent(NoConnectionEvent.READER));
            }
        } else {
            CsEventBus.getDefault().post(new StoryReaderTapEvent(payload));
        }
    }

    public void shareComplete(int stId, boolean success) {
        if (storyId != stId) return;
        storiesView.shareComplete(stId, success);
    }

    public void share(String id, String data) {
        ShareObject shareObj = JsonParser.fromJson(data, ShareObject.class);
        if (CallbackManager.getInstance().getShareCallback() != null) {
            CallbackManager.getInstance().getShareCallback()
                    .onShare(shareObj.getUrl(), shareObj.getTitle(), shareObj.getDescription(), id);
        } else {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, shareObj.getTitle());
            sendIntent.putExtra(Intent.EXTRA_TEXT, shareObj.getUrl());
            sendIntent.setType("text/plain");
            PendingIntent pi = PendingIntent.getBroadcast(storiesView.getContext(), 989,
                    new Intent(storiesView.getContext(), StoryShareBroadcastReceiver.class),
                    FLAG_UPDATE_CURRENT);
            Intent finalIntent = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                finalIntent = Intent.createChooser(sendIntent, null, pi.getIntentSender());
                finalIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                InAppStoryManager.getInstance().setTempShareId(id);
                InAppStoryManager.getInstance().setTempShareStoryId(storyId);
                InAppStoryManager.getInstance().getContext().startActivity(finalIntent);
            } else {
                finalIntent = Intent.createChooser(sendIntent, null);
                finalIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                InAppStoryManager.getInstance().getContext().startActivity(finalIntent);
                InAppStoryManager.getInstance().setOldTempShareId(id);
                InAppStoryManager.getInstance().setOldTempShareStoryId(storyId);
            }
        }
    }

    public void storyStartedEvent() {
        if (InAppStoryService.getInstance() == null) return;
        CsEventBus.getDefault().post(new StoryPageStartedEvent(storyId, index));
    }

    public void openGameReader(String gameUrl, String preloadPath, String gameConfig, String resources) {
        Intent intent2 = new Intent(context, GameActivity.class);
        intent2.putExtra("gameUrl", gameUrl);
        intent2.putExtra("storyId", Integer.toString(storyId));
        intent2.putExtra("slideIndex", index);
        Story story = InAppStoryService.getInstance().getDownloadManager().getStoryById(storyId);
        intent2.putExtra("tags", story.tags);
        intent2.putExtra("slidesCount", story.slidesCount);
        intent2.putExtra("title", story.title);
        intent2.putExtra("gameConfig", gameConfig);
        intent2.putExtra("gameResources", resources);
        intent2.putExtra("preloadPath", preloadPath != null ? preloadPath : "");
        CsEventBus.getDefault().post(new StartGame(storyId, story.title, story.tags, story.slidesCount, index));
        ((Activity) context).startActivityForResult(intent2, GAME_READER_REQUEST);
    }

    public void storyLoaded() {
        if (InAppStoryService.getInstance() == null) return;
        if (InAppStoryService.getInstance().getCurrentId() != storyId) {
            storiesView.stopVideo();
        } else {
            storiesView.playVideo();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    storiesView.resumeVideo();
                }
            }, 200);
        }
        Story story = InAppStoryService.getInstance().getDownloadManager().getStoryById(storyId);
        CsEventBus.getDefault().post(new ShowSlide(story.id, story.title,
                story.tags, story.slidesCount, index));
        //   CsEventBus.getDefault().post(new StoryPageStartedEvent(storyId, index));
        CsEventBus.getDefault().post(new PageTaskToLoadEvent(storyId, index, true));
    }

    public void freezeUI() {
        storiesView.freezeUI();
    }

    public void storySetLocalData(String data, boolean sendToServer) {
        KeyValueStorage.saveString("story" + storyId
                + "__" + InAppStoryManager.getInstance().getUserId(), data);

        if (!InAppStoryManager.getInstance().sendStatistic) return;
        if (sendToServer) {
            NetworkClient.getApi().sendStoryData(Integer.toString(storyId), data, StatisticSession.getInstance().id)
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

    public void storySendData(String data) {
        if (!InAppStoryManager.getInstance().sendStatistic) return;
        NetworkClient.getApi().sendStoryData(Integer.toString(storyId), data, StatisticSession.getInstance().id)
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

    public void stopVideo() {
        storiesView.stopVideo();
    }

    public void playVideo() {
        storiesView.playVideo();
    }

    public void pauseVideo() {
        storiesView.pauseVideo();
    }

    public void resumeVideo() {
        storiesView.resumeVideo();
    }

    public void pauseStory() {
        pauseVideo();
    }

    public void resumeStory() {
        resumeVideo();
    }
}