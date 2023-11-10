package com.inappstory.sdk.stories.ui.views;

import com.inappstory.sdk.core.IASCore;

import java.io.File;


/**
 * Name wasn't changed because of UGC SDK usage
 */
public class IASWebViewClient extends BaseWebViewClient {

    @Override
    protected File getCachedFile(String url) {
        String filePath = IASCore.getInstance().filesRepository.getLocalGameResource(url);
        if (filePath != null) return new File(filePath);
        return null;
    }
}