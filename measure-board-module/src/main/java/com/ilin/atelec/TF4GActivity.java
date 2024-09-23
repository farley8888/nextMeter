package com.ilin.atelec;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.ilin.service.GPSService;
import com.ilin.util.AmapLocationUtils;
import com.ilin.util.Config;
import com.ilin.util.NetWorkUtils;
import com.ilin.util.WebViewUtil;

import java.io.File;


public class TF4GActivity extends AppCompatActivity {

    private final String TAG = getClass().getSimpleName();
    private AtBusModel mBusModel;
    private TextView mTv4G;
    private TextView mTvGPS;
    private TextView mTvLocation;
    WebView mWebView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebViewUtil.hookWebView();
        setContentView(R.layout.activity_tf4g);
        mWebView = findViewById(R.id.webView);
        mTv4G = findViewById(R.id.tv_4gStatus);
        mTvGPS = findViewById(R.id.tv_gpsStatus);
        mTvLocation = findViewById(R.id.tv_amapLocation);
        openCH4G();
        initGpsTest();

        register();
        testGps();
        startAMapLocation();
    }

    private void startAMapLocation() {
        AmapLocationUtils.getInstance().setLocationListener(aMapLocation -> {
            String info = AmapLocationUtils.getInstance().getInfo(aMapLocation, false);
            mTvLocation.setText(info);
        });
        AmapLocationUtils.getInstance().startLocation();
    }


    private void openCH4G(){
        try {
            if (null == mBusModel) {
                mBusModel = mBusModel.getInstance(this);
            }
            mBusModel.init(Config.SERIAL_CH0, Config.BATE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initGpsTest() {
       findViewById(R.id.btn_testGps).setOnClickListener(v->{
           try {
               Intent it = new Intent();
               // 打开GPS测试app
               it.setComponent(new ComponentName("com.android.gpstest", "com.android.gpstest.GpsTestActivity"));
               startActivity(it);
           } catch (Exception e) {
               e.printStackTrace();
               Toast.makeText(this, e.getMessage(), 1).show();
           }
       });
        findViewById(R.id.btn_testGps2).setOnClickListener(v->{
            try {
                Intent it = new Intent();
                // 打开GPS测试app
//               com.chartcross.gpstestplus/.GPSTestPlus  芯片厂的
                it.setComponent(new ComponentName("com.chartcross.gpstestplus", "com.chartcross.gpstestplus.GPSTestPlus"));
                startActivity(it);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, e.getMessage(), 1).show();
            }
        });
        findViewById(R.id.btn_getLoc).setOnClickListener(v->{
            getGpsLocation();
        });
    }

    private void setGpsLS() {
        GPSService.setLs(location -> {
            mTvGPS.setText(location.toString());
       });
    }

    public void onClick(View v){
        if (v.getId() == R.id.btnBack) finish();
        if (v.getId() == R.id.btn_web){
            openBaidu();
        }
//        if (v.getId() == R.id.btn_testApi){
//            new Thread(()->{
//                HttpUtil.getDataAsync(rs-> {
//                    mWebView.post(()->{
//                        Toast.makeText(TF4GActivity.this, "httpRS = " + rs , 1).show();
//                    });
//                });
//            }).start();
//        }
        if (v.getId() == R.id.btn_goBrowse){
            if (mWebView.canGoBack()) mWebView.goBack();
        }
        if (v.getId() == R.id.btn_has4G){
            mBusModel.checkHas4G(data -> {
                boolean has4GModel = data.contains("OK");
                Log.i(TAG, "checkHas4G: 是否有扩展4G模块 = " + has4GModel);
                if (!has4GModel) set4gStatus("没硬件模块");
            });
        }
        if (v.getId() == R.id.btn_checkInsert){
            mBusModel.checkInsertCard(data -> {
                boolean isInsert = data.contains("READY");
                // +CPIN: READY //SIM 卡已准备好
                System.out.println("Gps4GActivity.onClick 是否插入4G卡 = " + isInsert);
                if (isInsert) checkCsqAndSysInfo();
                else set4gStatus("未插卡");
            });
        }
        if (v.getId() == R.id.btn_connect){
            atConnect();
        } else if (v.getId() == R.id.btn_createSocket) {
            createSocket();
        }
        if (v.getId() == R.id.btn_send){
            sendData();
        } else if (v.getId() == R.id.btn_closeAll) {
            closeAll();
        }
//        if (v.getId() == R.id.btn_httpInit){
//            mBusModel.http0Init(data -> {
//                System.out.println("TF4GActivity.onClick httpInit = " + data);
//            });
//        }
//        if (v.getId() == R.id.btn_httpSetUrl){
//            mBusModel.httpTest(data -> {
//                System.out.println("TF4GActivity.onClick httpSetUrl = " + data);
//            }, 100);
//        }
    }

    private void openBaidu() {
        if (mWebView == null) return;
        mWebView.setVisibility(View.VISIBLE);
//        mWebView.loadUrl("http://163.177.151.110/");
        mWebView.loadUrl("https://music.163.com/");
        WebViewUtil.setWebView(this, mWebView);
        mWebView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                //使用WebView加载显示url
                view.loadUrl(url);
                return true;
            }
        });
        WebViewUtil.setInnerLinkEnable(mWebView);
//        new Thread(HttpUtil::getDataAsync).start();
    }

    private void atConnect() {
        mBusModel.setAPN(data -> {
            Log.i(TAG, "atConnect: setApn = " + data);
            mBusModel.restRspLs();
            mBusModel.actionQIP(data1 -> {
                Log.i(TAG, "atConnect: actionQip = " + data1);

            });
        });

    }

    private void createSocket(){
//        mBusModel.crateSocket(data -> {
//            Log.i(TAG, "create: = " + data);
//
//        });
        new Thread(()->{
            int i = 0;
            String rs = "ping ";
            int state = -1;
            while (i < 5){
                state = NetWorkUtils.isNetPingUsable();
                System.out.println("TF4GActivity.ping State = " + state);
                if (state == 0) break;
                i++;
            }
            if (state == 0) rs += "网络可用";
            else if (state == 1) rs += "未认证";
            else if (state == 2) rs += "无网络";
            String finalRs = rs;
            mWebView.post(()->{
                Toast.makeText(this, finalRs, 1).show();
            });
        }).start();
    }

    void sendData(){
//        mBusModel.hasSocket(data -> {
//            Log.i(TAG, "sendData: socketConnect = " + data);
//           boolean hasConnect = data.contains("OK");
//           mBusModel.restRspLs();
//           if (hasConnect){
               mBusModel.sendData(data1 -> {
                   System.out.println("Gps4GActivity.sendData rsp = " + data1);

               }, null, 1);
//           }else {
//               Log.e(TAG, "sendData: socket未连接 ！" );
//           }
//        });

    }

    void closeAll(){
        mBusModel.closeSocket();
//        mBusModel.closeTcpIp();
    }

    void checkCsqAndSysInfo(){
        mBusModel.checkCSQ(data -> { // 信号
            //  +CSQ: 13,99
            System.out.println("Gps4GActivity.checkCsq = " + data);
            mBusModel.restRspLs();
            String csq = data.substring(data.indexOf("Q:") + 3, data.indexOf(","));
            setCsqLevel(csq);
            mBusModel.checkSysInfo(data1 -> { // 注册信息
                System.out.println("Gps4GActivity.checkSysInfo = " + data);
                mBusModel.restRspLs();
            });
        });
    }

    private void setCsqLevel(String csq) {
        int level = Integer.parseInt(csq.trim());
        String levStr = "";
        // 0-31之间 ，至少大于10
        if (level > 28){
            levStr = "信号强";
        }else if (level > 18){
            levStr = "信号中";
        }else if (level > 10){
            levStr = "信号弱";
        }else {
            levStr = "没信号";
        }
        set4gStatus(levStr);
//        if (level > 10) testGps();
    }

    private void testGps() {
        setGpsLS();
        Intent it = new Intent(getApplicationContext(), GPSService.class);
        it.setAction("act.init.gps");
        startService(it);
    }

    private void set4gStatus(String status){
        String finalLevStr = status;
        mTv4G.post(()->{
            mTv4G.setText(finalLevStr);
        });
    }

    // 声明TF卡监听广播
    BroadcastReceiver mountReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Uri data = intent.getData();
            String path = data.getPath();
            File file = new File(path);
            System.out.println(".onReceive 插拔SdCard = " + action);
            System.out.println(".onReceive 插拔SdCard = " + path);
            TextView tv = findViewById(R.id.tv_tfCard);
            if (Intent.ACTION_MEDIA_MOUNTED.equals(action)){
                tv.setText("插入：" + path);
            }else {
                tv.setText("拔出：" +path);
            }
            // doing
        }
    };

    // 注册接受广播
    void register() {
        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
            intentFilter.setPriority(1000);
            intentFilter.addDataScheme("file");
            registerReceiver(mountReceiver, intentFilter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 注销
    void unregister() {
        try {
            unregisterReceiver(mountReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBusModel != null) mBusModel.stop();
        unregister();
        AmapLocationUtils.getInstance().stopLocation();
    }

    void getGpsLocation(){
        //获取位置信息的入口级类，要获取位置信息，首先需要获取一个LocationManger对象
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this,"请手机GPS定位服务！",Toast.LENGTH_LONG).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this,"请授予位置信息权限！",Toast.LENGTH_LONG).show();
            return;
        }

         Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (location ==null) {
            Toast.makeText(this,"获取位置失败！",Toast.LENGTH_LONG).show();
            return;
        }

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        mTvGPS.setText("纬度：" + latitude +"\n" +"经度：" + longitude);

    }

}
