package com.inappstory.sdk.imagememcache;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import com.inappstory.sdk.InAppStoryService;
import com.inappstory.sdk.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GetBitmapFromCacheWithFilePath {

    public GetBitmapFromCacheWithFilePath(
            String filePath,
            IGetBitmapFromMemoryCache cacheSuccess,
            IGetBitmapFromMemoryCacheError cacheError
    ) {
        this.cacheSuccess = cacheSuccess;
        this.cacheError = cacheError;
        this.filePath = filePath;
    }

    private String filePath;
    private IGetBitmapFromMemoryCache cacheSuccess;
    private IGetBitmapFromMemoryCacheError cacheError;
    private static final ExecutorService fileSystemThread = Executors.newFixedThreadPool(1);
    private static final BitmapCacheHolder bitmapCacheHolder = new BitmapCacheHolder();

    void get() {
        Bitmap bitmap = bitmapCacheHolder.getBitmapFromCache(filePath);
        if (bitmap != null) {
            cacheSuccess.get(bitmap);
        } else {
            fileSystemThread.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        final Bitmap result = (new GetBitmapFromFilePath(filePath)).call();
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                if (result != null) {
                                    cacheSuccess.get(result);
                                } else {
                                    cacheError.onError();
                                }
                            }
                        });
                    } catch (Exception e) {
                        InAppStoryService.createExceptionLog(e);
                    }
                }
            });
        }
    }
}
