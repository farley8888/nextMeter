package com.ilin.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.concurrent.ConcurrentLinkedQueue;
/**
 * 日志打印记录录
 * Created by june on 2017/5/6 006.
 */

public class FLog {

    private static final FLog ourInstance = new FLog();

    private final String TAG = "disk433";

    private ConcurrentLinkedQueue<String> msgQueue = new ConcurrentLinkedQueue<String>();

    private Object mLock = new Object();

    private SimpleDateFormat mFormate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS  ");

    private boolean canWrite = true;
    /**只是标准输出即输出到控制台***/
    public static final int STDOUT = 0;
    /**不记录日志***/
    public static final int NONE = -1;
    /**标准输出及文件记录***/
    public static final int BORTH = 1;
    /**只记录到文件*/
    public static final int ONLY_FILE = 2;
    /**日志等级*/
    private int logCls = ONLY_FILE;
    /**文件路径**/
    private String filePath = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + java.io.File.separator + "hireSerialData.txt";

    private FileWriter mFw = null;

    private BufferedWriter mBuffer;

    public static FLog getInstance() {
        return ourInstance;
    }

    private FLog() {
        //newThread();
    }

    /***
     * 设置日志存放路径
     * @param path
     */
    public void setFile(String path) {
        filePath = path;
    }

    public void stopWrite() {
        canWrite = false;
        writeThread = null;
    }

    public void start() {
        newThread();
        writeThread.start();
    }

    /***
     * 设置日志等级;
     * STDOUT, NONE, BORTH, ONLY_FILE
     * @param cls 只支持上面四个中的一种
     */
    public void setRecordCls(int cls) {
        logCls = cls;
    }

    public void log(String msg) {
        if (!Config.BRECORD_LOG)
            return;
        synchronized (mLock) {
            String aa = mFormate.format(java.util.Calendar.getInstance().getTime());
            msgQueue.add(aa + msg);
            mLock.notify();
        }
    }

    public void log(byte[] data) {
        if (!Config.BRECORD_LOG)
            return;
        final String msg = bytesToHexString(data);
        log(msg);
    }

    /**
     * 把字节数组转换成16进制字符串
     * @param bArray
     * @return
     */
    public static final String bytesToHexString(byte[] bArray) {
        StringBuffer sb = new StringBuffer(bArray.length);
        /*String aa = mFormate.format(java.util.Calendar.getInstance().getTime());
        sb.append(aa);*/
        String sTemp;
        for (int i = 0; i < bArray.length; i++) {
            if (i%16 == 0)
                sb.append('\n');
            sTemp = Integer.toHexString(0xFF & bArray[i]);
            if (sTemp.length() < 2)
                sb.append(0);
            sb.append(sTemp.toUpperCase());
            sb.append(' ');
        }
        return sb.toString();
    }

    public static final String bytesToHexString(byte[] bArray, int len) {
        StringBuffer sb = new StringBuffer(bArray.length);
        /*String aa = mFormate.format(java.util.Calendar.getInstance().getTime());
        sb.append(aa);*/
        if (len >= bArray.length) {
            len = bArray.length;
        }
        String sTemp;
        for (int i = 0; i < len; i++) {
            if (i%16 == 0)
                sb.append('\n');
            sTemp = Integer.toHexString(0xFF & bArray[i]);
            if (sTemp.length() < 2)
                sb.append(0);
            sb.append(sTemp.toUpperCase());
            sb.append(' ');
        }
        return sb.toString();
    }

    /***
     * 写入到文件
     * @param msg
     */
    private void writeToFile(final String msg) {

        if (mFw == null) {
            try {
                mFw = new FileWriter(filePath, true);
                mBuffer = new BufferedWriter(mFw);
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
        try {
            if (mBuffer == null)
                return;
            mBuffer.write(msg);
            mBuffer.newLine();
            mBuffer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /***
     * 销毁
     */
    public void destroy() {
        try {
            if (mBuffer != null)
                mBuffer.close();
            if (mFw != null)
                mFw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Thread writeThread = null;

    private void newThread() {
        if (writeThread == null) {
            writeThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    while(canWrite == true) {

                        synchronized (mLock) {
                            if(msgQueue.isEmpty()) {
                                try {
                                    mLock.wait();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            final String msg = msgQueue.poll();
                            switch(logCls) {
                                case STDOUT:
                                    android.util.Log.d(TAG, msg);
                                    break;
                                case BORTH:
                                    android.util.Log.d(TAG, msg);
                                    writeToFile(msg);
                                    break;
                                case ONLY_FILE:
                                    writeToFile(msg);
                                    break;
                                case NONE:
                                    break;
                                default:
                                    android.util.Log.d(TAG, msg);
                                    break;
                            }

                        }

                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            writeThread.setPriority(Thread.NORM_PRIORITY);
        }
    }
}
