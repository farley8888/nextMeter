package com.ilin.atelec;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ilin.util.AmapLocationUtils;
import com.ilin.util.Config;
import com.ilin.util.FloatWindow;
import com.ilin.util.ShellUtils;
import com.serial.opt.UartWorkerCH;
import com.serial.port.ByteUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import android_serialport_api.Command;

/**
 * 计价器通信demo
 */
public class MainActivity extends AppCompatActivity {

    private static final int WHAT_PRINT_STATUS = 110;
    private BusModel mBusModel;
    final String TAG = getClass().getSimpleName();
    public static final int STATUES_INIT = 1;
    public static final int STATUES_START = 2;
    public static final int STATUES_PAUSE = 3;
    public static final int STATUES_END = 4;
    ExecutorService mExecutor;

    private int mCurrentStatus = 0;

    private TextView mTv, mPrintStatus, mAccStatus;
    private LinearLayout mLy;
    private TextView mTVCh3Opt;
    private UartWorkerCH mWorkCh2;
    private UartWorkerCH mWorkCh3;
    private int mCh3Opt;
    private boolean isPause;
    private FloatWindow mFloatWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        mTv = findViewById(R.id.tv_statues);
        mLy = findViewById(R.id.ly_datas);
        mPrintStatus = findViewById(R.id.tv_print_status);
        mAccStatus = findViewById(R.id.tv_acc_status);
        mExecutor = createThreadPool(true);
        AmapLocationUtils.getInstance().init(getApplicationContext());
        mHandler.postDelayed(()->{
            openCommonUart();
            setReceiveEvalDataLs();
        }, 2000);
        setSwitchLs();
        // echo 180 > /sys/class/backlight/backlight/brightness
        ShellUtils.execShellCmd("settings put system screen_brightness 150"); // 210 会闪屏
    }

    // 空车开关
    private void setSwitchLs() {
        Switch sw = findViewById(R.id.switch1);
        String gpio117 = ShellUtils.execShellCmd("cat /sys/class/gpio/gpio117/value");
        String gpio116 = ShellUtils.execShellCmd("cat /sys/class/gpio/gpio116/value");
        System.out.println("MainActivity.setSwitchLs gpio117 = " + gpio117 + ", io116 = " + gpio116);
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            new Thread(() -> {
                if (isChecked){
                    try {
                        ShellUtils.echo(new String[]{"echo 0 >/sys/class/gpio/gpio117/value"});
                        Thread.sleep(300);
                        ShellUtils.echo(new String[]{"echo 1 > /sys/class/gpio/gpio116/value"});
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else {
                    try {
                        ShellUtils.echo(new String[]{"echo 1 > /sys/class/gpio/gpio117/value"});
                        Thread.sleep(300);
                        ShellUtils.echo(new String[]{"echo 0 > /sys/class/gpio/gpio116/value"});
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        });
    }

    Handler mHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
//            System.out.println("handleMessage.msg = " + msg);
            if (msg.what == IAtCmd.W_MSG_DISPLAY){
                if (msg.obj == null) return;
                String receiveData = msg.obj.toString();
                TextView tv = (TextView) LayoutInflater.from(getBaseContext()).inflate(R.layout.item_data, null);
                mLy.addView(tv, 0);
                Calendar cl = Calendar.getInstance(TimeZone.getTimeZone("GMT-0:00"));
                tv.setText(dateToString(cl.getTime(), "HH:mm:ss "));
                tv.append(receiveData);
                if (mLy.getChildCount() > 20){
                    mLy.removeViewAt(20);
                }

                Log.d("setReceiveEvalDataLs","setReceiveEvalDataLs time: "+ dateToString(cl.getTime(), "HH:mm:ss ")+ " -> " +receiveData);
                checkStatues(receiveData);
            } else if (msg.what == WHAT_PRINT_STATUS) {
                String st = ShellUtils.execShellCmd("cat /sys/class/gpio/gpio73/value");
//                System.out.println("MainActivity.handleMessage 打印机状态 = [" + st + "]");
                mPrintStatus.setText(st);
                // 钥匙开关状态
                String acc = ShellUtils.execShellCmd("cat /sys/class/gpio/gpio75/value");
                mAccStatus.setText(acc);
                removeMessages(WHAT_PRINT_STATUS);
//                sendEmptyMessageDelayed(WHAT_PRINT_STATUS, 1800);
            } else if (msg.what == 120) {
                try {
                    mWorkCh3.getWriter().writeData(ByteUtils.hexStr2Byte(msg.obj.toString()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private void checkStatues(String result) {
        if (result.startsWith("55AA") && result.endsWith("55AA")
            && result.length() > 16 && result.startsWith("E4", 14)) {
//        Thread.sleep(50L);
            mBusModel.write(Command.CMD_END_RESPONSE);
            Log.e(TAG, "checkStatues: 营运数据应答");
        }
    }

    /**
     * 把日期转换为字符串
     */
    public String dateToString(Date date, String format) {
        String result = "";
        if (null != date) {
            SimpleDateFormat formater = new SimpleDateFormat(format);
            try {
                result = formater.format(date);
            } catch (Exception e) {
                Log.e("DataUtils", "时间格式转换异常: date:" + date + ", format:" + format);
            }
        }
        return result;
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        createFlowView(100, 100);
    }

    public void createFlowView(int w, int h){
        if (mFloatWindow == null) {
            // 创建一个新的悬浮窗
            mFloatWindow = new FloatWindow(getApplicationContext(), w, h);
            // 设置悬浮窗的布局内容
            mFloatWindow.setLayout(R.layout.flow_view);
//            mFloatWindow.show();
            Log.i(TAG, "createFlowView: 启动浮窗侦测");
        }
    }

    public void onClick(View v){
        if (v.getId() == R.id.btn1){
            initStatus();
        }
        else if (v.getId() == R.id.btnStart){
            mBusModel.write(Command.CMD_START);
            changeStatus(STATUES_START);
        }
        else if (v.getId() == R.id.btnPause){
            ((TextView)v).setText(mCurrentStatus == STATUES_START ? "continue" : "pause");
            mBusModel.write(mCurrentStatus == STATUES_START ? Command.CMD_PAUSE : Command.CMD_CONTINUE);
            changeStatus(mCurrentStatus == STATUES_START ? STATUES_PAUSE : STATUES_START);
        }
        else if (v.getId() == R.id.btnEnd){
            mBusModel.write(Command.CMD_END);
            changeStatus(STATUES_END);
        }
        else if (v.getId() == R.id.btn_ser2_open){
            openCH2();
        }
        else if (v.getId() == R.id.btn_ser2_send){
            if (mWorkCh2 != null) {
                try {
                    String writeData = ((EditText)findViewById(R.id.edt_ser2)).getText().toString();
//                    mWorkCh2.getWriter().writeData(ByteUtils.hexStr2Byte("5a6a7a"));
                    mWorkCh2.getWriter().writeData(ByteUtils.hexStr2Byte(writeData));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else Toast.makeText(this, "未打开", 0).show();
        }
        else if (v.getId() == R.id.btn_ser3_open) {
            openCH3();
        } else if (v.getId() == R.id.btn_ser31_send) {
            workCh3SendData(1, ((EditText)findViewById(R.id.edt_ser31)).getText().toString());
        }else if (v.getId() == R.id.btn_ser32_send) {
            workCh3SendData(2, ((EditText)findViewById(R.id.edt_ser32)).getText().toString());
        }else if (v.getId() == R.id.btn_ser33_send) {
            workCh3SendData(3, ((EditText)findViewById(R.id.edt_ser33)).getText().toString());
        } else if (v.getId() == R.id.btn_print) {
            sendPrintCmd();
        } else if (v.getId() == R.id.btn_settings) {
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            ComponentName component = new ComponentName("com.android.settings", "com.android.settings.Settings");
            intent.setComponent(component);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("extra_prefs_show_button_bar", true);
            intent.putExtra("extra_prefs_set_next_text", "完成");
            intent.putExtra("extra_prefs_set_back_text", "返回");
            startActivity(intent);
        }
        else if (v.getId() == R.id.btn_4gEtc) {
            startActivity(new Intent(this, TF4GActivity.class));
        }
        if (v.getId() == R.id.btn_L150){
            ShellUtils.execShellCmd("settings put system screen_brightness 150");
        } else if (v.getId() == R.id.btn_L190) {
            ShellUtils.execShellCmd("settings put system screen_brightness 190");
        }
    }


    private ExecutorService createThreadPool(boolean isCheck) {
        if (isCheck){
            if (mExecutor != null) return mExecutor;
        }
        //
        // 不能用 new LinkedBlockingQueue<>() 默认构造函数，时间久了可能会出现OOM
        return new ThreadPoolExecutor(3, 3, 0L,
                java.util.concurrent.TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(50));
    }

    //TODO: copy over the printer cmd
    private void sendPrintCmd(){
        if (mWorkCh3 == null){
            Toast.makeText(this, "未打开Ch3", 0).show();
            return;
        }
//        mTVCh3Opt = findViewById(R.id.tv_result_);
        mCh3Opt = 4;
        mExecutor.execute(()->{
            try {
                ShellUtils.execEcho("echo 0 > /sys/class/gpio/gpio64/value");
                Thread.sleep(200);
                ShellUtils.execEcho("echo 0 > /sys/class/gpio/gpio65/value");  // 11 打印
                Thread.sleep(200);
                String data = Config.getSPIData().replace(" ", "");
                byte[] bytes = ByteUtils.hexStr2Byte(data);
                mWorkCh3.getWriter().writeData(bytes);
                System.out.println("MainActivity.sendPrintCmd 开始打印……");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void workCh3SendData(int opt, String data) {
        if (mWorkCh3 != null) {
            if (opt == 1){
                ShellUtils.execEcho("echo 1 > /sys/class/gpio/gpio64/value");
                ShellUtils.execEcho("echo 1 > /sys/class/gpio/gpio65/value"); // 00 对应背面第1个串口
                mTVCh3Opt = findViewById(R.id.tv_result_ser31);
            }else if (opt == 2){
                ShellUtils.execEcho("echo 1 > /sys/class/gpio/gpio64/value");
                ShellUtils.execEcho("echo 0 > /sys/class/gpio/gpio65/value"); // 01 对应背面第2个串口
                mTVCh3Opt = findViewById(R.id.tv_result_ser32);
            }else if (opt == 3){
                ShellUtils.execEcho("echo 0 > /sys/class/gpio/gpio64/value");
                ShellUtils.execEcho("echo 1 > /sys/class/gpio/gpio65/value");  // 10 对应背面第4个串口
                mTVCh3Opt = findViewById(R.id.tv_result_ser33);
            }else {
                Log.w(TAG, "workCh3SendData: 不支持的opt = " + opt );
                return;
            }
            mCh3Opt = opt;
            Message msg = mHandler.obtainMessage();
            msg.what = 120;
            msg.obj = data;
            mHandler.sendMessageDelayed(msg, 20);

//            mExecutor.execute(()->{
//                try {
//                    Thread.sleep(50);
//                    mWorkCh3.getWriter().writeData(ByteUtils.hexStr2Byte(data));
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            });
        }else Toast.makeText(this, "Ch3未打开", Toast.LENGTH_SHORT).show();
    }

    void changeStatus(int statu){
        mCurrentStatus = statu;
        switch (statu){
            case STATUES_INIT:
                mTv.setText("就绪");
                break;
            case STATUES_START:
                mTv.setText("运行中");
                break;
            case STATUES_PAUSE:
                mTv.setText("暂停");
                break;
            case STATUES_END:
                mTv.setText("结束");
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isPause = false;
        if (mBusModel != null) mBusModel.startCommunicate();
        if (mFloatWindow != null) mFloatWindow.close();
        mHandler.sendEmptyMessageDelayed(WHAT_PRINT_STATUS, 2800);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mFloatWindow != null) mFloatWindow.show();
        isPause = true;
        if (mBusModel != null) mBusModel.stopCommunicate();
        mHandler.removeMessages(WHAT_PRINT_STATUS);
    }

    // 打开计价串口
    void openCommonUart(){
        try {
            if (null == mBusModel) {
                mBusModel = mBusModel.getInstance(this);
            }
            mBusModel.init(Config.SERIAL_CH1, Config.BATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 设置接受计量版数据监听
    void setReceiveEvalDataLs(){
        mBusModel.setListener(data -> {
            mHandler.sendMessage(mHandler.obtainMessage(IAtCmd.W_MSG_DISPLAY, data));
            Log.d("setReceiveEvalDataLs","setReceiveEvalDataLs "+ data.toString());
        });
    }

    private void openCH2(){
        try {
            mWorkCh2 = new UartWorkerCH(Config.SERIAL_CH2, Config.BATE_CH, 0, "CH2"); // 对应背面第3个串口
            mWorkCh2.setOnReceiveListener(data -> {
                mHandler.post(()->{
                    ((TextView)findViewById(R.id.tv_result_ser2)).setText(data);
                });
            });
            mWorkCh2.startCommunicate();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openCH3(){
        try {
            mWorkCh3 = new UartWorkerCH(Config.SERIAL_CH3, Config.BATE_CH, 0, "CH3");
            mWorkCh3.setOnReceiveListener(data -> {
                mHandler.post(()->{
                    System.out.println("CH3.Opt" + mCh3Opt + " receive = " + data);
                    if (mTVCh3Opt != null) mTVCh3Opt.setText(data);
                });
            });
            mWorkCh3.startCommunicate();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initStatus() {
        mBusModel.write(Command.CMD_INIT);
        changeStatus(STATUES_INIT);
        mLy.removeAllViews();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        System.out.println(" keyCode = " + keyCode + ", event = " + event);
        int code = event.getScanCode();
        if (248 <= code && code <= 255){
            myKeyCodeDown(code);
        }
        if (keyCode == 10){
            initStatus();
        }
        if (keyCode == 20){
            mBusModel.write(Command.CMD_START);
        }
        return super.onKeyDown(keyCode, event);
    }

    String[] keyMaps = new String[]{"empty", "go", "stop", "pay", "set", "dollar 10", "dollar 1", "print",};
    // 自定义按键，8个
    void myKeyCodeDown(int code){
        switch (code){
            case 248: // empty

                break;
            case 249: // go

                break;
            case 250: // stop

                break;
            case 251: // pay

                break;
            case 252: // set

                break;
            case 253: // dollar 10

                break;
            case 254: // dollar 1

                break;
            case 255: // print

                break;
        }
        Toast.makeText(this, keyMaps[code - 248], Toast.LENGTH_SHORT).show();
    }

}