package com.serial.opt;


import android.util.Log;

import java.io.IOException;

import android_serialport_api.SerialPort;

/**
 * Uart工作主类
 */
public class UartCommunication
{

    private static final JLog LOG = new JLog("UartCommunication", true, JLog.TYPE_INFO);

    private static java.util.WeakHashMap<String, UartCommunication> UartCommunicationMap = new java.util.WeakHashMap();

    private SerialPort mSerialPort;

    private java.io.InputStream mInputStream;
    private java.io.OutputStream mOutputStream;
    private java.util.concurrent.BlockingQueue<byte[]> mWriteBlock;
    private boolean mRunningFlag = true;

    private String keyPath = null;
    /**写线程*/
    private Runnable mWritingRunnable = new Runnable()
    {
        public void run()
        {
            while (UartCommunication.this.mRunningFlag)
            {
                try
                {
                    byte[] data = UartCommunication.this.mWriteBlock.take();
                    if (UartCommunication.this.mOutputStream != null)
                    {
                        synchronized (this) {
                            UartCommunication.this.mOutputStream.write(data, 0, data.length);
                            System.out.println("写入 = " + new String(data));
                            try { // 若串口反应没有这么快，则需要延时处理
                                Thread.sleep(9);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    };

    private int testReadIndex = 0;
    private byte[] testData = {
            -16, 5, 1, 1, 1, 0, 2, 6,
            -16, 4, 3, 5, 1, 0, 5,
            -16, 7, 4, 2, 1, 0, 1, 2, 3, 8 };

    private UartCommunication(String uartFilePath, int rate, int flags)
            throws SecurityException, IOException
    {
        keyPath = uartFilePath;
        this.mSerialPort = new SerialPort(new java.io.File(uartFilePath), rate, flags);
        //this.mSerialPort = SerialPortManager.openPort(uartFilePath, rate);
        this.mInputStream = this.mSerialPort.getInputStream();
        this.mOutputStream = this.mSerialPort.getOutputStream();

        //this.mInputStream = SerialPortManager.getInputStream(this.mSerialPort);
        //this.mOutputStream = SerialPortManager.getOutputStream(this.mSerialPort);


        this.mWriteBlock = new java.util.concurrent.ArrayBlockingQueue(10, true);
        new Thread(this.mWritingRunnable).start();
    }

    public static UartCommunication getInstance(String uartFilePath, int rate, int flags)
            throws SecurityException, IOException
    {
        UartCommunication instance = (UartCommunication)UartCommunicationMap.get(uartFilePath);
        if (instance == null) {
            instance = new UartCommunication(uartFilePath, rate, flags);
            UartCommunicationMap.put(uartFilePath, instance);

        }
        return instance;
    }

    public void release()
    {
        if (this.mInputStream != null)
        {
            try {
                this.mInputStream.close();
            }
            catch (IOException e) {
                LOG.print("关闭串口read流出错！");
            }
            this.mInputStream = null;
        }
        if (this.mOutputStream != null)
        {
            try {
                this.mOutputStream.close();
            }
            catch (IOException e) {
                LOG.print("关闭串口写流出错！");
            }
            this.mOutputStream = null;
        }
        if (this.mSerialPort != null)
        {
            this.mSerialPort.close();
            this.mSerialPort = null;
        }
        this.mRunningFlag = false;

        if ((UartCommunicationMap != null) && (UartCommunicationMap.containsValue(this)))
            UartCommunicationMap.remove(keyPath);
        keyPath = null;
    }

    public boolean writeData(byte[] aData) throws IOException
    {
        try
        {
            this.mWriteBlock.put(aData);
            return true;
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    /***
     * 读取数据
     * @param aData
     * @param aBeginPos
     * @param aLength
     * @return
     * @throws IOException
     */
    protected synchronized int readData(byte[] aData, int aBeginPos, int aLength)
            throws IOException
    {
        if (this.mInputStream != null)
        {
            return this.mInputStream.read(aData, aBeginPos, aLength);
        }
        return 0;
    }

    protected synchronized byte[] readData()
    {
        byte[] byffer = null;
        for (int i = 0; i < 10; i++) {
            try {
                if (mInputStream != null) {
                    int bufferNum = 0;
                    if (mInputStream.available() < 50) {
                        bufferNum = 50;
                    } else {
                        bufferNum = mInputStream.available();
                    }
                    byffer = new byte[bufferNum];

                    int size = mInputStream.read(byffer);
                    if (size > 0) {
                        break;
                    } else {
//                        System.out.println("SerialUtils.readData sleep 200");
                        Thread.sleep(100);
                    }
                }
                if (i == 9) {
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return byffer;
    }

    private int testReadData(byte[] aData, int aBeginPos, int aLength)
    {
        java.util.Random random = new java.util.Random();
        int size = random.nextInt(this.testData.length);

        for (int i = 0; i < size; i++)
        {
            aData[(aBeginPos + i)] = this.testData[(this.testReadIndex++)];
            if (this.testReadIndex >= this.testData.length)
            {
                this.testReadIndex = 0;
            }
        }
        try {
            Thread.sleep(5000L);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return size;
    }
}
