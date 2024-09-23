package com.ilin.atelec;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.serial.opt.JLog;
import com.serial.opt.UartWorker;

import java.io.IOException;

import android_serialport_api.SerialUtils;

import android.os.Build;

/***
 * 业务处理类
 */
public class BusModel {

    private final String TAG = getClass().getSimpleName();
    private UartWorker uartWorker = null;

    private static final JLog LOG = new JLog("BusModel", ICmd.isLogOn, JLog.TYPE_INFO);

    private Context mCtx;

    private static BusModel instance;

    /****
     * 收到数据后,分发通知
     */
    private UartWorker.OnDistributeListener mListener;

    public static BusModel getInstance(Context ctx) {
        if (null == instance) {
            instance = new BusModel(ctx);
        }
        return instance;
    }

    public void setListener(UartWorker.OnDistributeListener listener) {
        this.mListener = listener;
        boolean isSimulator = Build.MODEL.contains("sdk") || Build.MODEL.contains("google_sdk") || Build.MODEL.contains("Android SDK built for arm64");

        if (!isSimulator) {
            if (uartWorker != null) {
                uartWorker.setProtocalDistributeListener(mListener);
            }
        }

    }

    /***
     * 初始化串口
     * @param path
     * @param bitRate
     */
    public void init(final String path, int bitRate) {
        if (uartWorker != null) {
            uartWorker.release();
        }
        try {
            uartWorker = new UartWorker(path, bitRate, 0);
        } catch (SecurityException e) {
            LOG.print("打开串口失败，安全问题，即权限不够！");
            Toast.makeText(mCtx, "打开串口失败，安全问题，即权限不够！", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            LOG.print("打开串口失败，不存在！");
            //LOG.print(e.getLocalizedMessage());
            Toast.makeText(mCtx, "打开串口失败，异常发生了", Toast.LENGTH_SHORT).show();
        }

        if (null != uartWorker) {
            uartWorker.setProtocalDistributeListener(mListener);
            uartWorker.startCommunicate();
        }
    }

    public void stopCommunicate() {
        if (uartWorker != null) {
            uartWorker.stopCommunicate();
        }
    }

    public void startCommunicate() {
        if (uartWorker != null) {
            uartWorker.startCommunicate();
        }
    }

    /***
     *
     * activity 的context
     * @param ctx
     */
    private BusModel(final Context ctx) {
        mCtx = ctx;
    }


    public boolean write(String data) {
        try {
            boolean isOk = uartWorker.getWriter().writeData(SerialUtils.HexToByteArr(data));
            Log.i(TAG, "write: 写入数据 = " + data);
            return isOk;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /***
     * 发送命令
     * @param data
     */
    public boolean sendCmd(byte[] data) {
        try {
            boolean isOk = uartWorker.getWriter().writeData(data);
            return isOk;
        } catch (Exception e) {
            Log.e("BusModelWriteError", e.getLocalizedMessage(), e);
            return false;
        }
    }

    public void stop() {
        if (uartWorker != null) {
//            uartWorker.stopCommunicate();
            uartWorker.release();
        }
    }

}
