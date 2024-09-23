package android_serialport_api;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * 接口通信类，232接口，全双工模式
 */
public class SerialUtils {

    final String TAG = SerialUtils.class.getSimpleName();
    private SerialPort mSerialPort;
    private OutputStream mOutputStream;
    private InputStream mInputStream;

    private static SerialUtils mSerialUtils;


    private SerialUtils() {

    }

    public static SerialUtils getInstance() {
        if (null == mSerialUtils) {
            mSerialUtils = new SerialUtils();
        }
        return mSerialUtils;
    }

    boolean openResult = false;
    //打开串口通信通道
    public boolean openGPIO() throws InterruptedException {
        Thread td = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                    boolean isSuc = openSerial();
                    openResult = isSuc;
                    Log.e(TAG, "run: 串口通信 = " + (isSuc ? "成功":"失败") );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        td.start();
        td.join();
        return openResult;
    }


    private  boolean openSerial() {
        int iBaudRate = 115200;
        String sPort = "/dev/ttyS1"; //S1~S4

        closeSerial();
        try {
            mSerialPort = new SerialPort(new File(sPort), iBaudRate, 0);
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void closeSerial() {
        try {
            if (mSerialPort != null) {
                //Log.e("keven","========bu deng yu null");
                mSerialPort.close();
                mSerialPort = null;
                mOutputStream.close();
                mOutputStream = null;
                mInputStream.close();
                mInputStream = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /********yuzhiwang*******/

    public static int isOdd(int paramInt) {
        return paramInt & 0x1;
    }

    public static byte HexToByte(String paramString) {
        return (byte) Integer.parseInt(paramString, 16);
    }

    public static byte[] HexToByteArr(String paramString) {
        int i = paramString.length();
        //  byte[] arrayOfByte = null;
        if (isOdd(i) == 1) {
            i += 1;


            paramString = paramString + "0";
        }
        byte[] arrayOfByte = new byte[i / 2];


        for (int t = 0; t < arrayOfByte.length; t++) {
            try {
                arrayOfByte[t] = (byte) (0xff & Integer.parseInt(paramString.substring(
                        t * 2, t * 2 + 2), 16));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return arrayOfByte;
    }

    /********end************/

    public void sendData(String sendDate) {
//        Log.e("perryn", "发送的命令:" + sendDate);
        try {
            if (mOutputStream != null) {
                mOutputStream.write(HexToByteArr(sendDate));
            }
        } catch (Exception e) {
            Log.e(TAG, sendDate + ".sendData: 发命令异常 = "+e.getMessage());
            e.printStackTrace();
        }
    }
    public void sendDataNotTry(String sendDate) throws IOException {
        if (mOutputStream != null) {
            mOutputStream.write(HexToByteArr(sendDate));
        }
    }

    public byte[] readData() {
        byte[] byffer = null;

        for (int i = 0; i < 10; i++) {
            try {
                if (mInputStream != null) {
                    int bufferNum = 0;
                    if (mInputStream.available() > 50) {
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
                        Thread.sleep(200);
                    }
                }
                if (i == 9) {
                    break;
                }
            } catch (Exception e) {
                Log.e(TAG, "readData: 读命令异常 = " + e.getMessage());
                e.printStackTrace();
            }
        }
        return byffer;
    }

    public void Write2File(File file, String mode) {
        if ((file == null) || (!file.exists()) || (mode == null)) {
            if (file == null) Log.d("keven", "file null");
            if (!file.exists()) Log.d("keven", "file no exitsts");
            if (mode == null) Log.d("keven", "mode null");
            return;
        }
        try {
            FileOutputStream fout = new FileOutputStream(file);
            PrintWriter pWriter = new PrintWriter(fout);
            pWriter.println(mode);
            pWriter.flush();
            pWriter.close();
            fout.close();
        } catch (IOException re) {
            Log.d("keven", "write error:" + re);
            return;
        }
    }


}
