package com.inappstory.sdk.stories.ui.widgets.progresstimeline;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.inappstory.sdk.stories.utils.Sizes;

import java.util.ArrayList;
import java.util.List;

public class Timeline extends LinearLayout {
    public Timeline(Context context) {
        super(context);
        init();
    }

    int slidesCount = -1;

    public Timeline(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Timeline(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private final LayoutParams PROGRESS_BAR_LAYOUT_PARAM = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1);
    private final LayoutParams SPACE_LAYOUT_PARAM = new LayoutParams(Sizes.dpToPxExt(8), LayoutParams.WRAP_CONTENT);

    private TimelineProgressBar createProgressBar() {
        TimelineProgressBar p = new TimelineProgressBar(getContext());
        p.setLayoutParams(PROGRESS_BAR_LAYOUT_PARAM);
        return p;
    }

    private View createSpace() {
        View v = new View(getContext());
        v.setLayoutParams(SPACE_LAYOUT_PARAM);
        return v;
    }

    private void init() {
        setOrientation(LinearLayout.HORIZONTAL);
        bindViews();
    }

    public void setSlidesCount(int slidesCount) {
        this.slidesCount = slidesCount;
        bindViews();
    }

    ValueAnimator curAnimation;
    int activeInd = 0;

    public void setActive(int ind) {
        curAnimation = progressBars.get(ind).animation;
        activeInd = ind;
    }

    List<TimelineProgressBar> progressBars = new ArrayList<>();

    private void bindViews() {
        progressBars.clear();
        removeAllViews();

        for (int i = 0; i < slidesCount; i++) {
            final TimelineProgressBar p = createProgressBar();
            progressBars.add(p);
            addView(p);
            if ((i + 1) < slidesCount) {
                addView(createSpace());
            }
        }
    }
}
