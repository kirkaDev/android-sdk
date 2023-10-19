package com.inappstory.sdk.core.repository.session;

public interface IGetSessionCallback<T> {
    void onSuccess(T session);

    void onError();
}
