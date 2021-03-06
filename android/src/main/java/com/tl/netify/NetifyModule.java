package com.tl.netify;

import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANRequest;
import com.androidnetworking.common.Method;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.androidnetworking.utils.ParseUtil;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.google.gson.Gson;
import com.google.gson.internal.ObjectConstructor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NetifyModule extends ReactContextBaseJavaModule {
    private final ReactApplicationContext reactContext;

    public NetifyModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "Netify";
    }

    @ReactMethod
    public void init(ReadableMap params) {
        int timeout = params.hasKey("timeout") ? params.getInt("timeout") : 60000;
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .callTimeout(timeout / 1000, TimeUnit.SECONDS)
                .build();
        AndroidNetworking.initialize(reactContext.getApplicationContext(), okHttpClient);
    }

    @ReactMethod
    public void jsonRequest(ReadableMap params, final Promise promise) {
        String url = params.getString("url");
        String methodString = params.hasKey("method") ? params.getString("method") : "get";
        int method = methodMapper(methodString);
        ReadableMap headersMap = params.hasKey("headers") ? params.getMap("headers") : null;
        Map<String, String> headers = convertMap(headersMap);
        String body = params.hasKey("body") ? params.getString("body") : null;
        AndroidNetworking.request(url, method)
                .addHeaders(headers)
                .addStringBody(body)
                .setContentType("application/json; charset=utf-8")
                .build()
                .getAsJSONObject(new NetifyJSONObjectRequestListener(promise));
    }

    private int methodMapper(String method) {
        if (method.equals("get")) return Method.GET;
        if (method.equals("post")) return Method.POST;
        if (method.equals("put")) return Method.PUT;
        if (method.equals("patch")) return Method.PATCH;
        if (method.equals("delete")) return Method.DELETE;
        return Method.GET;
    }

    private static class NetifyJSONObjectRequestListener implements JSONObjectRequestListener {
        private final Promise promise;

        public NetifyJSONObjectRequestListener(Promise promise) {
            this.promise = promise;
        }

        @Override
        public void onResponse(JSONObject response) {
            try {
                promise.resolve(convertJsonToMap(response));
            } catch (JSONException exception) {
                promise.reject(exception);
            }
        }

        @Override
        public void onError(ANError anError) {
            WritableMap userInfo = Arguments.createMap();
            WritableMap headers = Arguments.createMap();
            WritableMap response = null;
            String code = anError.getErrorDetail();
            String message = anError.getErrorDetail();
            if (anError.getCause() != null) {
                message = anError.getCause().getMessage();
            }
            if (message == null) {
                message = "Unknown error";
            }
            if (message.startsWith("java.net.SocketTimeoutException")) {
                code = "timeout";
            } else if (message.startsWith("java.net.ConnectException")) {
                code = "network_error";
            }
            if (anError.getErrorCode() > 0) {
                response = Arguments.createMap();
                response.putInt("status", anError.getErrorCode());
            }
            if (anError.getResponse() != null) {
                Headers responseHeaders = anError.getResponse().headers();
                for (String headerName: responseHeaders.names()) {
                    headers.putString(headerName, responseHeaders.get(headerName));
                }
                if (response == null) {
                    response = Arguments.createMap();
                }
                response.putMap("headers", headers);
            }

            if (anError.getErrorBody() != null) {
                if (response == null) {
                    response = Arguments.createMap();
                }
                try {
                    JSONObject obj = new JSONObject(anError.getErrorBody());
                    WritableMap data = convertJsonToMap(obj);
                    response.putMap("data", data);
                } catch (Throwable t) {
                    Log.e("Netify", "Could not parse malformed JSON: \"" + anError.getErrorBody() + "\"");
                    response.putString("data", anError.getErrorBody());
                }
            }
            if (response != null) {
                userInfo.putMap("response", response);
            }
            promise.reject(code, message, anError, userInfo);
        }
    }

    private static Map<String, String> convertMap(ReadableMap readableMap) {
        Map<String, String> map = new HashMap<>();
        if (readableMap != null) {
            Iterator<Map.Entry<String, Object>> it = readableMap.getEntryIterator();
            while (it.hasNext()) {
                Map.Entry<String, Object> pair = it.next();
                Object value = pair.getValue();
                map.put(pair.getKey(), value.toString());
            }
        }
        return map;
    }

    private static WritableMap convertJsonToMap(JSONObject jsonObject) throws JSONException {
        WritableMap map = new WritableNativeMap();

        Iterator<String> iterator = jsonObject.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                map.putMap(key, convertJsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                map.putArray(key, convertJsonToArray((JSONArray) value));
            } else if (value instanceof Boolean) {
                map.putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                map.putInt(key, (Integer) value);
            } else if (value instanceof Double) {
                map.putDouble(key, (Double) value);
            } else if (value instanceof Float) {
                map.putDouble(key, ((Float) value).doubleValue());
            } else if (value instanceof Long) {
                map.putDouble(key, ((Long) value).doubleValue());
            } else if (value instanceof String) {
                map.putString(key, (String) value);
            } else {
                map.putString(key, value.toString());
            }
        }
        return map;
    }

    private static WritableArray convertJsonToArray(JSONArray jsonArray) throws JSONException {
        WritableArray array = new WritableNativeArray();

        for (int i = 0; i < jsonArray.length(); i++) {
            Object value = jsonArray.get(i);
            if (value instanceof JSONObject) {
                array.pushMap(convertJsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                array.pushArray(convertJsonToArray((JSONArray) value));
            } else if (value instanceof Boolean) {
                array.pushBoolean((Boolean) value);
            } else if (value instanceof Integer) {
                array.pushInt((Integer) value);
            } else if (value instanceof Double) {
                array.pushDouble((Double) value);
            } else if (value instanceof Float) {
                array.pushDouble(((Float) value).doubleValue());
            } else if (value instanceof Long) {
                array.pushDouble(((Long) value).doubleValue());
            } else if (value instanceof String) {
                array.pushString((String) value);
            } else {
                array.pushString(value.toString());
            }
        }
        return array;
    }

    private static JSONObject convertMapToJson(ReadableMap readableMap) throws JSONException {
        JSONObject object = new JSONObject();
        ReadableMapKeySetIterator iterator = readableMap.keySetIterator();
        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            switch (readableMap.getType(key)) {
                case Null:
                    object.put(key, JSONObject.NULL);
                    break;
                case Boolean:
                    object.put(key, readableMap.getBoolean(key));
                    break;
                case Number:
                    object.put(key, readableMap.getDouble(key));
                    break;
                case String:
                    object.put(key, readableMap.getString(key));
                    break;
                case Map:
                    object.put(key, convertMapToJson(readableMap.getMap(key)));
                    break;
                case Array:
                    object.put(key, convertArrayToJson(readableMap.getArray(key)));
                    break;
            }
        }
        return object;
    }

    private static JSONArray convertArrayToJson(ReadableArray readableArray) throws JSONException {
        JSONArray array = new JSONArray();
        for (int i = 0; i < readableArray.size(); i++) {
            switch (readableArray.getType(i)) {
                case Null:
                    break;
                case Boolean:
                    array.put(readableArray.getBoolean(i));
                    break;
                case Number:
                    array.put(readableArray.getDouble(i));
                    break;
                case String:
                    array.put(readableArray.getString(i));
                    break;
                case Map:
                    array.put(convertMapToJson(readableArray.getMap(i)));
                    break;
                case Array:
                    array.put(convertArrayToJson(readableArray.getArray(i)));
                    break;
            }
        }
        return array;
    }
}
