package com.inappstory.sdk.stories.ui.ugclist;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.inappstory.sdk.AppearanceManager;
import com.inappstory.sdk.InAppStoryService;
import com.inappstory.sdk.R;
import com.inappstory.sdk.eventbus.CsEventBus;
import com.inappstory.sdk.stories.api.models.Story;
import com.inappstory.sdk.stories.callbacks.CallbackManager;
import com.inappstory.sdk.stories.callbacks.OnFavoriteItemClick;
import com.inappstory.sdk.stories.events.NoConnectionEvent;
import com.inappstory.sdk.stories.events.StoriesErrorEvent;
import com.inappstory.sdk.stories.outercallbacks.common.reader.ClickAction;
import com.inappstory.sdk.stories.outercallbacks.storieslist.ListCallback;
import com.inappstory.sdk.stories.outerevents.CallToAction;
import com.inappstory.sdk.stories.outerevents.ClickOnStory;
import com.inappstory.sdk.stories.outerevents.ShowStory;
import com.inappstory.sdk.stories.statistic.OldStatisticManager;
import com.inappstory.sdk.stories.statistic.StatisticManager;
import com.inappstory.sdk.stories.ui.ScreensManager;
import com.inappstory.sdk.stories.ui.list.BaseStoryListItem;
import com.inappstory.sdk.stories.ui.list.ClickCallback;
import com.inappstory.sdk.stories.ui.list.StoryFavoriteListItem;
import com.inappstory.sdk.stories.ui.list.StoryListItem;
import com.inappstory.sdk.ugc.list.OnUGCItemClick;
import com.inappstory.sdk.ugc.list.UGCListItem;

import java.util.ArrayList;
import java.util.List;

public class UgcStoriesAdapter extends RecyclerView.Adapter<BaseStoryListItem> implements ClickCallback {
    public List<Integer> getStoriesIds() {
        return storiesIds;
    }

    private List<Integer> storiesIds = new ArrayList<>();
    OnUGCItemClick ugcItemClick;
    ListCallback callback;

    boolean useUGC;

    public Context context;
    private String listID;

    public UgcStoriesAdapter(Context context,
                             String listID,
                             List<Integer> storiesIds,
                             AppearanceManager manager,
                             ListCallback callback,
                             boolean useUGC,
                             OnUGCItemClick ugcItemClick) {
        this.context = context;
        this.listID = listID;
        this.storiesIds = storiesIds;
        this.manager = manager;
        this.ugcItemClick = ugcItemClick;
        this.callback = callback;
        this.useUGC = useUGC;
    }

    public void refresh(List<Integer> storiesIds) {
        this.storiesIds = storiesIds;
    }

    public int getIndexById(int id) {
        if (storiesIds == null) return -1;
        return storiesIds.indexOf(id);
    }


    AppearanceManager manager;

    @NonNull
    @Override
    public BaseStoryListItem onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int vType = viewType % 10;
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cs_story_list_custom_item, parent, false);
        if (vType == -2) {
            return new UGCListItem(v, manager);
        } else
            return new UgcStoryListItem(v, manager, true, false);
    }

    @Override
    public void onBindViewHolder(@NonNull BaseStoryListItem holder, int position) {
        if (holder == null || InAppStoryService.isNull()) return;
        if (holder.isUGC) {
            holder.bindUGC();
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ugcItemClick != null) {
                        ugcItemClick.onClick();
                    }
                }
            });
        } else {
            int hasUGC = useUGC ? 1 : 0;
            final Story story = InAppStoryService.getInstance().getDownloadManager()
                    .getStoryById(storiesIds.get(position - hasUGC), Story.StoryType.UGC);
            if (story == null) return;
            String imgUrl = (story.getImage() != null && story.getImage().size() > 0) ?
                    story.getProperImage(manager.csCoverQuality()).getUrl() : null;
            holder.bind(story.id,
                    story.getTitle(),
                    story.getTitleColor() != null ? Color.parseColor(story.getTitleColor()) : null,
                    story.getSource(),
                    imgUrl,
                    Color.parseColor(story.getBackgroundColor()),
                    true,
                    story.hasAudio(),
                    story.getVideoUrl(), this);
        }
    }

    Long clickTimestamp = -1L;

    @Override
    public void onItemClick(int ind) {
        if (InAppStoryService.isNull()) return;
        if (System.currentTimeMillis() - clickTimestamp < 1500) {
            return;
        }
        int hasUGC = useUGC ? 1 : 0;
        int index = ind - hasUGC;
        clickTimestamp = System.currentTimeMillis();
        Story current = InAppStoryService.getInstance().getDownloadManager()
                .getStoryById(storiesIds.get(index), Story.StoryType.UGC);
        if (current != null) {
            if (callback != null) {
                callback.itemClick(current.id, index, current.title, current.tags,
                        current.getSlidesCount(), false, null);
            }
            if (current.deeplink != null) {
                StatisticManager.getInstance().sendDeeplinkStory(current.id, current.deeplink, null);
                OldStatisticManager.getInstance().addDeeplinkClickStatistic(current.id);
                if (CallbackManager.getInstance().getCallToActionCallback() != null) {
                    CallbackManager.getInstance().getCallToActionCallback().callToAction(
                            current.id, current.title,
                            current.tags, current.getSlidesCount(), 0,
                            current.deeplink, ClickAction.DEEPLINK);
                }
                if (CallbackManager.getInstance().getUrlClickCallback() != null) {
                    CallbackManager.getInstance().getUrlClickCallback().onUrlClick(current.deeplink);
                    current.isOpened = true;
                    current.saveStoryOpened();
                    notifyItemChanged(ind);
                } else {
                    if (!InAppStoryService.isConnected()) {
                        if (CallbackManager.getInstance().getErrorCallback() != null) {
                            CallbackManager.getInstance().getErrorCallback().noConnection();
                        }
                        return;
                    }
                    current.isOpened = true;
                    current.saveStoryOpened();
                    notifyItemChanged(ind);
                    try {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(current.deeplink));
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(i);
                    } catch (Exception ignored) {
                        InAppStoryService.createExceptionLog(ignored);
                    }
                }
                return;
            }
            if (current.isHideInReader()) {
                if (CallbackManager.getInstance().getErrorCallback() != null) {
                    CallbackManager.getInstance().getErrorCallback().emptyLinkError();
                }
                return;
            }
        } else {
            if (callback != null) {
                callback.itemClick(storiesIds.get(index), index, null, null, 0,
                        false, null);
            }
        }
        ArrayList<Integer> tempStories = new ArrayList();
        for (Integer storyId : storiesIds) {
            Story story = InAppStoryService.getInstance().getDownloadManager().getStoryById(storyId, Story.StoryType.UGC);
            if (story == null || !story.isHideInReader())
                tempStories.add(storyId);
        }
        ScreensManager.getInstance().openStoriesReader(
                context, listID,
                manager, tempStories,
                tempStories.indexOf(storiesIds.get(index)), ShowStory.UGC_LIST,
                null, null, Story.StoryType.UGC);
    }

    @Override
    public int getItemViewType(int position) {
        int hasUGC = useUGC ? 1 : 0;
        if (useUGC && position == 0)
            return -2;
        try {
            int pos = position - hasUGC;
            int pref = pos * 10;
            Story story = InAppStoryService.getInstance().getDownloadManager()
                    .getStoryById(storiesIds.get(pos), Story.StoryType.UGC);
            if (story.getVideoUrl() != null) pref += 5;
            return story.isOpened ? (pref + 2) : (pref + 1);
        } catch (Exception e) {
            return 0;
        }

    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return storiesIds.size() +
                ((!storiesIds.isEmpty() && useUGC) ? 1 : 0);
    }
}