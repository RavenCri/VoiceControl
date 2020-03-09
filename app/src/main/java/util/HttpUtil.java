package util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;


import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HttpUtil {
    public static JSONObject get(String url, Map<String,String> param){
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
                    .url(urlBuilder.build()).get().build();
            Response response = client.newCall(request).execute();

            return JSON.parseObject(response.body().string());

        } catch (Exception e) {
            JSONObject object = new JSONObject();
            object.put("status","error");
            object.put("data","服务器连接异常，请先检测您是否已经开启了服务器。");
            return object;

        }
    }
}
