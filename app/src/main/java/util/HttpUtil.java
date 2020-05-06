package util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;


import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import activity.LoginActivity;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpUtil {
    public static Response get(String url, Map<String,String> param){
        String text = null ;
        try {
            OkHttpClient client = new OkHttpClient.Builder().connectTimeout(2, TimeUnit.SECONDS).build();//创建OkHttpClient对象

            HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
            if(param != null){
                param.forEach((k,v)->{
                    urlBuilder.addQueryParameter(k,v);
                });
            }

            Request request = new Request.Builder()
                    .url(urlBuilder.build()).get().header("token", LoginActivity.token).build();
            Response response = client.newCall(request).execute();
            return response;
        } catch (Exception e) {
            e.printStackTrace();
           return null;
        }
    }

    public static Response post(String url, Map<String,String> param)  {

        try {
            OkHttpClient client = new OkHttpClient.Builder().connectTimeout(2, TimeUnit.SECONDS).build();
            HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
            FormBody.Builder requestBody = new FormBody.Builder();
            System.out.println(url);
            param.forEach((k,v)->{
                requestBody.add(k,v);
            });
            Request request = new Request.Builder()
                    .url(urlBuilder.build()).post(requestBody.build())
                    .header("token", LoginActivity.token)
                    .build();
            Response response = client.newCall(request).execute();
            return response;
        } catch (IOException e) {
            return null;
        }
    }
}
