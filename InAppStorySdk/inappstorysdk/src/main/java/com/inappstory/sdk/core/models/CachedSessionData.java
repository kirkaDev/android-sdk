package com.inappstory.sdk.core.models;

import android.content.Context;

import com.inappstory.sdk.core.utils.network.JsonParser;
import com.inappstory.sdk.core.utils.sharedpref.SharedPreferencesAPI;

import java.util.List;

public class CachedSessionData {
    private static CachedSessionData INSTANCE;

    public static final String CACHED_SESSION_DATA = "cached_session_data";

    public static CachedSessionData getInstance(Context context) {
        if (!SharedPreferencesAPI.hasContext())
            SharedPreferencesAPI.setContext(context);
        if (INSTANCE == null)
            INSTANCE = JsonParser.fromJson(SharedPreferencesAPI.getString(CACHED_SESSION_DATA),
                    CachedSessionData.class);
        return INSTANCE;
    }

    public static void setInstance(CachedSessionData data) {
        INSTANCE = data;
        try {
            SharedPreferencesAPI.saveString(CACHED_SESSION_DATA, JsonParser.getJson(data));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public String userId;
    public float previewAspectRatio;
    public String tags;
    public List<StoryPlaceholder> placeholders;
    public String sessionId;
    public String testKey;
    public String token;
}