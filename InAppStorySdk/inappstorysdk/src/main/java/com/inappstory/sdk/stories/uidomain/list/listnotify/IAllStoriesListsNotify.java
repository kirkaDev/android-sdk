package com.inappstory.sdk.stories.uidomain.list.listnotify;

import com.inappstory.sdk.stories.api.models.Story;
import com.inappstory.sdk.stories.api.models.Story.StoryType;
import com.inappstory.sdk.stories.ui.list.FavoriteImage;
import com.inappstory.sdk.stories.ui.list.adapters.IStoriesListAdapter;
import com.inappstory.sdk.core.repository.stories.dto.PreviewStoryDTO;

import java.util.List;

public interface IAllStoriesListsNotify {
    void unsubscribe();

    void subscribe();

    void bindListAdapter(IStoriesListAdapter storiesListAdapter);

    void openStory(
            final int storyId,
            StoryType storyType
    );

    void changeUserId();

    void clearAllFavorites();

    void storyFavoriteCellNotify(
            final List<FavoriteImage> favoriteImages,
            Story.StoryType storyType,
            final boolean isEmpty
    );

    void storyAddToFavoriteItemNotify(final PreviewStoryDTO data);

    void storyRemoveFromFavoriteItemNotify(final int storyId);
}
