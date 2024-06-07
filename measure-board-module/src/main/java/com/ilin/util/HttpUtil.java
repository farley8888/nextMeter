package com.ilin.util;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpUtil {

    final static String TAG = HttpUtil.class.getSimpleName();


    public interface Ls {
        void onResult(String rs);
    }
    public static void getDataAsync(Ls ls) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
//                .url("http://www.baidu.com")
                .url("http://163.177.151.110/")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                ls.onResult(e.getMessage());
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful()){//回调的方法执行在子线程。
                    Log.d("kwwl","获取数据成功了");
                    Log.d("kwwl","response.code()=="+response.code());
                    Log.d("kwwl","response.body().string()=="+response.body().string());
                    ls.onResult(response.body().toString());
                }else {
                    ls.onResult(response.code()+"");
                }
            }
        });
        ls.onResult("start");
    }


    public static void httpPostReq() {
        OkHttpClient client = new OkHttpClient();//创建OkHttpClient对象。
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        JSONObject json = new JSONObject();
        try {
            json.put("idCard", "450330198708080719");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        System.out.println("HttpUtil.httpPostReq = " + json);
        RequestBody requestBody = RequestBody.create(json.toString(), JSON);

        String url = "http://xt3api.yunjivip.cn/acheng-user/user/getUserFeatureByIdOrIdCardOrMobile";
//        String url = "http://116.211.154.180:80/acheng-user/user/getUserFeatureByIdOrIdCardOrMobile";
        Request request = new Request.Builder()//创建Request 对象。
                .url(url)
                .post(requestBody)//传递请求体
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.i(TAG,"response.getData()= "+ response.code());
                Log.d(TAG,"response.getData().string() = "+response.body().string());
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                System.err.println("call = " + call + ", err = " + e);
                e.printStackTrace();
            }
        });
    }


    public static void postDataWithParame() {
        OkHttpClient client = new OkHttpClient();//创建OkHttpClient对象。
        FormBody.Builder formBody = new FormBody.Builder();//创建表单请求体
        formBody.add("username","zhangsan");//传递键值对参数
        Request request = new Request.Builder()//创建Request 对象。
                .url("http://www.baidu.com")
                .post(formBody.build())//传递请求体
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }
        });
    }

}
