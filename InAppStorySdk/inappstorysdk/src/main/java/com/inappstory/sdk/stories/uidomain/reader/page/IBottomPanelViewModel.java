package com.inappstory.sdk.stories.uidomain.reader.page;

import androidx.lifecycle.LiveData;

public interface IBottomPanelViewModel {

    BottomPanelVisibilityState visibilityStateLD();

    LiveData<BottomPanelFavoriteState> favoriteStateLD();

    LiveData<BottomPanelLikeState> likeStateLD();

    LiveData<Boolean> soundOnStateLD();

    LiveData<Boolean> shareEnabledStateLD();

    void setVisibility(
            boolean hasFavorite,
            boolean hasLike,
            boolean hasShare,
            boolean hasSound
    );

    void like(int like);

    void likeEnabled(boolean enabled);

    void favorite(boolean favorite);

    void favoriteEnabled(boolean enabled);

    void shareEnabled(boolean enabled);

    void soundOn(boolean soundOn);
}
