package com.inappstory.sdk.stories.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;

import com.inappstory.sdk.InAppStoryManager;

import com.inappstory.sdk.R;

/**
 * Created by Paperrose on 11.07.2018.
 */

public class Sizes {

    public static int getStatusBarHeight(Context context) {
        if (context == null) return 60;
        int result = 0;
        @SuppressLint("InternalInsetResource")
        int resourceId = context.getResources().getIdentifier(
                "status_bar_height",
                "dimen",
                "android"
        );
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        } else {
            result = (int) Math.ceil(
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 24 : 25)
                            * context.getResources().getDisplayMetrics().density);
        }
        return result;
    }

    public static float getPixelScaleFactorExt(Context context) {
        if (context == null)
            return 1;
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return displayMetrics.density;
    }

    public static Point getScreenSize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    public static int dpFloatToPxExt(float dp, Context context) {
        return Math.round(dp * getPixelScaleFactorExt(context));
    }
    public static boolean isTablet(Context context) {
        if (context == null) return false;
        return context.getResources().getBoolean(R.bool.isTablet);
    }

    public static int dpToPxExt(int dp, Context context) {
        return Math.round(dp * getPixelScaleFactorExt(context));
    }

    public static int pxToDpExt(int dp, Context context) {
        return Math.round(dp / getPixelScaleFactorExt(context));
    }


    public static float typedDpToPx(float dpValue, Context context) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dpValue,
                context.getResources().getDisplayMetrics()
        );
    }
}

