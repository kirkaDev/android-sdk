package com.inappstory.sdk;

import static com.inappstory.sdk.lrudiskcache.LruDiskCache.MB_10;
import static com.inappstory.sdk.lrudiskcache.LruDiskCache.MB_100;
import static com.inappstory.sdk.lrudiskcache.LruDiskCache.MB_200;
import static com.inappstory.sdk.lrudiskcache.LruDiskCache.MB_5;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.inappstory.sdk.lrudiskcache.CacheSize;
import com.inappstory.sdk.network.ApiSettings;
import com.inappstory.sdk.network.NetworkClient;
import com.inappstory.sdk.network.callbacks.NetworkCallback;
import com.inappstory.sdk.network.models.Response;
import com.inappstory.sdk.network.utils.HostFromSecretKey;
import com.inappstory.sdk.stories.api.models.ExceptionCache;
import com.inappstory.sdk.stories.api.models.Feed;
import com.inappstory.sdk.stories.api.models.ImagePlaceholderValue;
import com.inappstory.sdk.stories.api.models.Story;
import com.inappstory.sdk.stories.api.models.StoryPlaceholder;
import com.inappstory.sdk.stories.api.models.callbacks.GetStoryByIdCallback;
import com.inappstory.sdk.stories.api.models.callbacks.LoadFeedCallback;
import com.inappstory.sdk.stories.api.models.callbacks.OpenSessionCallback;
import com.inappstory.sdk.stories.api.models.logs.ApiLogRequest;
import com.inappstory.sdk.stories.api.models.logs.ApiLogResponse;
import com.inappstory.sdk.stories.api.models.logs.ExceptionLog;
import com.inappstory.sdk.stories.api.models.logs.WebConsoleLog;
import com.inappstory.sdk.stories.cache.Downloader;
import com.inappstory.sdk.stories.callbacks.AppClickCallback;
import com.inappstory.sdk.stories.callbacks.CallbackManager;
import com.inappstory.sdk.stories.callbacks.ExceptionCallback;
import com.inappstory.sdk.stories.callbacks.IShowStoryCallback;
import com.inappstory.sdk.stories.callbacks.ShareCallback;
import com.inappstory.sdk.stories.callbacks.UrlClickCallback;
import com.inappstory.sdk.stories.exceptions.ExceptionManager;
import com.inappstory.sdk.stories.outercallbacks.common.errors.ErrorCallback;
import com.inappstory.sdk.stories.outercallbacks.common.gamereader.GameReaderCallback;
import com.inappstory.sdk.stories.outercallbacks.common.objects.DefaultOpenGameReader;
import com.inappstory.sdk.stories.outercallbacks.common.objects.DefaultOpenStoriesReader;
import com.inappstory.sdk.stories.outercallbacks.common.objects.IOpenGameReader;
import com.inappstory.sdk.stories.outercallbacks.common.objects.IOpenStoriesReader;
import com.inappstory.sdk.stories.outercallbacks.common.objects.StoriesReaderAppearanceSettings;
import com.inappstory.sdk.stories.outercallbacks.common.objects.StoriesReaderLaunchData;
import com.inappstory.sdk.stories.outercallbacks.common.onboarding.OnboardingLoadCallback;
import com.inappstory.sdk.stories.outercallbacks.common.reader.CallToActionCallback;
import com.inappstory.sdk.stories.outercallbacks.common.reader.ClickOnShareStoryCallback;
import com.inappstory.sdk.stories.outercallbacks.common.reader.CloseStoryCallback;
import com.inappstory.sdk.stories.outercallbacks.common.reader.FavoriteStoryCallback;
import com.inappstory.sdk.stories.outercallbacks.common.reader.LikeDislikeStoryCallback;
import com.inappstory.sdk.stories.outercallbacks.common.reader.ShowSlideCallback;
import com.inappstory.sdk.stories.outercallbacks.common.reader.ShowStoryCallback;
import com.inappstory.sdk.stories.outercallbacks.common.reader.SourceType;
import com.inappstory.sdk.stories.outercallbacks.common.reader.StoryWidgetCallback;
import com.inappstory.sdk.stories.outercallbacks.common.single.SingleLoadCallback;
import com.inappstory.sdk.stories.outerevents.CloseStory;
import com.inappstory.sdk.stories.outerevents.ShowStory;
import com.inappstory.sdk.stories.stackfeed.IStackFeedActions;
import com.inappstory.sdk.stories.stackfeed.IStackFeedResult;
import com.inappstory.sdk.stories.stackfeed.IStackStoryData;
import com.inappstory.sdk.stories.stackfeed.StackStoryCoverLoadType;
import com.inappstory.sdk.stories.stackfeed.StackStoryObserver;
import com.inappstory.sdk.stories.stackfeed.StackStoryUpdatedCallback;
import com.inappstory.sdk.stories.statistic.OldStatisticManager;
import com.inappstory.sdk.stories.statistic.ProfilingManager;
import com.inappstory.sdk.stories.statistic.SharedPreferencesAPI;
import com.inappstory.sdk.stories.ui.ScreensManager;
import com.inappstory.sdk.stories.utils.KeyValueStorage;
import com.inappstory.sdk.stories.utils.RunnableCallback;
import com.inappstory.sdk.stories.utils.SessionManager;
import com.inappstory.sdk.utils.IVibrateUtils;
import com.inappstory.sdk.utils.StringsUtils;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Main class for work with SDK.
 * Need to initialize it first with {@link Builder} before other interactions.
 * Singleton class, can be available with {@link #getInstance()}.
 * Can be reinitialized.
 */
public class InAppStoryManager {

    private static InAppStoryManager INSTANCE;

    public static NetworkClient getNetworkClient() {
        synchronized (lock) {
            if (INSTANCE == null) return null;
            return INSTANCE.networkClient;
        }
    }

    public static void useInstance(@NonNull UseManagerInstanceCallback callback) {
        InAppStoryManager manager = getInstance();
        try {
            if (manager != null) {
                callback.use(manager);
            } else {
                callback.error();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    IVibrateUtils vibrateUtils = new VibrateUtils();

    public IVibrateUtils getVibrateUtils() {
        return vibrateUtils;
    }

    public static boolean isNull() {
        synchronized (lock) {
            return INSTANCE == null;
        }
    }

    public static void setInstance(InAppStoryManager manager) {
        synchronized (lock) {
            INSTANCE = manager;
        }
    }

    public Context getContext() {
        return context;
    }

    Context context;


    static final String DEBUG_API = "IAS debug api";


    @SuppressLint(DEBUG_API)
    public static void debugSDKCalls(String methodName, String args) {
        Log.d("InAppStory_SDKCalls", System.currentTimeMillis()
                + " "
                + methodName + " " + args);
    }

    @SuppressLint(DEBUG_API)
    public static IASLogger logger;

    @SuppressLint(DEBUG_API)
    public interface IASLogger {
        void showELog(String tag, String message);

        void showDLog(String tag, String message);
    }

    @SuppressLint(DEBUG_API)
    public static IAS_QA_Log iasQaLog;

    @SuppressLint(DEBUG_API)
    public static void showELog(String tag, String message) {
        IASLogger currentLogger = logger != null ? logger : defaultLogger;
        if (currentLogger != null) currentLogger.showELog(tag, message);
    }

    private static final IASLogger defaultLogger = new IASLogger() {
        @Override
        public void showELog(String tag, String message) {
            Log.e(tag, message);
        }

        @Override
        public void showDLog(String tag, String message) {
            Log.d(tag, message);
        }
    };

    @SuppressLint(DEBUG_API)
    public static void showDLog(String tag, String message) {
        if (logger != null) logger.showDLog(tag, message);
    }

    @SuppressLint(DEBUG_API)
    public static void sendApiRequestLog(ApiLogRequest log) {
        if (iasQaLog != null) iasQaLog.getApiRequestLog(log);
    }

    @SuppressLint(DEBUG_API)
    public static void sendApiResponseLog(ApiLogResponse log) {
        if (iasQaLog != null)
            iasQaLog.getApiResponseLog(log);
    }

    @SuppressLint(DEBUG_API)
    public static void sendApiRequestResponseLog(ApiLogRequest logRequest,
                                                 ApiLogResponse logResponse) {
        if (iasQaLog != null)
            iasQaLog.getApiRequestResponseLog(logRequest, logResponse);
    }

    @SuppressLint(DEBUG_API)
    public static void sendExceptionLog(ExceptionLog log) {
        if (iasQaLog != null) iasQaLog.getExceptionLog(log);
    }

    @SuppressLint(DEBUG_API)
    public static void sendWebConsoleLog(WebConsoleLog log) {
        if (iasQaLog != null) iasQaLog.getWebConsoleLog(log);
    }

    /**
     * use set custom callback in case of uncaught exceptions.
     *
     * @param callback (callback). Has {@link ExceptionCallback} type
     */
    public void setCallback(ExceptionCallback callback) {
        this.exceptionCallback = callback;
    }

    public ExceptionCallback getExceptionCallback() {
        return exceptionCallback;
    }

    private ExceptionCallback exceptionCallback;


    /**
     * @return {@link ArrayList} of tags
     */
    public ArrayList<String> getTags() {
        synchronized (tagsLock) {
            return tags;
        }
    }

    //Test

    /**
     * use to clear downloaded files and in-app cache
     */
    public void clearCache() {
        InAppStoryService.useInstance(new UseServiceInstanceCallback() {
            @Override
            public void use(@NonNull InAppStoryService service) {
                service.getDownloadManager().clearCache();
            }
        });
    }
    //Test

    /**
     * use to clear downloaded files and in-app cache without manager
     */
    public void clearCache(Context context) {
        InAppStoryService.useInstance(new UseServiceInstanceCallback() {
            @Override
            public void use(@NonNull InAppStoryService service) {
                service.getDownloadManager().clearCache();
            }
        });
    }

    /**
     * use to force close story reader
     */
    public static void closeStoryReader() {
        closeStoryReader(CloseStory.CUSTOM);
    }

    @Deprecated
    public void openGame(String gameId) {
        InAppStoryService service = InAppStoryService.getInstance();
        if (service != null && context != null) {
            service.openGameReaderWithGC(context, null, gameId, null);
        }
    }

    public void openGame(String gameId, @NonNull Context context) {
        InAppStoryService service = InAppStoryService.getInstance();
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                ScreensManager.getInstance().closeStoryReader(CloseStory.CUSTOM);
            }
        });
        if (service != null) {
            service.openGameReaderWithGC(context, null, gameId, null);
        }
    }

    public void closeGame() {
        ScreensManager.getInstance().closeGameReader();
    }

    /**
     * use to force close story reader
     */
    public static void closeStoryReader(final int action) {

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                ScreensManager.getInstance().closeStoryReader(action);
                ScreensManager.getInstance().closeGameReader();
                ScreensManager.getInstance().closeUGCEditor();
            }
        });
    }

    /**
     * use to set callback on different errors
     */
    public void setErrorCallback(ErrorCallback errorCallback) {
        CallbackManager.getInstance().setErrorCallback(errorCallback);
    }

    /**
     * use to set callback on share click
     */
    public void setClickOnShareStoryCallback(ClickOnShareStoryCallback clickOnShareStoryCallback) {
        CallbackManager.getInstance().setClickOnShareStoryCallback(clickOnShareStoryCallback);
    }


    /**
     * use to set callback on game start/close/finish
     */
    public void setGameReaderCallback(GameReaderCallback gameReaderCallback) {
        CallbackManager.getInstance().setGameReaderCallback(gameReaderCallback);
    }

    /**
     * use to set callback on onboardings load
     */
    public void setOnboardingLoadCallback(OnboardingLoadCallback onboardingLoadCallback) {
        CallbackManager.getInstance().setOnboardingLoadCallback(onboardingLoadCallback);
    }

    /**
     * use to set callback on click on buttons in stories (with info)
     */
    public void setCallToActionCallback(CallToActionCallback callToActionCallback) {
        CallbackManager.getInstance().setCallToActionCallback(callToActionCallback);
    }

    /**
     * use to set callback on click on widgets in stories (with info)
     */
    public void setStoryWidgetCallback(StoryWidgetCallback storyWidgetCallback) {
        CallbackManager.getInstance().setStoryWidgetCallback(storyWidgetCallback);
    }


    /**
     * use to set callback on stories reader closing
     */
    public void setCloseStoryCallback(CloseStoryCallback closeStoryCallback) {
        CallbackManager.getInstance().setCloseStoryCallback(closeStoryCallback);
    }

    /**
     * use to set callback on favorite action
     */
    public void setFavoriteStoryCallback(FavoriteStoryCallback favoriteStoryCallback) {
        CallbackManager.getInstance().setFavoriteStoryCallback(favoriteStoryCallback);
    }

    /**
     * use to set callback on like/dislike action
     */
    public void setLikeDislikeStoryCallback(LikeDislikeStoryCallback likeDislikeStoryCallback) {
        CallbackManager.getInstance().setLikeDislikeStoryCallback(likeDislikeStoryCallback);
    }

    /**
     * use to set callback on slide shown in reader
     */
    public void setShowSlideCallback(ShowSlideCallback showSlideCallback) {
        CallbackManager.getInstance().setShowSlideCallback(showSlideCallback);
    }

    /**
     * use to set callback on story shown in reader
     */
    public void setShowStoryCallback(ShowStoryCallback showStoryCallback) {
        CallbackManager.getInstance().setShowStoryCallback(showStoryCallback);
    }

    /**
     * use to set callback on single story loading
     */
    public void setSingleLoadCallback(SingleLoadCallback singleLoadCallback) {
        CallbackManager.getInstance().setSingleLoadCallback(singleLoadCallback);
    }

    /**
     * use to set callback on click on buttons in stories (without additional info)
     */
    @Deprecated
    public void setUrlClickCallback(UrlClickCallback urlClickCallback) {
        CallbackManager.getInstance().setUrlClickCallback(urlClickCallback);
    }


    /**
     * use to customize share functional
     */
    public void setShareCallback(ShareCallback shareCallback) {
        CallbackManager.getInstance().setShareCallback(shareCallback);
    }

    /**
     * use to customize click on non-url buttons in reader
     */
    public void setAppClickCallback(AppClickCallback appClickCallback) {
        CallbackManager.getInstance().setAppClickCallback(appClickCallback);
    }

    //Test

    /**
     * @return {@link String} with tags joined by comma
     */
    public String getTagsString() {
        synchronized (tagsLock) {
            if (tags == null) return null;
            return TextUtils.join(",", tags);
        }
    }

    /**
     * use to customize tags in runtime. Replace tags array.
     *
     * @param tags (tags)
     */

    public void setTags(ArrayList<String> tags) {
        if (tags != null && getBytesLength(TextUtils.join(",", tags)) > TAG_LIMIT) {
            showELog(IAS_ERROR_TAG, getErrorStringFromContext(context, R.string.ias_setter_tags_length_error));
            return;
        }
        synchronized (tagsLock) {
            this.tags = tags;
            clearCachedLists();
        }
    }


    private final static int TAG_LIMIT = 4000;

    private Object tagsLock = new Object();

    /**
     * use to customize tags in runtime. Adds tags to array.
     *
     * @param newTags (newTags) - list of additional tags
     */

    public void addTags(ArrayList<String> newTags) {
        synchronized (tagsLock) {
            if (newTags == null || newTags.isEmpty()) return;
            if (tags == null) tags = new ArrayList<>();
            String oldTagsString = TextUtils.join(",", tags);
            String newTagsString = TextUtils.join(",", newTags);
            if (getBytesLength(oldTagsString + newTagsString) > TAG_LIMIT - 1) {
                showELog(IAS_ERROR_TAG, getErrorStringFromContext(context, R.string.ias_setter_tags_length_error));
                return;
            }
            for (String tag : newTags) {
                addTag(tag);
            }
            clearCachedLists();
        }
    }

    /**
     * use to customize tags in runtime. Removes tags from array.
     *
     * @param removedTags (removedTags) - list of removing tags
     */

    public void removeTags(ArrayList<String> removedTags) {
        synchronized (tagsLock) {
            if (tags == null || removedTags == null || removedTags.isEmpty()) return;
            for (String tag : removedTags) {
                removeTag(tag);
            }
            clearCachedLists();
        }
    }

    /**
     * use to customize tags in runtime. Adds tag to array.
     *
     * @param tag (tag) - single additional tag
     */
    private void addTag(String tag) {
        if (!tags.contains(tag)) tags.add(tag);
    }

    /**
     * use to customize tags in runtime. Removes tag from array.
     *
     * @param tag (tag) - single removing tags
     */
    private void removeTag(String tag) {
        if (tags.contains(tag)) tags.remove(tag);
    }

    /**
     * use to customize default string in stories runtime.
     *
     * @param key   (key) - what we replace
     * @param value (value) - replacement result
     */
    public void setPlaceholder(String key, String value) {
        synchronized (placeholdersLock) {
            if (defaultPlaceholders == null) defaultPlaceholders = new HashMap<>();
            if (placeholders == null) placeholders = new HashMap<>();
            if (value == null) {
                if (defaultPlaceholders.containsKey(key)) {
                    placeholders.put(key, defaultPlaceholders.get(key));
                } else {
                    placeholders.remove(key);
                }
            } else {
                placeholders.put(key, value);
            }

        }
    }

    /**
     * use to customize default strings in stories runtime.
     *
     * @param newPlaceholders (newPlaceholders) - key-value map (key - what we replace, value - replacement result)
     */
    public void setPlaceholders(@NonNull Map<String, String> newPlaceholders) {
        synchronized (placeholdersLock) {
            if (defaultPlaceholders == null) defaultPlaceholders = new HashMap<>();
            if (this.placeholders == null)
                this.placeholders = new HashMap<>();
            else
                this.placeholders.clear();
            for (String key : newPlaceholders.keySet()) {
                String value = newPlaceholders.get(key);
                if (value == null) {
                    if (defaultPlaceholders.containsKey(key)) {
                        this.placeholders.put(key, defaultPlaceholders.get(key));
                    } else {
                        this.placeholders.remove(key);
                    }
                } else {
                    this.placeholders.put(key, value);
                }
            }
        }
    }

    void setDefaultPlaceholders(@NonNull List<StoryPlaceholder> placeholders) {
        synchronized (placeholdersLock) {
            for (StoryPlaceholder placeholder : placeholders) {
                String key = placeholder.name;
                this.defaultPlaceholders.put(key,
                        placeholder.defaultVal);
                if (!this.placeholders.containsKey(key)) {
                    InAppStoryManager.getInstance().placeholders.put(key,
                            placeholder.defaultVal);
                }
            }
        }
    }

    public Map<String, String> getPlaceholdersCopy() {
        synchronized (placeholdersLock) {
            if (placeholders == null) return new HashMap<>();
            return new HashMap<>(placeholders);
        }
    }

    ArrayList<String> tags;

    private final Object placeholdersLock = new Object();

    /**
     * Returns map with all default strings replacements
     */
    public Map<String, String> getPlaceholders() {
        synchronized (placeholdersLock) {
            if (defaultPlaceholders == null) defaultPlaceholders = new HashMap<>();
            if (placeholders == null) placeholders = new HashMap<>();
            return placeholders;
        }
    }

    public Map<String, ImagePlaceholderValue> getImagePlaceholdersValues() {
        synchronized (placeholdersLock) {
            Map<String, ImagePlaceholderValue> resultPlaceholders = new HashMap<>();
            if (defaultImagePlaceholders == null) defaultImagePlaceholders = new HashMap<>();
            if (imagePlaceholders == null) imagePlaceholders = new HashMap<>();
            resultPlaceholders.putAll(defaultImagePlaceholders);
            resultPlaceholders.putAll(imagePlaceholders);
            return resultPlaceholders;
        }
    }

    public Map<String, Pair<ImagePlaceholderValue, ImagePlaceholderValue>> getImagePlaceholdersValuesWithDefaults() {
        synchronized (placeholdersLock) {
            Map<String, Pair<ImagePlaceholderValue, ImagePlaceholderValue>> resultPlaceholders = new HashMap<>();
            Map<String, ImagePlaceholderValue> tempPlaceholders = new HashMap<>();
            if (defaultImagePlaceholders == null) defaultImagePlaceholders = new HashMap<>();
            if (imagePlaceholders == null) imagePlaceholders = new HashMap<>();

            tempPlaceholders.putAll(defaultImagePlaceholders);
            tempPlaceholders.putAll(imagePlaceholders);
            for (Map.Entry<String, ImagePlaceholderValue> entry : tempPlaceholders.entrySet()) {
                if (defaultImagePlaceholders.containsKey(entry.getKey())) {
                    resultPlaceholders.put(
                            entry.getKey(),
                            new Pair<>(
                                    entry.getValue(),
                                    //entry.getValue()
                                    defaultImagePlaceholders.get(entry.getKey())
                            )
                    );
                } else {
                    resultPlaceholders.put(
                            entry.getKey(),
                            new Pair<>(
                                    entry.getValue(),
                                    entry.getValue()
                            )
                    );
                }
            }
            return resultPlaceholders;
        }
    }


    public void setImagePlaceholders(@NonNull Map<String, ImagePlaceholderValue> placeholders) {
        synchronized (placeholdersLock) {
            imagePlaceholders.clear();
            if (imagePlaceholders == null)
                imagePlaceholders = new HashMap<>();
            else
                imagePlaceholders.clear();
            imagePlaceholders.putAll(placeholders);
        }
    }

    void setDefaultImagePlaceholders(@NonNull Map<String, ImagePlaceholderValue> placeholders) {
        synchronized (placeholdersLock) {
            if (defaultImagePlaceholders == null) defaultImagePlaceholders = new HashMap<>();
            defaultImagePlaceholders.clear();
            defaultImagePlaceholders.putAll(placeholders);
        }
    }

    void setDefaultImagePlaceholder(@NonNull String key, @NonNull ImagePlaceholderValue value) {
        synchronized (placeholdersLock) {
            if (defaultImagePlaceholders == null) defaultImagePlaceholders = new HashMap<>();
            defaultImagePlaceholders.put(key, value);
        }
    }


    public void setImagePlaceholder(@NonNull String key, ImagePlaceholderValue value) {
        synchronized (placeholdersLock) {
            if (imagePlaceholders == null) imagePlaceholders = new HashMap<>();
            if (value == null) imagePlaceholders.remove(key);
            else imagePlaceholders.put(key, value);
        }
    }

    Map<String, String> placeholders = new HashMap<>();
    Map<String, ImagePlaceholderValue> imagePlaceholders = new HashMap<>();

    public Map<String, String> getDefaultPlaceholders() {
        synchronized (placeholdersLock) {
            if (defaultPlaceholders == null) defaultPlaceholders = new HashMap<>();
            if (placeholders == null) placeholders = new HashMap<>();
            return defaultPlaceholders;
        }
    }

    Map<String, String> defaultPlaceholders = new HashMap<>();
    Map<String, ImagePlaceholderValue> defaultImagePlaceholders = new HashMap<>();

    private static final String TEST_DOMAIN = "https://api.test.inappstory.com/";
    private static final String PRODUCT_DOMAIN = "https://api.inappstory.ru/";

    public String getApiKey() {
        return API_KEY;
    }

    public String getTestKey() {
        return TEST_KEY;
    }

    String API_KEY = "";

    String TEST_KEY = null;

    public static void initSDK(@NonNull Context context) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        boolean calledFromApplication = false;
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            try {
                if (Application.class.isAssignableFrom(Class.forName(stackTraceElement.getClassName()))) {
                    calledFromApplication = true;
                }
            } catch (ClassNotFoundException e) {

            }
        }
        if (!(context instanceof Application)) calledFromApplication = false;
        if (!calledFromApplication)
            showELog(IAS_ERROR_TAG, "Method must be called from Application class and context has to be an applicationContext");
        synchronized (lock) {
            if (INSTANCE == null) {
                INSTANCE = new InAppStoryManager(context);
            }
        }
        INSTANCE.createServiceThread(context);
    }

    InAppStoryService service;

    Thread serviceThread;

    private InAppStoryManager(Context context) {
        this.context = context;
        KeyValueStorage.setContext(context);
        SharedPreferencesAPI.setContext(context);
        this.soundOn = !context.getResources().getBoolean(R.bool.defaultMuted);
    }

    void createServiceThread(final Context context) {
        InAppStoryService.useInstance(new UseServiceInstanceCallback() {
            @Override
            public void use(@NonNull InAppStoryService service) throws Exception {
                service.onDestroy();
            }
        });
        if (serviceThread != null) {
            serviceThread.interrupt();
            serviceThread = null;
        }
        serviceThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                service = new InAppStoryService();
                service.onCreate(context, exceptionCache);
                Looper.loop();
            }
        });
        //serviceThread.setUncaughtExceptionHandler(new InAppStoryService.DefaultExceptionHandler());
        serviceThread.start();
    }

    void setExceptionCache(ExceptionCache exceptionCache) {
        this.exceptionCache = exceptionCache;
    }

    private ExceptionCache exceptionCache;

    public void removeFromFavorite(final int storyId) {
        SessionManager.getInstance().useOrOpenSession(new OpenSessionCallback() {
            @Override
            public void onSuccess() {
                favoriteOrRemoveStory(storyId, false);
            }

            @Override
            public void onError() {

            }
        });
    }

    public void removeAllFavorites() {
        SessionManager.getInstance().useOrOpenSession(new OpenSessionCallback() {
            @Override
            public void onSuccess() {
                favoriteRemoveAll();
            }

            @Override
            public void onError() {

            }
        });
    }

    NetworkClient networkClient;

    private void favoriteRemoveAll() {
        if (networkClient == null) return;
        InAppStoryService.useInstance(new UseServiceInstanceCallback() {
            @Override
            public void use(@NonNull final InAppStoryService service) throws Exception {
                final String favUID = ProfilingManager.getInstance().addTask("api_favorite_remove_all");
                networkClient.enqueue(
                        networkClient.getApi().removeAllFavorites(),
                        new NetworkCallback<Response>() {
                            @Override
                            public void onSuccess(Response response) {
                                ProfilingManager.getInstance().setReady(favUID);
                                service.getDownloadManager()
                                        .clearAllFavoriteStatus(Story.StoryType.COMMON);
                                service.getDownloadManager()
                                        .clearAllFavoriteStatus(Story.StoryType.UGC);
                                service.getFavoriteImages().clear();
                                service.getListReaderConnector().clearAllFavorites();

                                if (ScreensManager.getInstance().currentStoriesReaderScreen != null) {
                                    ScreensManager.getInstance().currentStoriesReaderScreen.removeAllStoriesFromFavorite();
                                }
                            }

                            @Override
                            public void onError(int code, String message) {
                                ProfilingManager.getInstance().setReady(favUID);
                                super.onError(code, message);
                            }

                            @Override
                            public void timeoutError() {
                                super.timeoutError();
                                ProfilingManager.getInstance().setReady(favUID);
                            }

                            @Override
                            public Type getType() {
                                return null;
                            }
                        });
            }
        });

    }


    private void favoriteOrRemoveStory(final int storyId, final boolean favorite) {
        if (networkClient == null) return;
        InAppStoryService.useInstance(new UseServiceInstanceCallback() {
            @Override
            public void use(@NonNull final InAppStoryService service) throws Exception {
                final String favUID = ProfilingManager.getInstance().addTask("api_favorite");
                networkClient.enqueue(
                        networkClient.getApi().storyFavorite(Integer.toString(storyId), favorite ? 1 : 0),
                        new NetworkCallback<Response>() {
                            @Override
                            public void onSuccess(Response response) {
                                ProfilingManager.getInstance().setReady(favUID);
                                Story story = service.getDownloadManager()
                                        .getStoryById(storyId, Story.StoryType.COMMON);
                                if (story != null)
                                    story.favorite = favorite;
                                service.getListReaderConnector().storyFavorite(storyId, favorite);
                                if (ScreensManager.getInstance().currentStoriesReaderScreen != null) {
                                    ScreensManager.getInstance().currentStoriesReaderScreen.removeStoryFromFavorite(storyId);
                                }
                            }

                            @Override
                            public void onError(int code, String message) {
                                ProfilingManager.getInstance().setReady(favUID);
                                super.onError(code, message);
                            }

                            @Override
                            public void timeoutError() {
                                super.timeoutError();
                                ProfilingManager.getInstance().setReady(favUID);
                            }

                            @Override
                            public Type getType() {
                                return null;
                            }
                        });
            }
        });

    }

    private boolean isSandbox = false;

    public final static String IAS_ERROR_TAG = "InAppStory_SDK_error";

    private String getErrorStringFromContext(Context context, @StringRes int resourceId) {
        if (context != null)
            return context.getResources().getString(resourceId);
        return "";
    }

    private void build(final Builder builder) {
        Context context = this.context;
        if (context == null) {
            showELog(IAS_ERROR_TAG, "InAppStoryManager data is not valid. 'context' can't be null");
            return;
        }
        if (builder.apiKey == null &&
                context.getResources().getString(R.string.csApiKey).isEmpty()) {
            showELog(IAS_ERROR_TAG, getErrorStringFromContext(context,
                    R.string.ias_api_key_error));
            return;
        }
        if (getBytesLength(builder.userId) > 255) {
            showELog(IAS_ERROR_TAG, getErrorStringFromContext(context,
                    R.string.ias_builder_user_length_error));
            return;
        }
        if (builder.tags != null && getBytesLength(TextUtils.join(",", builder.tags)) > TAG_LIMIT) {
            showELog(IAS_ERROR_TAG, getErrorStringFromContext(context,
                    R.string.ias_builder_tags_length_error));
            return;
        }
        long freeSpace = context.getCacheDir().getFreeSpace();
        if (freeSpace < MB_5 + MB_10 + MB_10) {
            showELog(IAS_ERROR_TAG, getErrorStringFromContext(context,
                    R.string.ias_min_free_space_error));
            return;
        }
        InAppStoryService.useInstance(new UseServiceInstanceCallback() {
            @Override
            public void use(@NonNull InAppStoryService service) throws Exception {

            }
        });
        InAppStoryService inAppStoryService = InAppStoryService.getInstance();
        if (inAppStoryService != null) {
            inAppStoryService.setUserId(builder.userId);
            long commonCacheSize = MB_100;
            long fastCacheSize = MB_10;
            switch (builder.cacheSize) {
                case CacheSize.SMALL:
                    fastCacheSize = MB_5;
                    commonCacheSize = MB_10;
                    break;
                case CacheSize.LARGE:
                    commonCacheSize = MB_200;
                    break;
            }
            inAppStoryService.getFastCache().setCacheSize(fastCacheSize);
            inAppStoryService.getCommonCache().setCacheSize(commonCacheSize);
        }
        String domain = new HostFromSecretKey(
                builder.apiKey
        ).get(builder.sandbox);

        this.isSandbox = builder.sandbox;
        initManager(
                context,
                domain,
                builder.apiKey != null ? builder.apiKey : context.getResources().getString(R.string.csApiKey),
                builder.testKey != null ? builder.testKey : null,
                builder.userId,
                builder.tags != null ? builder.tags : null,
                builder.placeholders != null ? builder.placeholders : null,
                builder.imagePlaceholders != null ? builder.imagePlaceholders : null
        );
        new ExceptionManager().sendSavedException();
    }

    private int getBytesLength(String value) {
        if (value == null) return 0;
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    private void setUserIdInner(String userId) {
        InAppStoryService inAppStoryService = InAppStoryService.getInstance();
        if (inAppStoryService == null) return;
        if (userId == null || getBytesLength(userId) > 255) {
            showELog(IAS_ERROR_TAG, getErrorStringFromContext(context, R.string.ias_setter_user_length_error));
            return;
        }
        if (userId.equals(this.userId)) return;
        localOpensKey = null;
        String oldUserId = this.userId;
        this.userId = userId;
        if (inAppStoryService.getFavoriteImages() != null)
            inAppStoryService.getFavoriteImages().clear();
        inAppStoryService.getDownloadManager().refreshLocals(Story.StoryType.COMMON);
        inAppStoryService.getDownloadManager().refreshLocals(Story.StoryType.UGC);
        closeStoryReader(CloseStory.AUTO);
        SessionManager.getInstance().closeSession(sendStatistic, true, oldUserId);
        OldStatisticManager.getInstance().eventCount = 0;
        inAppStoryService.getDownloadManager().cleanTasks(false);
        inAppStoryService.setUserId(userId);
    }


    //Test

    /**
     * use to change user id in runtime
     *
     * @param userId (userId) - can't be longer than 255 characters
     */
    public void setUserId(@NonNull String userId) {
        setUserIdInner(userId);
    }

    private String userId;

    public String getUserId() {
        return userId;
    }

    public void clearCachedList(String id) {
        if (id == null) return;
        InAppStoryService inAppStoryService = InAppStoryService.getInstance();
        if (inAppStoryService != null) {
            inAppStoryService.listStoriesIds.remove(id);
        }
    }

    public IOpenStoriesReader getOpenStoriesReader() {
        return openStoriesReader;
    }

    public void setOpenStoriesReader(IOpenStoriesReader openStoriesReader) {
        this.openStoriesReader = openStoriesReader;
    }

    private IOpenStoriesReader openStoriesReader = new DefaultOpenStoriesReader();

    public IOpenGameReader getOpenGameReader() {
        return openGameReader;
    }

    public void setOpenGameReader(IOpenGameReader openGameReader) {
        this.openGameReader = openGameReader;
    }

    private IOpenGameReader openGameReader = new DefaultOpenGameReader();

    public void clearCachedLists() {
        InAppStoryService inAppStoryService = InAppStoryService.getInstance();
        if (inAppStoryService != null) {
            inAppStoryService.listStoriesIds.clear();
        }
    }

    public void setActionBarColor(int actionBarColor) {
        this.actionBarColor = actionBarColor;
    }

    public int actionBarColor = -1;

    public boolean isSendStatistic() {
        return sendStatistic;
    }

    private boolean sendStatistic = true;

    public boolean isGameDemoMode() {
        return gameDemoMode;
    }

    private boolean gameDemoMode = false;

    private void initManager(Context context,
                             String cmsUrl,
                             String apiKey,
                             String testKey,
                             String userId,
                             ArrayList<String> tags,
                             Map<String, String> placeholders,
                             Map<String, ImagePlaceholderValue> imagePlaceholders) {
        this.context = context;
        soundOn = !context.getResources().getBoolean(R.bool.defaultMuted);

        synchronized (tagsLock) {
            this.tags = tags;
        }
        if (placeholders != null)
            setPlaceholders(placeholders);
        if (imagePlaceholders != null)
            setImagePlaceholders(imagePlaceholders);
        this.API_KEY = apiKey;
        this.TEST_KEY = testKey;
        this.userId = userId;
        if (!isNull()) {
            localHandler.removeCallbacksAndMessages(null);
            localDestroy();
        }

        OldStatisticManager.getInstance().statistic = new ArrayList<>();
        setInstance(this);
        if (ApiSettings.getInstance().hostIsDifferent(cmsUrl)) {
            if (networkClient != null) {
                networkClient.clear();
                networkClient = null;
            }
        }
        ApiSettings
                .getInstance()
                .cacheDirPath(context.getCacheDir().getAbsolutePath())
                .apiKey(this.API_KEY)
                .testKey(this.TEST_KEY)
                .host(cmsUrl);

        networkClient = new NetworkClient(context, cmsUrl);
    }


    private static final Object lock = new Object();

    public static void logout() {
        InAppStoryService.useInstance(new UseServiceInstanceCallback() {
            @Override
            public void use(@NonNull InAppStoryService inAppStoryService) throws Exception {
                inAppStoryService.listStoriesIds.clear();
                inAppStoryService.getListSubscribers().clear();
                inAppStoryService.getDownloadManager().cleanTasks();
                inAppStoryService.logout();
            }
        });
    }

    @Deprecated
    public static void destroy() {
        logout();
    }

    private static void localDestroy() {
        logout();
    }


    private String localOpensKey;

    public String getLocalOpensKey(Story.StoryType type) {
        if (localOpensKey == null && userId != null) {
            localOpensKey = "opened" + userId;
        }
        return (type == Story.StoryType.COMMON) ? localOpensKey : type.name() + localOpensKey;
    }

    public String getLocalOpensKey() {
        return getLocalOpensKey(Story.StoryType.COMMON);
    }

    /**
     * @return current instance of {@link InAppStoryManager}
     */
    public static InAppStoryManager getInstance() {
        synchronized (lock) {
            return INSTANCE;
        }
    }

    /**
     * @return {@link Pair} with version name in first argument and version code in second
     */
    public static Pair<String, Integer> getLibraryVersion() {
        return new Pair<>(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
    }

    private boolean soundOn = false;

    public void soundOn(boolean isSoundOn) {
        this.soundOn = isSoundOn;
    }

    public boolean soundOn() {
        return soundOn;
    }

    private Handler localHandler = new Handler();
    private Object handlerToken = new Object();

    private void showLoadedOnboardings(
            final List<Story> response,
            final Context outerContext,
            final AppearanceManager manager,
            final String feed
    ) {
        Story.StoryType storyType = Story.StoryType.COMMON;
        if (response == null || response.size() == 0) {
            if (CallbackManager.getInstance().getOnboardingLoadCallback() != null) {
                CallbackManager.getInstance().getOnboardingLoadCallback().onboardingLoad(0, StringsUtils.getNonNull(feed));
            }
            return;
        }

        InAppStoryService inAppStoryService = InAppStoryService.getInstance();
        if (inAppStoryService == null) return;
        if (ScreensManager.getInstance().currentStoriesReaderScreen != null) {
            InAppStoryManager.closeStoryReader(CloseStory.AUTO);
            localHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showLoadedOnboardings(response, outerContext, manager, feed);
                    ScreensManager.created = 0;
                }
            }, 500);
            return;
        }

        ArrayList<Story> stories = new ArrayList<Story>();
        ArrayList<Integer> storiesIds = new ArrayList<>();
        stories.addAll(response);
        for (Story story : response) {
            storiesIds.add(story.id);
        }
        inAppStoryService.getDownloadManager().uploadingAdditional(stories, storyType);
        StoriesReaderLaunchData launchData = new StoriesReaderLaunchData(
                null,
                feed,
                storiesIds,
                0,
                ShowStory.ACTION_OPEN,
                SourceType.ONBOARDING,
                0,
                Story.StoryType.COMMON,
                null
        );
        ScreensManager.getInstance().openStoriesReader(
                outerContext,
                manager,
                launchData
        );
        if (CallbackManager.getInstance().getOnboardingLoadCallback() != null) {
            CallbackManager.getInstance().getOnboardingLoadCallback().onboardingLoad(response.size(), StringsUtils.getNonNull(feed));
        }
    }


    public void getStackFeed(
            final String feed,
            final String uniqueStackId,
            final List<String> tags,
            final AppearanceManager appearanceManager,
            final IStackFeedResult stackFeedResult
    ) {
        if (tags != null && getBytesLength(TextUtils.join(",", tags)) > TAG_LIMIT) {
            showELog(IAS_ERROR_TAG, getErrorStringFromContext(context, R.string.ias_setter_tags_length_error));
            stackFeedResult.error();
            return;
        }
        final String localFeed;
        if (feed != null && !feed.isEmpty()) localFeed = feed;
        else localFeed = "default";
        final String localUniqueStackId = (uniqueStackId != null) ? uniqueStackId : localFeed;
        final AppearanceManager localAppearanceManager =
                appearanceManager != null ? appearanceManager
                        : AppearanceManager.getCommonInstance();
        InAppStoryService service = InAppStoryService.getInstance();
        if (service == null) {
            localHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    getStackFeed(feed, uniqueStackId, tags, appearanceManager, stackFeedResult);
                }
            }, 1000);
            return;
        }
        if (networkClient == null) {
            stackFeedResult.error();
            return;
        }
        SessionManager.getInstance().useOrOpenSession(new OpenSessionCallback() {
            @Override
            public void onSuccess() {
                String localTags = null;
                if (tags != null) {
                    localTags = TextUtils.join(",", tags);
                } else if (getTags() != null) {
                    localTags = TextUtils.join(",", getTags());
                }
                networkClient.enqueue(
                        networkClient.getApi().getFeed(
                                localFeed,
                                ApiSettings.getInstance().getTestKey(),
                                0,
                                localTags == null ? getTagsString() : localTags,
                                null,
                                null//"feed_info"
                        ),
                        new LoadFeedCallback() {
                            @Override
                            public void onSuccess(final Feed response) {
                                if (response == null || response.stories == null) {
                                    stackFeedResult.error();
                                } else {
                                    InAppStoryService.useInstance(new UseServiceInstanceCallback() {
                                        @Override
                                        public void use(@NonNull InAppStoryService service) throws Exception {
                                            service.getDownloadManager().uploadingAdditional(
                                                    response.stories,
                                                    Story.StoryType.COMMON
                                            );
                                        }
                                    });
                                    final StackStoryObserver observer = new StackStoryObserver(
                                            response.stories,
                                            localAppearanceManager,
                                            localUniqueStackId,
                                            localFeed,
                                            new StackStoryUpdatedCallback() {
                                                @Override
                                                public void onUpdate(IStackStoryData newStackStoryData) {
                                                    stackFeedResult.update(newStackStoryData);
                                                }
                                            }
                                    );

                                    final IStackFeedActions stackFeedActions = new IStackFeedActions() {
                                        @Override
                                        public void openReader(Context context) {
                                            observer.openReader(context);
                                        }

                                        @Override
                                        public void unsubscribe() {
                                            observer.unsubscribe();
                                        }
                                    };
                                    if (response.stories.size() == 0) {
                                        stackFeedResult.success(null, stackFeedActions);
                                        return;
                                    }
                                    final Runnable loadObserver = new Runnable() {
                                        @Override
                                        public void run() {
                                            observer.subscribe();
                                            observer.onLoad(new StackStoryUpdatedCallback() {
                                                @Override
                                                public void onUpdate(IStackStoryData newStackStoryData) {
                                                    stackFeedResult.success(
                                                            newStackStoryData,
                                                            stackFeedActions
                                                    );
                                                }
                                            });
                                        }
                                    };
                                    if (response.feedCover != null) {
                                        Downloader.downloadFileAndSendToInterface(response.feedCover, new RunnableCallback() {
                                            @Override
                                            public void run(String coverPath) {
                                                observer.feedCover = coverPath;
                                                loadObserver.run();
                                            }

                                            @Override
                                            public void error() {
                                                loadObserver.run();
                                            }
                                        });
                                    } else {
                                        loadObserver.run();
                                    }
                                }
                            }

                            @Override
                            public void onError(int code, String message) {
                                stackFeedResult.error();
                            }
                        }
                );
            }

            @Override
            public void onError() {
                stackFeedResult.error();
            }
        });
    }

    private void showOnboardingStoriesInner(final Integer limit, final String feed, final List<String> tags, final Context outerContext, final AppearanceManager manager) {
        InAppStoryService service = InAppStoryService.getInstance();
        if (service == null) {
            localHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showOnboardingStoriesInner(limit, feed, tags, outerContext, manager);
                }
            }, 1000);
            return;
        }

        if (tags != null && getBytesLength(TextUtils.join(",", tags)) > TAG_LIMIT) {
            showELog(IAS_ERROR_TAG, getErrorStringFromContext(context, R.string.ias_setter_tags_length_error));
            return;
        }
        SessionManager.getInstance().useOrOpenSession(new OpenSessionCallback() {
            @Override
            public void onSuccess() {
                String localTags = null;
                if (tags != null) {
                    localTags = TextUtils.join(",", tags);
                } else if (getTags() != null) {
                    localTags = TextUtils.join(",", getTags());
                }

                final String onboardUID =
                        ProfilingManager.getInstance().addTask("api_onboarding");
                final String localFeed;
                if (feed != null) localFeed = feed;
                else localFeed = ONBOARDING_FEED;
                networkClient.enqueue(
                        networkClient.getApi().getOnboardingFeed(
                                localFeed,
                                limit,
                                localTags == null ? getTagsString() : localTags
                        ),
                        new LoadFeedCallback() {
                            @Override
                            public void onSuccess(Feed response) {
                                InAppStoryManager inAppStoryManager = InAppStoryManager.getInstance();
                                if (inAppStoryManager == null) return;
                                ProfilingManager.getInstance().setReady(onboardUID);
                                List<Story> notOpened = new ArrayList<>();
                                Set<String> opens = SharedPreferencesAPI.getStringSet(
                                        inAppStoryManager.getLocalOpensKey()
                                );
                                if (opens == null) opens = new HashSet<>();
                                if (response.stories != null) {
                                    for (Story story : response.stories) {
                                        boolean add = true;
                                        for (String opened : opens) {
                                            if (Integer.toString(story.id).equals(opened)) {
                                                add = false;
                                            }
                                        }
                                        if (add) notOpened.add(story);
                                    }
                                }
                                showLoadedOnboardings(notOpened, outerContext, manager, localFeed);
                            }

                            @Override
                            public void onError(int code, String message) {
                                ProfilingManager.getInstance().setReady(onboardUID);
                                loadOnboardingError(localFeed);
                            }

                            @Override
                            public void timeoutError() {
                                ProfilingManager.getInstance().setReady(onboardUID);
                                loadOnboardingError(localFeed);
                            }
                        });
            }

            @Override
            public void onError() {
                loadOnboardingError(feed);
            }

        });
    }

    private void loadOnboardingError(String feed) {
        if (CallbackManager.getInstance().getErrorCallback() != null) {
            CallbackManager.getInstance().getErrorCallback().loadOnboardingError(StringsUtils.getNonNull(feed));
        }
    }


    /**
     * Function for loading onboarding stories with custom tags
     *
     * @param tags         (tags)
     * @param outerContext (outerContext) any type of context (preferably - same as for {@link InAppStoryManager}
     * @param manager      (manager) {@link AppearanceManager} for reader. May be null
     */
    public void showOnboardingStories(String feed, List<String> tags, Context outerContext, AppearanceManager manager) {
        if (feed == null || feed.isEmpty()) feed = ONBOARDING_FEED;
        showOnboardingStoriesInner(null, feed, tags, outerContext, manager);
    }


    /**
     * function for loading onboarding stories with default tags (set in InAppStoryManager.Builder)
     *
     * @param context (context) any type of context (preferably - same as for {@link InAppStoryManager}
     * @param manager (manager) {@link AppearanceManager} for reader. May be null
     */
    public void showOnboardingStories(String feed, Context context, final AppearanceManager manager) {
        if (feed == null || feed.isEmpty()) feed = ONBOARDING_FEED;
        showOnboardingStories(feed, getTags(), context, manager);
    }


    /**
     * Function for loading onboarding stories with custom tags
     *
     * @param tags         (tags)
     * @param outerContext (outerContext) any type of context (preferably - same as for {@link InAppStoryManager}
     * @param manager      (manager) {@link AppearanceManager} for reader. May be null
     */
    public void showOnboardingStories(List<String> tags, Context outerContext, AppearanceManager manager) {
        showOnboardingStoriesInner(null, ONBOARDING_FEED, tags, outerContext, manager);
    }

    /**
     * function for loading onboarding stories with default tags (set in InAppStoryManager.Builder)
     *
     * @param context (context) any type of context (preferably - same as for {@link InAppStoryManager}
     * @param manager (manager) {@link AppearanceManager} for reader. May be null
     */
    public void showOnboardingStories(Context context, final AppearanceManager manager) {
        showOnboardingStories(ONBOARDING_FEED, getTags(), context, manager);
    }

    /**
     * Function for loading onboarding stories with custom tags
     *
     * @param tags         (tags)
     * @param outerContext (outerContext) any type of context (preferably - same as for {@link InAppStoryManager}
     * @param manager      (manager) {@link AppearanceManager} for reader. May be null
     */
    public void showOnboardingStories(int limit, String feed, List<String> tags, Context outerContext, AppearanceManager manager) {
        if (feed == null || feed.isEmpty()) feed = ONBOARDING_FEED;
        showOnboardingStoriesInner(limit, feed, tags, outerContext, manager);
    }


    /**
     * function for loading onboarding stories with default tags (set in InAppStoryManager.Builder)
     *
     * @param context (context) any type of context (preferably - same as for {@link InAppStoryManager}
     * @param manager (manager) {@link AppearanceManager} for reader. May be null
     */
    public void showOnboardingStories(int limit, String feed, Context context, final AppearanceManager manager) {
        if (feed == null || feed.isEmpty()) feed = ONBOARDING_FEED;
        showOnboardingStories(limit, feed, getTags(), context, manager);
    }


    /**
     * Function for loading onboarding stories with custom tags
     *
     * @param tags         (tags)
     * @param outerContext (outerContext) any type of context (preferably - same as for {@link InAppStoryManager}
     * @param manager      (manager) {@link AppearanceManager} for reader. May be null
     */
    public void showOnboardingStories(int limit, List<String> tags, Context outerContext, AppearanceManager manager) {
        showOnboardingStoriesInner(limit, ONBOARDING_FEED, tags, outerContext, manager);
    }

    /**
     * function for loading onboarding stories with default tags (set in InAppStoryManager.Builder)
     *
     * @param context (context) any type of context (preferably - same as for {@link InAppStoryManager}
     * @param manager (manager) {@link AppearanceManager} for reader. May be null
     */
    public void showOnboardingStories(int limit, Context context, final AppearanceManager manager) {
        showOnboardingStories(limit, ONBOARDING_FEED, getTags(), context, manager);
    }

    public boolean isSandbox() {
        return isSandbox;
    }

    private final static String ONBOARDING_FEED = "onboarding";

    private String lastSingleOpen = null;

    private void showStoryInner(final Story story,
                                final Context context,
                                final AppearanceManager manager,
                                final IShowStoryCallback callback,
                                final Integer slide,
                                final Story.StoryType type,
                                final SourceType readerSource,
                                final int readerAction) {
        try {
            int c = Integer.parseInt(lastSingleOpen);
            if (c != story.id)
                return;
        } catch (Exception ignored) {

        }
        if (callback != null)
            callback.onShow();
        InAppStoryService.useInstance(new UseServiceInstanceCallback() {
            @Override
            public void use(@NonNull InAppStoryService service) {
                service.getDownloadManager().putStories(
                        service.getDownloadManager().getStories(Story.StoryType.COMMON),
                        type
                );
            }
        });

        ArrayList<Integer> stIds = new ArrayList<>();
        stIds.add(story.id);
        StoriesReaderLaunchData launchData = new StoriesReaderLaunchData(
                null,
                null,
                stIds,
                0,
                readerAction,
                readerSource,
                slide,
                type,
                null
        );
        ScreensManager.getInstance().openStoriesReader(
                context,
                manager,
                launchData
        );
        localHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                lastSingleOpen = null;
            }
        }, 1000);
    }

    private void showStoryInner(final String storyId,
                                final Context context,
                                final AppearanceManager manager,
                                final IShowStoryCallback callback,
                                final Integer slide,
                                final Story.StoryType type,
                                final SourceType readerSource,
                                final int readerAction) {
        final InAppStoryService service = InAppStoryService.getInstance();
        if (service == null) {
            localHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showStoryInner(
                            storyId,
                            context,
                            manager,
                            callback,
                            slide,
                            type,
                            readerSource,
                            readerAction
                    );
                }
            }, 1000);
            return;
        }
        if (this.userId == null || getBytesLength(this.userId) > 255) {
            showELog(IAS_ERROR_TAG, getErrorStringFromContext(context, R.string.ias_setter_user_length_error));
            return;
        }
        if (lastSingleOpen != null &&
                lastSingleOpen.equals(storyId)) return;
        lastSingleOpen = storyId;
        service.getDownloadManager().getFullStoryByStringId(
                new GetStoryByIdCallback() {
                    @Override
                    public void getStory(final Story story) {
                        if (story != null) {
                            service.getDownloadManager().addCompletedStoryTask(story,
                                    Story.StoryType.COMMON);
                            if (ScreensManager.getInstance().currentStoriesReaderScreen != null) {
                                InAppStoryManager.closeStoryReader(CloseStory.AUTO);
                                localHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        showStoryInner(
                                                story,
                                                context,
                                                manager,
                                                callback,
                                                slide,
                                                type,
                                                readerSource,
                                                readerAction
                                        );
                                        // StoriesActivity.destroyed = 0;
                                    }
                                }, 500);
                                return;
                            }
                            showStoryInner(
                                    story,
                                    context,
                                    manager,
                                    callback,
                                    slide,
                                    type,
                                    readerSource,
                                    readerAction
                            );
                        } else {
                            if (callback != null)
                                callback.onError();
                            lastSingleOpen = null;
                            return;
                        }
                    }

                    @Override
                    public void loadError(int type) {
                        if (callback != null)
                            callback.onError();
                        lastSingleOpen = null;
                    }

                },
                storyId,
                type,
                readerSource
        );
    }

    private void showStoryInner(final String storyId, final Context context,
                                final AppearanceManager manager,
                                final IShowStoryCallback callback, Story.StoryType type,
                                final SourceType readerSource,
                                final int readerAction) {
        showStoryInner(storyId, context, manager, callback, null, type, readerSource, readerAction);
    }

    /**
     * use to show single story in reader by id
     *
     * @param storyId  (storyId)
     * @param context  (context) any type of context (preferably - same as for {@link InAppStoryManager}
     * @param manager  (manager) {@link AppearanceManager} for reader. May be null
     * @param callback (callback) custom action when story is loaded
     */
    public void showStory(String storyId, Context context, AppearanceManager manager, IShowStoryCallback callback) {
        showStoryInner(
                storyId,
                context,
                manager,
                callback,
                Story.StoryType.COMMON,
                SourceType.SINGLE,
                ShowStory.ACTION_OPEN
        );
    }

    public void showStory(String storyId, Context context, AppearanceManager manager, IShowStoryCallback callback, Integer slide) {
        showStoryInner(
                storyId,
                context,
                manager,
                callback,
                slide,
                Story.StoryType.COMMON,
                SourceType.SINGLE,
                ShowStory.ACTION_OPEN
        );
    }

    /**
     * use to show single story in reader by id
     *
     * @param storyId (storyId)
     * @param context (context) any type of context (preferably - same as for {@link InAppStoryManager}
     * @param manager (manager) {@link AppearanceManager} for reader. May be null
     */
    public void showStory(String storyId, Context context, AppearanceManager manager) {
        showStoryInner(
                storyId,
                context,
                manager,
                null,
                Story.StoryType.COMMON,
                SourceType.SINGLE,
                ShowStory.ACTION_OPEN
        );
    }

    public void showStoryCustom(String storyId, Context context, AppearanceManager manager) {
        showStoryInner(
                storyId,
                context,
                manager,
                null,
                Story.StoryType.COMMON,
                SourceType.SINGLE,
                ShowStory.ACTION_CUSTOM
        );
    }

    public void showStoryWithSlide(
            String storyId,
            Context context,
            Integer slide,
            StoriesReaderAppearanceSettings settings,
            Story.StoryType type,
            final SourceType readerSource,
            final int readerAction
    ) {
        AppearanceManager appearanceManager = new AppearanceManager();
        if (settings != null) {
            appearanceManager.csHasLike(settings.csHasLike());
            appearanceManager.csHasFavorite(settings.csHasFavorite());
            appearanceManager.csHasShare(settings.csHasShare());
            appearanceManager.csClosePosition(settings.csClosePosition());
            appearanceManager.csCloseOnOverscroll(settings.csCloseOnOverscroll());
            appearanceManager.csCloseOnSwipe(settings.csCloseOnSwipe());
            appearanceManager.csIsDraggable(settings.csIsDraggable());
            appearanceManager.csTimerGradientEnable(settings.csTimerGradientEnable());
            appearanceManager.csStoryReaderAnimation(settings.csStoryReaderAnimation());
            appearanceManager.csCloseIcon(settings.csCloseIcon());
            appearanceManager.csDislikeIcon(settings.csDislikeIcon());
            appearanceManager.csLikeIcon(settings.csLikeIcon());
            appearanceManager.csRefreshIcon(settings.csRefreshIcon());
            appearanceManager.csFavoriteIcon(settings.csFavoriteIcon());
            appearanceManager.csShareIcon(settings.csShareIcon());
            appearanceManager.csSoundIcon(settings.csSoundIcon());
            appearanceManager.csStoryReaderPresentationStyle(
                    settings.csStoryReaderPresentationStyle()
            );
        }
        showStoryInner(storyId, context, appearanceManager,
                null, slide, type, readerSource, readerAction);
    }

    public static class Builder {

        public boolean sandbox() {
            return sandbox;
        }

        public String userId() {
            return userId;
        }

        public String apiKey() {
            return apiKey;
        }

        public String testKey() {
            return testKey;
        }

        public ArrayList<String> tags() {
            return tags;
        }

        public int getCacheSize() {
            return cacheSize;
        }

        public Map<String, String> placeholders() {
            return placeholders;
        }

        boolean sandbox;

        int cacheSize;
        String userId;
        String apiKey;
        String testKey;
        ArrayList<String> tags;
        Map<String, String> placeholders;
        Map<String, ImagePlaceholderValue> imagePlaceholders;

        public Builder() {
        }

        @Deprecated
        public Builder sandbox(boolean sandbox) {
            Builder.this.sandbox = sandbox;
            return Builder.this;
        }

        /**
         * use to set available space for file caching (slide images, videos, games, etc.)
         *
         * @param cacheSize (cacheSize) - size of available space for cache. Can be set with {@link CacheSize} constants
         *                  {@link com.inappstory.sdk.lrudiskcache.CacheSize#SMALL} - 10mb for stories, 5mb fo story covers
         *                  {@link com.inappstory.sdk.lrudiskcache.CacheSize#MEDIUM} - (by default) 100mb for stories, 10mb fo story covers
         *                  {@link com.inappstory.sdk.lrudiskcache.CacheSize#LARGE} -  200mb for stories, 10mb fo story covers
         * @return {@link Builder}
         */
        public Builder cacheSize(int cacheSize) {
            Builder.this.cacheSize = cacheSize;
            return Builder.this;
        }

        /**
         * use to set api key in runtime (or as alternate to csApiKey string constant)
         *
         * @param apiKey (apiKey) value for api key
         *               false by default
         * @return {@link Builder}
         */
        public Builder apiKey(String apiKey) {
            Builder.this.apiKey = apiKey;
            return Builder.this;
        }

        public Builder testKey(String testKey) {
            Builder.this.testKey = testKey;
            return Builder.this;
        }

        /**
         * use to set user id.
         *
         * @param userId (userId) value for user id. Can't be longer than 255 characters.
         * @return {@link Builder}
         */
        public Builder userId(@NonNull String userId) {
            Builder.this.userId = userId;
            return Builder.this;
        }

        /**
         * @param tags (tags) tags for targeting stories
         * @return {@link Builder}
         */
        public Builder tags(String... tags) {
            Builder.this.tags = new ArrayList<>();
            for (int i = 0; i < tags.length; i++) {
                Builder.this.tags.add(tags[i]);
            }
            return Builder.this;
        }

        /**
         * @param tags (tags) tags for targeting stories
         * @return {@link Builder}
         */
        public Builder tags(ArrayList<String> tags) {
            Builder.this.tags = tags;
            return Builder.this;
        }

        /**
         * @param placeholders (placeholders) placeholders for default values in stories
         * @return {@link Builder}
         */
        public Builder placeholders(Map<String, String> placeholders) {
            Builder.this.placeholders = placeholders;
            return Builder.this;
        }

        /**
         * @param placeholders (placeholders) - placeholders for default values in stories
         * @return {@link Builder}
         */
        public Builder imagePlaceholders(Map<String, ImagePlaceholderValue> placeholders) {
            Builder.this.imagePlaceholders = placeholders;
            return Builder.this;
        }

        /**
         * main method to create {@link InAppStoryManager} instance.
         *
         * @return {@link InAppStoryManager}
         */
        public InAppStoryManager create() {
            synchronized (lock) {
                if (INSTANCE == null) {
                    showELog(IAS_ERROR_TAG, "Method InAppStoryManager.init must be called from Application class");
                    return null;
                }
            }
            INSTANCE.build(Builder.this);
            return INSTANCE;
        }
    }
}
