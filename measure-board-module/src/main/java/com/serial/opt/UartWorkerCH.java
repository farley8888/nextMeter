package com.serial.opt;

import android.util.Log;

import com.serial.port.ByteUtils;

import java.io.IOException;

import android_serialport_api.Command;
import android_serialport_api.StringUtils;

/***
 * 扩展串口工作类
 */
public class UartWorkerCH
{

    final String TAG = getClass().getSimpleName();
    private String mName, mTag;

    private CircleBuff mReadingCache;

    private static final int MCU_BUFFER_SIZE = 1024 * 4;

    private java.util.concurrent.BlockingQueue<byte[]> mBlockQueue = new java.util.concurrent.ArrayBlockingQueue(10);// 最多可放入10个包

    private int isRunningFlag;

    private UartCommunication mUartCommunication;

    private OnReceiveListener mReceiveListener;

    private boolean mResultIsHex = true; // 结果数据是否要转为16进制

    /***
     * 读取线程
     */
    private Runnable mReadRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            byte[] data = new byte[2048];
            int flag = isRunningFlag;
            while (isRunningFlag == flag)
            {
//                System.out.println(getTag() + ".read0 = " + mUartCommunication);
                if (mUartCommunication != null)
                {
                    try
                    {
                        int size = mUartCommunication.readData(data, 0, data.length);
                        System.out.println(getTag() + ".read = " + size);
                        if (size > 0)
                        {
                            byte[] temp = new byte[size];
                            System.arraycopy(data, 0, temp, 0, size);
//                            String result = ByteUtils.byteArrayToHexString(temp);
                            String result = StringUtils.bytesToHexString(temp);
//                            System.out.println( getTag() + ".read2 = " + result);
                            mBlockQueue.put(temp); // 一次性读完一条数据时
                        }
                        else
                        {
                            Thread.sleep(50L);
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    private void checkData(StringBuffer strBuf) {
        int start = 0, end = 0;
        if ((start = strBuf.indexOf(Command.FLAG)) >= 0){

        }
    }


    /***
     * 分发线程
     */
    private Runnable mDistributionRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            int flag = isRunningFlag;
            while (isRunningFlag == flag)
            {
                try
                {
                    byte[] data = (byte[]) mBlockQueue.take();
                    String disResult = "";
                    if (mResultIsHex){
                        disResult = ByteUtils.byteArrayToHexString(data);
                    }else {
                        disResult = new String(data);
                    }
                    System.out.println(getTag() + " 分发 Date = " + disResult);
                    if (mReceiveListener != null)
                    {
                        mReceiveListener.onReceive(disResult);
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    };

    /***
     * 串口路径
     * @param uartFilePath 串口路径
     * @param rate 波特率
     * @param flags 是否有停止位
     */
    public UartWorkerCH(String uartFilePath, int rate, int flags, String name)
            throws SecurityException, IOException
    {
        this.mName = name;
        this.mReadingCache = new CircleBuff(MCU_BUFFER_SIZE);
        this.mUartCommunication = UartCommunication.getInstance(uartFilePath, rate, flags);
    }

    public void setOnReceiveListener(OnReceiveListener listener)
    {
        this.mReceiveListener = listener;
    }

    public void setIsHexResult(boolean isHexResult){
        mResultIsHex = isHexResult;
    }

    public UartCommunication getWriter()
    {
        return this.mUartCommunication;
    }

    public void startCommunicate()
    {
        initWrokingThread();
    }

    public void stopCommunicate()
    {
        Log.e(getTag(), "stopCommunicate: ");
        this.isRunningFlag += 1;
    }

    private String getTag() {
        if (mName == null) return TAG;
        if (mTag == null) mTag = TAG + "." + mName;
        return mTag;
    }

    public void release()
    {
        stopCommunicate();
        if (this.mReadingCache != null)
        {
            this.mReadingCache.release();
            this.mReadingCache = null;
        }
        this.mReadRunnable = null;

        if (this.mBlockQueue != null)
        {
            this.mBlockQueue.clear();
            this.mBlockQueue = null;
        }
        this.mDistributionRunnable = null;
        if (this.mUartCommunication != null)
        {
            this.mUartCommunication.release();
            this.mUartCommunication = null;
        }
    }

    /***
     * 初始化好读写分以线程
     */
    private void initWrokingThread()
    {
        new Thread(this.mReadRunnable).start();
//        new Thread(this.mAnalyseRunnable).start();
        new Thread(this.mDistributionRunnable).start();
    }

    public static interface OnReceiveListener
    {
        /***
         * 处理一个完整的数据包
         * @param data
         */
        void onReceive(String data);
    }
}
