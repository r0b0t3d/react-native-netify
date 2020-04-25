package com.tl.netify;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class NetifyModule extends ReactContextBaseJavaModule {
    private final ReactApplicationContext reactContext;

    public NetifyModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        initializeNetworking();
    }

    @Override
    public String getName() {
        return "Netify";
    }

    @ReactMethod
    public void jsonRequest(ReadableMap params, final Promise promise) {
        String url = params.getString("url");
        String methodString = params.hasKey("method") ? params.getString("method") : "get";
        NetifyMethod method = NetifyMethod.valueOf(methodString.toUpperCase());
        ReadableMap headersMap = params.getMap("headers");
        Map<String, String> headers = convertMap(headersMap);
        ReadableMap bodyMap = params.hasKey("body") ? params.getMap("body") : null;
        JSONObject body = new JSONObject();
        if (bodyMap != null) {
            try {
                body = convertMapToJson(bodyMap);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        switch (method) {
            case GET:
                AndroidNetworking.get(url)
                        .addHeaders(headers)
                        .build()
                        .getAsJSONObject(new NetifyJSONObjectRequestListener(promise));
                break;
            case POST:
                AndroidNetworking.post(url)
                        .addHeaders(headers)
                        .addBodyParameter(body)
                        .build()
                        .getAsJSONObject(new NetifyJSONObjectRequestListener(promise));
                break;
            case PUT:
                AndroidNetworking.put(url)
                        .addHeaders(headers)
                        .addBodyParameter(body)
                        .build()
                        .getAsJSONObject(new NetifyJSONObjectRequestListener(promise));
                break;
            case PATCH:
                AndroidNetworking.patch(url)
                        .addHeaders(headers)
                        .addBodyParameter(body)
                        .build()
                        .getAsJSONObject(new NetifyJSONObjectRequestListener(promise));
                break;
            case DELETE:
                AndroidNetworking.delete(url)
                        .addHeaders(headers)
                        .addBodyParameter(body)
                        .build()
                        .getAsJSONObject(new NetifyJSONObjectRequestListener(promise));
                break;
            default:
                break;

        }
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
            promise.reject(anError);
        }
    }

    private static Map<String, String> convertMap(ReadableMap readableMap) {
        Map<String, String> map = new HashMap<>();
        if (readableMap != null) {
            Iterator<Map.Entry<String, Object>> it = readableMap.getEntryIterator();
            while (it.hasNext()) {
                Map.Entry<String, Object> pair = it.next();
                if (pair.getValue() instanceof String) {
                    map.put(pair.getKey(), (String) pair.getValue());
                }
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

    private void initializeNetworking() {
        AndroidNetworking.initialize(reactContext.getApplicationContext());
    }
}
