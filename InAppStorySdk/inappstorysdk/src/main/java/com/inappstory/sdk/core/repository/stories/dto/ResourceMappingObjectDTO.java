package com.inappstory.sdk.core.repository.stories.dto;


import com.inappstory.sdk.core.network.annotations.models.SerializedName;
import com.inappstory.sdk.stories.api.models.ResourceMappingObject;

/**
 * Created by paperrose on 19.02.2018.
 */

public class ResourceMappingObjectDTO {
    public ResourceMappingObjectDTO(
            String url,
            String key,
            String type,
            Integer index
    ) {
        this.url = url;
        this.key = key;
        this.type = type;
        this.index = index;
    }

    public ResourceMappingObjectDTO(
            ResourceMappingObject object
    ) {
        this.url = object.getUrl();
        this.key = object.getKey();
        this.type = object.getType();
        this.index = object.getIndex();
    }

    private String url;
    private String key;
    private String type;
    private Integer index;

    public String getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    public Integer getIndex() {
        return index;
    }

    public String getKey() {
        return key;
    }

    @Override
    public int hashCode() {
        int res = 0;
        if (url != null) {
            res += url.hashCode();
        }
        if (key != null) {
            res += key.hashCode();
        }
        return res;
    }
}
