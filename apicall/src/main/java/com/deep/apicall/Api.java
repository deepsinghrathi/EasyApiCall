package com.deep.apicall;

import android.Manifest;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

/** @noinspection unused*/
public class Api {

    private String BASE_URL;
    private String method;
    private ShowProgress showProgress;
    private ShowNoInternet showNoInternet;
    private boolean canShowProgress = true;
    OkHttpClient client;
    Context context;
    String TAG;

    public Api(Context context,String baseUrl){
        File httpCacheDirectory = new File(context.getCacheDir(), "http-cache");
        int cacheSize = 10 * 1024 * 1024; // 10 MiB
        Cache cache = new Cache(httpCacheDirectory, cacheSize);
//        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
//        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        client = new OkHttpClient.Builder()
                .cache(cache)
//                .addInterceptor(loggingInterceptor)
//                .addInterceptor(new GzipRequestInterceptor())
                .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
                .connectTimeout(30, TimeUnit.SECONDS) // Increase connection timeout
                .readTimeout(30, TimeUnit.SECONDS)    // Increase read timeout
                .writeTimeout(30, TimeUnit.SECONDS)// Increase write timeout
                .build();
        this.context = context;
        this.BASE_URL = baseUrl;
        this.TAG = context.getClass().getSimpleName();
        this.showProgress = ShowProgress.init(context);
        this.showNoInternet = ShowNoInternet.init(context);
    }

    public Api(String TAG,String baseUrl) {
//        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
//        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        client = new OkHttpClient.Builder()
//                .addInterceptor(loggingInterceptor)
//                .addInterceptor(new GzipRequestInterceptor())
                .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
                .connectTimeout(30, TimeUnit.SECONDS) // Increase connection timeout
                .readTimeout(30, TimeUnit.SECONDS)    // Increase read timeout
                .writeTimeout(30, TimeUnit.SECONDS)   // Increase write timeout
                .build();
        this.TAG = TAG;
        this.BASE_URL = baseUrl;
    }

    public static Api with(Context context,String baseUrl){
        return new Api(context,baseUrl);
    }

    public static Api with(String tag,String baseUrl){
        return new Api(tag,baseUrl);
    }

    public Api setRequestMethod(RequestMethod requestMethod) {
        switch (requestMethod) {
            case POST -> method = "POST";
            case PUT -> method = "PUT";
            case DELETE -> method = "DELETE";
            case GET -> method = "GET";
        }
        return this;
    }

    private MediaTypes mediaType = MediaTypes.MULTIPART_FORM_DATA;

    public Api setMediaType(MediaTypes mediaType){
        this.mediaType = mediaType;
        return this;
    }

    private RequestBody body;
    private HashMap<String, String> perms = null;

    private void setPerms(String key, String value) {
        if (perms == null) {
            perms = new HashMap<>();
        }
        perms.put(key, value);
    }

    public Api canShowProgress(boolean canShowProgress) {
        this.canShowProgress = canShowProgress;
        return this;
    }

    MultipartBody.Builder builder;

    public Api setPerm(String key, String value) {
        if ("POST".equals(method)) {
            if (mediaType == MediaTypes.MULTIPART_FORM_DATA || mediaType == MediaTypes.FORM_DATA) {
                postPerms(key, value);
            }else if (mediaType == MediaTypes.JSON){
                log("setPerm","Coming Soon");
            }else{
                log("setPerm","NOT ALLOWED");
            }
        } else if ("GET".equals(method)) {
            setPerms(key, value);
        }
        return this;
    }



    private void postPerms(String key, String value){
        if (builder == null) {
            builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        }
        log("setPerm",key + " = " + value);
        builder.addFormDataPart(key, value);
    }


    public Api setPerm(Object object) {
        if ("POST".equals(method)) {
            if (builder == null) {
                builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
            }
            for (Field field : object.getClass().getDeclaredFields()) {
                for (Method method : object.getClass().getDeclaredMethods()) {
                    if (method.getName().startsWith("get") && method.getName().length() == (field.getName().length() + 3) && method.getName().toLowerCase().endsWith(field.getName().toLowerCase())) {
                        try {
                            Object value = method.invoke(object);
                            log("setPerm",field.getName() + " = " + value);
                            builder.addFormDataPart(field.getName(), value == null ? "" : (String) value);
                        } catch (IllegalAccessException | InvocationTargetException |
                                 ClassCastException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }else if ("GET".equals(method)){
            for (Field field : object.getClass().getDeclaredFields()) {
                for (Method method : object.getClass().getDeclaredMethods()) {
                    if (method.getName().startsWith("get") && method.getName().length() == (field.getName().length() + 3) && method.getName().toLowerCase().endsWith(field.getName().toLowerCase())) {
                        try {
                            Object value = method.invoke(object);
                            log("setPerm",field.getName() + " = " + value);
                            setPerms(field.getName(), value == null ? "" : (String) value);
                        } catch (IllegalAccessException | InvocationTargetException |
                                 ClassCastException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return this;
    }


    public Api setPerm(String key, String fileName, String url) {
        if ("POST".equals(method)) {
            if (builder == null) {
                builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
            }
            log("setPerm",key + " : File url = " + url + "\nFile name = " + fileName);
            builder.addFormDataPart(key, fileName, RequestBody.create(MediaType.parse("application/octet-stream"),
                    new File(url)));
        }else if ("GET".equals(method)){
            log("setPerm",key + " = NOT ALLOW IN GET");
        }
        return this;
    }

    @Deprecated
    private void setPerms() {
        if (body == null) {
            MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
            for (Map.Entry<String, String> entry : perms.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    builder.addFormDataPart(entry.getKey(), entry.getValue());
                }
                log("setPerms",entry.getKey() + " = " + entry.getValue());
            }
            body = builder.build();
        }
    }


    public Api setSubFolder(String subFolder) {
        BASE_URL = BASE_URL + subFolder + "/";
        return this;
    }

    public void showProgress() {
        if (canShowProgress && showProgress!=null) {
            showProgress.Show();
        }
    }

    public void dismissProgress() {
        if (canShowProgress && showProgress!=null) {
            showProgress.Dismiss();
        }
    }

    public Class<?> dataTo(String jsonData,Class<?> pojo){
        try{
            return (Class<?>) new Gson().fromJson(jsonData,pojo);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public void showInternet() {
        if (showNoInternet!=null) {
            showNoInternet.Show();
        }
    }

    public void dismissInternet() {
        if (showNoInternet!=null) {
            showNoInternet.Dismiss();
        }
    }

    /**
     * ...
     * @deprecated  To update your existing code and use the execute method instead of the deprecated
     * ...
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    @Deprecated
    public void call(String url, @NonNull Response response) {
        dismissInternet();
        if (checkInternet()) {

            if (response.getContext() == null) {
                if (context != null) {
                    response.with(context);
                }
            }

            if ("GET".equals(method)) {
                url = embedUrl(url);
            } else {

                if (perms != null && !perms.isEmpty()) {
                    setPerms();
                } else {
                    if (body == null) {
                        if (builder == null) {
                            //builder = new MultipartBody.Builder().setType(type);
                            body = RequestBody.create(MediaType.parse("text/plain"), "");
                        } else {
                            body = builder.build();
                        }
                    }
                }
            }
            String finalUrl = url;
            showProgress();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            executor.execute(() -> {
                try {
                    OkHttpClient client = new OkHttpClient().newBuilder()
                            .build();

                    log("called url",BASE_URL + finalUrl);

                    Request request = new Request.Builder()
                            .url(BASE_URL + finalUrl)
                            .method(method, body)
                            .build();
                    okhttp3.Response res = client.newCall(request).execute();

                    int responseCode = res.code();
                    StringBuilder sBuilder1 = new StringBuilder();

                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        String line;
                        ResponseBody responseBody = res.body();

                        try {
                            BufferedReader br;
                            if (responseBody != null) {
                                br = new BufferedReader(new InputStreamReader(responseBody.byteStream()));

                                while ((line = br.readLine()) != null) {
                                    sBuilder1.append(line).append("\n");
                                }
                                log("response", sBuilder1.toString());
                                if (!sBuilder1.toString().trim().isEmpty()) {
                                    Object json = new JSONTokener(sBuilder1.toString()).nextValue();
                                    if (json instanceof JSONObject) {
                                        JSONObject jsonObject = new JSONObject(sBuilder1.toString());
                                        if (jsonObject.has("res")) {
                                            String success = jsonObject.getString("res");
                                            String message = jsonObject.getString("msg");
                                            if (success.matches("success")) {
                                                String data = jsonObject.getString("data");
                                                Object json1 = new JSONTokener(data).nextValue();
                                                if (json1 instanceof JSONObject) {
                                                    dismissProgress();
                                                    handler.post(() -> response.onSuccess((JSONObject) json1));
                                                } else if (json1 instanceof JSONArray) {
                                                    dismissProgress();
                                                    handler.post(() -> response.onSuccess((JSONArray) json1));
                                                }
                                                dismissProgress();
                                                handler.post(() -> response.onSuccess(data));

                                            } else {
                                                dismissProgress();
                                                handler.post(() -> response.onFailed(responseCode, message, environment));
                                            }
                                        } else {
                                            dismissProgress();
                                            handler.post(() -> response.onSuccess((JSONObject) json));
                                        }
                                    } else if (json instanceof JSONArray) {
                                        dismissProgress();
                                        handler.post(() -> response.onSuccess((JSONArray) json));
                                    } else {
                                        dismissProgress();
                                        handler.post(() -> response.onSuccess(sBuilder1.toString()));
                                    }
                                } else {
                                    dismissProgress();
                                    handler.post(() -> response.onFailed(responseCode, "No Request Found For this Data.", environment));
                                }
                            }else{
                                dismissProgress();
                                handler.post(() -> response.onFailed(responseCode, "No Request Found For this Data.", environment));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();

                            dismissProgress();
                            handler.post(() -> response.onFailed(responseCode, "No Request Found For this Data. due to exception",environment));

                        }

                    } else {

                        dismissProgress();
                        handler.post(() -> response.onFailed(responseCode, res.message().isEmpty() ? "Page Not Found" : res.message(),environment));

                    }

                } catch (Exception e) {
                    e.printStackTrace();

                    dismissProgress();
                    handler.post(() -> response.onFailed(500, e.getMessage(),environment));

                }
            });


        } else {
            log("check internet","no internet");
            showNoInternet(response);
        }
    }

    private static final int MAX_RETRIES = 3;
    private static final int INITIAL_BACKOFF_SECONDS = 1;

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    public void execute(String url, @NonNull Response response) {
        dismissInternet();

        if (!checkInternet()) {
            log("internet check","no internet");
            showNoInternet(response);
            return;
        }
        CircuitBreaker circuitBreaker = new CircuitBreaker(3, 10000);
        execute(circuitBreaker,url, response, MAX_RETRIES, INITIAL_BACKOFF_SECONDS);
    }

    private boolean freshData = true;

    public Api setFreshData(boolean freshData) {
        this.freshData = freshData;
        return this;
    }

    private void execute(CircuitBreaker circuitBreaker,String url, @NonNull Response response, int retries, int backoffSeconds) {

        if (!circuitBreaker.allowRequest()) {
            System.out.println("Circuit is open. Skipping request.");
            return;
        }

        final String finalUrl = url;
        if ("GET".equals(method)) {
            url = embedUrl(url);
        } else {
            if (perms != null && !perms.isEmpty()) {
                setPerms();
            } else {
                if (body == null) {
                    body = (builder != null) ? builder.build() : RequestBody.create(MediaType.parse("text/plain"), "");
                }
            }
        }

        showProgress();
        log("called url",BASE_URL + finalUrl);
        Request request = new Request.Builder()
                .url(BASE_URL + url)
                .method(method, body)
                .cacheControl(new CacheControl.Builder()
                        .maxStale(freshData ? 0:1, TimeUnit.HOURS) // Accept cached response up to 7 days old
                        .build())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                circuitBreaker.recordFailure();
                if (retries > 0) {
                    int nextBackoffSeconds = backoffSeconds * 2; // Exponential backoff
                    new Handler(Looper.getMainLooper()).postDelayed(() ->
                            execute(circuitBreaker,finalUrl, response, retries - 1, nextBackoffSeconds), nextBackoffSeconds * 1000L);
                } else {
                    handleFailure(response, 500, e.getMessage(), new Handler(Looper.getMainLooper()));
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response res) {
                try (ResponseBody responseBody = res.body()) {
                    if (!res.isSuccessful()) {
                        circuitBreaker.recordFailure();
                        handleFailure(response, res.code(), res.message().isEmpty() ? "Page Not Found" : res.message(), new Handler(Looper.getMainLooper()));
                        return;
                    }

                    circuitBreaker.recordSuccess();
                    String responseBodyString = responseBody != null ? responseBody.string() : "";
                    log("response",responseBodyString);
                    processResponse(responseBodyString, res.code(), response, new Handler(Looper.getMainLooper()));
                } catch (IOException e) {
                    e.printStackTrace();
                    circuitBreaker.recordFailure();
                    handleFailure(response, 500, e.getMessage(), new Handler(Looper.getMainLooper()));
                } finally {
                    dismissProgress();
                }
            }
        });
    }

    private void processResponse(String responseBody, int responseCode, Response response, Handler handler) {
        try {
            Object json = new JSONTokener(responseBody).nextValue();

            if (json instanceof JSONObject) {
                JSONObject jsonObject = new JSONObject(responseBody);
                if (jsonObject.has("res")) {
                    String success = jsonObject.getString("res");
                    String message = jsonObject.getString("msg");

                    if ("success".equals(success)) {
                        String data = jsonObject.optString("data");
                        Object jsonData = new JSONTokener(data).nextValue();
                        if (jsonData instanceof JSONObject) {
                            handler.post(() -> response.onSuccess((JSONObject) jsonData));
                        } else if (jsonData instanceof JSONArray) {
                            handler.post(() -> response.onSuccess((JSONArray) jsonData));
                        } else {
                            handler.post(() -> response.onSuccess(data));
                        }
                    } else {
                        handler.post(() -> response.onFailed(responseCode, message,environment));
                    }
                } else {
                    handler.post(() -> response.onSuccess(jsonObject));
                }
            } else if (json instanceof JSONArray) {
                handler.post(() -> response.onSuccess((JSONArray) json));
            } else {
                handler.post(() -> response.onSuccess(responseBody));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            handler.post(() -> response.onFailed(500, "Error parsing response",environment));
        }
    }

    private void handleFailure(Response response, int statusCode, String errorMessage, Handler handler) {
        dismissProgress();
        handler.post(() -> response.onFailed(statusCode, errorMessage,environment));
    }



    private void showNoInternet(Response response) {
        if (context != null) {
            if (response.getContext() != null) {
                response.onFailed(0, "No Internet Connection",environment);
            } else {
                if (context != null) {
                    response.with(context);
                    response.onFailed(0, "No Internet Connection",environment);
                }
            }
            showInternet();
        }
    }

    private <T> void showNoInternet(ResponseCallback<T> callback) {
        // Implement showNoInternet logic
        callback.onFailed(0,"No Internet Connection");
        showInternet();
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private boolean checkInternet() {
        String status;
        if (context==null){
            log("checkInternet","UNABLE TO CHECK this is background process");
            return true;
        }
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                status = "Wifi enabled";
                log("checkInternet",status);
                return true;
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                status = "Mobile data enabled";
                log("checkInternet",status);
                return true;
            }
        } else {
            status = "No internet is available";
            log("checkInternet",status);
            return false;
        }

        return false;
    }

    private String embedUrl(String url) {
        StringBuilder u = new StringBuilder();
        u.append(url);
        if (perms != null && !perms.isEmpty()) {
            u.append("?");
            for (Map.Entry<String, String> entry : perms.entrySet()) {
                u.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
        }
        return u.toString();
    }

    private Environment environment = Environment.LIVE;

    public Api setEnvironment(Environment environment) {
        this.environment = environment;
        return this;
    }

    private void log(String type, String string){
        if(environment == Environment.DEBUG) {
            Log.e(TAG, "log: " + type + " : " + string);
        }
    }

}
