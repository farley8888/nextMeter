package com.ilin.atelec;

import android.content.Context;
import android.os.Environment;


import com.ilin.util.FileUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by fanjc on 2021/9/27 027.
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    public static final String TAG = "CrashHandler";

    private static CrashHandler INSTANCE = new CrashHandler();

    private Context mContext;

    private SimpleDateFormat mFormate = null;

    private Thread.UncaughtExceptionHandler mDefaultHandler;

    private CrashHandler() {
    }

    public static CrashHandler getInstance() {
        return INSTANCE;
    }

    public void init(Context ctx) {
        mContext = ctx;
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
        mFormate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ");
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {

        handleException(ex);

       /* new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                new AlertDialog.Builder(mContext).setTitle("提示").setCancelable(false)
                        .setMessage("433服务崩溃了...").setNeutralButton("我知道了", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        System.exit(0);
                    }
                })
                        .create().show();
                Looper.loop();
            }
        }.start();*/
        System.exit(0);
    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成. 开发者可以根据自己的情况来自定义异常处理逻辑
     *
     * @param ex
     * @return true:如果处理了该异常信息;否则返回false
     */
    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return true;
        }
        saveToFile(ex.getMessage());
        return true;
    }

    private void saveToFile(String msg) {
        File f = new File(Environment.getExternalStorageDirectory() + "/eclectric");
        if (!f.exists())
            f.mkdirs();
        File f1 = new File(f, "ChargeStubCrash.txt");
        FileUtil.appendToFile(mFormate.format(new Date()) + msg, f1);
        f = null;
        f1 = null;
    }

}
