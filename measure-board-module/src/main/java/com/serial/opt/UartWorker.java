package com.serial.opt;


import com.serial.port.ByteUtils;

import java.io.IOException;

import android_serialport_api.Command;

/***
 * 通用串口工作类
 */
public class UartWorker
{
    private static final JLog LOG = new JLog("UartWorker", true, JLog.TYPE_DEBUG);

    private int isRunningFlag;

    private UartCommunication mUartCommunication;

    private OnDistributeListener mDistributeListener;


    /***
     * 读取线程
     */
    private Runnable mReadRunnable = new Runnable()
    {
        StringBuffer strBuf = new StringBuffer();
        @Override
        public void run()
        {
//            byte[] data = new byte[2048];
            byte[] data = new byte[1024];
            int flag = isRunningFlag;
            while (isRunningFlag == flag)
            {
                System.out.println("UartWorker.read0 = " + mUartCommunication);
                if (mUartCommunication != null)
                {
                    try
                    {
                        int size = mUartCommunication.readData(data, 0, data.length);
                        System.out.println("UartWorker.read1 = " + size);
                        if (size > 0)
                        {
                            byte[] temp = new byte[size];
                            System.arraycopy(data, 0, temp, 0, size);
                            String item = ByteUtils.byteArrayToHexString(temp);
                            strBuf.append(item);
                            System.out.println("UartWorker.read2 = " + item);
                            checkData(strBuf);

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

    // 验证收到数据的完整性，及分发
    private void checkData(StringBuffer strBuf) {
        int start = 0, end = 0;
        if ((start = strBuf.indexOf(Command.FLAG)) >= 0){
//            end = strBuf.indexOf(Command.FLAG, start + 2);
            end = strBuf.lastIndexOf(Command.FLAG);
            if (end > 0){
                String itemData = strBuf.substring(start, end + 4);
                distributeData(itemData, "normal");
                strBuf.delete(start, end + 4); // 删除分发的数据
                if (start > 0){ // 有无效数据
                    System.err.println("UartWorker.errorData = " + strBuf.substring(0, start));
                    distributeData(strBuf.substring(0, start), "errorData");
                    strBuf.delete(0, start);
                }
            }
        }
    }

    // 分发
    private void distributeData(String data, String where) {
        if (mDistributeListener != null)
        {
            mDistributeListener.onDistribution(data);
        }
    }


    /***
     * 串口路径
     * @param uartFilePath 串口路径
     * @param rate 波特率
     * @param flags 是否有停止位
     */
    public UartWorker(String uartFilePath, int rate, int flags)
            throws SecurityException, IOException
    {
        this.mUartCommunication = UartCommunication.getInstance(uartFilePath, rate, flags);
    }

    public void setProtocalDistributeListener(OnDistributeListener listener)
    {
        this.mDistributeListener = listener;
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
        LOG.print("stop communicaate");
        this.isRunningFlag += 1;
    }

    public void release()
    {
        LOG.print("=====.release()");
        stopCommunicate();

        this.mReadRunnable = null;

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
    }

    public interface OnDistributeListener
    {
        /***
         * 处理一个完整的数据包
         * @param data
         */
        void onDistribution(String data);
    }
}
