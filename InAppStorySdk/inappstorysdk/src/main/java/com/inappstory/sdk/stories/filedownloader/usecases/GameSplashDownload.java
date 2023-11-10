package com.inappstory.sdk.stories.filedownloader.usecases;

import androidx.annotation.NonNull;


import com.inappstory.sdk.core.lrudiskcache.LruDiskCache;
import com.inappstory.sdk.stories.cache.DownloadFileState;
import com.inappstory.sdk.stories.filedownloader.AsyncFileDownload;
import com.inappstory.sdk.stories.filedownloader.FileDownload;
import com.inappstory.sdk.stories.filedownloader.IFileDownloadCallback;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class GameSplashDownload extends AsyncFileDownload {
    public GameSplashDownload(
            @NonNull String url,
            Long checkSize,
            String checkSha1,
            Long needSpace,
            @NonNull LruDiskCache cache,
            @NonNull ExecutorService service
    ) {
        super(url, checkSize, checkSha1, needSpace, cache, service);
    }

    @Override
    public String getCacheKey() {
        return url;
    }

    @Override
    public String getDownloadFilePath() {
        return cache.getFileFromKey(getCacheKey()).getAbsolutePath();
    }

    @Override
    public void onProgress(long currentProgress, long max) {

    }

    @Override
    public boolean isInterrupted() {
        return false;
    }

}
