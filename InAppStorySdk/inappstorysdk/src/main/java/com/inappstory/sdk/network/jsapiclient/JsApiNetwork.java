package com.inappstory.sdk.network.jsapiclient;

import android.content.Context;

import com.inappstory.sdk.InAppStoryManager;
import com.inappstory.sdk.InAppStoryService;
import com.inappstory.sdk.network.NetworkClient;
import com.inappstory.sdk.network.constants.HttpMethods;
import com.inappstory.sdk.network.models.Request;
import com.inappstory.sdk.network.models.Response;
import com.inappstory.sdk.network.utils.headers.CustomHeader;
import com.inappstory.sdk.network.utils.headers.Header;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.List;
import java.util.Map;

public class JsApiNetwork {

    public static JsApiResponse sendRequest(
            String method,
            String path,
            Map<String, String> headers,
            Map<String, String> getParams,
            String body,
            String requestId,
            Context context
    ) throws Exception {
        final JsApiResponse jsResponse = new JsApiResponse();
        jsResponse.requestId = requestId;

        NetworkClient networkClient = InAppStoryManager.getNetworkClient();
        if (!InAppStoryService.isConnected() || networkClient == null) {
            jsResponse.status = 12163;
            return jsResponse;
        }
        Request.Builder requestBuilder = new Request.Builder();

        boolean hasBody = !method.equals(HttpMethods.GET)
                && !method.equals(HttpMethods.HEAD)
                && body != null
                && !body.isEmpty();
        List<Header> defaultHeaders = networkClient.generateHeaders(
                context,
                new String[]{},
                false,
                hasBody
        );
        for (Map.Entry<String, String> header : headers.entrySet()) {
            if (header.getValue() != null) {
                defaultHeaders.add(new CustomHeader(header.getKey(), header.getValue()));
            }
        }

        Request request = requestBuilder
                .isFormEncoded(false)
                .headers(defaultHeaders)
                .url("v2/" + path)
                .vars(getParams)
                .body(body)
                .build();

        requestBuilder
                .method(method)
                .headers(defaultHeaders)
                .isFormEncoded(false)
                .body(body);
        Response networkResponse = networkClient.execute(request, null);
        jsResponse.status = networkResponse.code;
        if (networkResponse.headers.size() > 0) {
            JSONObject jheaders = new JSONObject();
            try {
                for (Map.Entry<String, String> header : networkResponse.headers.entrySet()) {
                    if (header.getValue() != null) {
                        jheaders.put(header.getKey(), header.getValue());
                    }
                }
                jsResponse.headers = jheaders.toString();
            } catch (JSONException e) {

            }
        }
        jsResponse.data = networkResponse.body != null ?
                networkResponse.body :
                networkResponse.errorBody;
        return jsResponse;
    }
}
