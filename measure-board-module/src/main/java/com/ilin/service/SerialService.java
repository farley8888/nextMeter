package com.ilin.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.ilin.util.SoundUtil;
import com.ilin.atelec.AtBusModel;
import com.ilin.atelec.IAtCmd;
import com.ilin.atelec.R;
import com.serial.opt.JLog;

/***
 * 串口服务test
 */
public class SerialService extends Service {

    private AtBusModel busModel = null;

    protected String appId;

    protected String appKey;

    protected String secretKey;

    protected String sn; // 纯离线合成SDK授权码；离在线合成SDK没有此参数


    private Context mCtx;

    private static final JLog LOG = new JLog("SerialService", IAtCmd.isLogOn, JLog.TYPE_INFO);

    @Override
    public void onCreate() {
        super.onCreate();
        LOG.print("服务创建了！");
        mCtx = this;
        init();
    }

    private void init() {
        LOG.print("初始化串口！");

        if (busModel == null) {
            busModel = AtBusModel.getInstance(this);
            busModel.init("/dev/ttyMT2", 115200);
            Toast.makeText(this, "初始化成功！", Toast.LENGTH_SHORT).show();
        }
        SoundUtil.init(this, R.raw.welcome_jt);
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (busModel != null) {
            busModel.stop();
            busModel = null;
        }
    }

    public SerialService() {
        super();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
