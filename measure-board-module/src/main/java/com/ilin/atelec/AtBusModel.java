package com.ilin.atelec;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.serial.opt.AtProtocal;
import com.serial.opt.UartWorkerCH;
import com.serial.opt.UartWorkerCH.OnReceiveListener;

import java.io.File;
import java.io.IOException;


/***
 * At指令业务处理类
 */
public class AtBusModel {

    private UartWorkerCH uartWorker = null;
    private AtProtocal atProtocal = null;

    final String TAG = getClass().getSimpleName();
    private Context mCtx;

    private File serialFile;

    private static AtBusModel instance;

    public static AtBusModel getInstance(final Context ctx) {
        if (null == instance) {
            instance = new AtBusModel(ctx);
        }
        return instance;
    }

    public void setSerialFile(File f) {
        serialFile = f;
    }

    private UartWorkerCH.OnReceiveListener mListener;

    /****
     * 收到数据后应该先编码或判断是否接受完，再转发
     */
    private UartWorkerCH.OnReceiveListener selfListener = new UartWorkerCH.OnReceiveListener() {
        StringBuffer sb = new StringBuffer();
        @Override
        public void onReceive(String data) {
            sb.append(data);
            if (data.contains("\r\n")){
                if (mListener != null) mListener.onReceive(sb.toString());
                sb = new StringBuffer();
            }
//            String result = hexToAscii(data);
//            String oa = hexToAscii("0D0A"); = \r\n
        }
    };

    /***
     * 初始化串口
     * @param path
     * @param bitRate
     */
    public void init(final String path, int bitRate) {
        if (uartWorker != null) {
            uartWorker.release();
        }
        System.out.println("path = " + path + ", bitRate = " + bitRate);
        atProtocal = new AtProtocal();
        atProtocal.setSerialFile(serialFile);
        try {
//            uartWorker = new UartWorker(path, bitRate, 0, atProtocal);
            uartWorker = new UartWorkerCH(path, bitRate, 0, "At");
            Log.i(TAG, path + " 串口打开成功");
        }
        catch (SecurityException e) {
            Log.i(TAG, "打开串口失败，安全问题，即权限不够！");
            Toast.makeText(mCtx, "打开串口失败，安全问题，即权限不够！", Toast.LENGTH_SHORT).show();
        }
        catch (IOException e) {
            Log.i(TAG, "打开串口失败，不存在！" + e.getMessage());
            e.printStackTrace();
            Toast.makeText(mCtx, "打开串口失败，异常发生了", Toast.LENGTH_SHORT).show();
        }

        if (null != uartWorker) {
            uartWorker.setIsHexResult(false); // At结果非16进制
            uartWorker.startCommunicate();
            uartWorker.setOnReceiveListener(selfListener);
        }
    }


    public void setRspLs(UartWorkerCH.OnReceiveListener listener){
        mListener = listener;
    }

    public void restRspLs(){
        mListener = null;
    }

    /**
     * 16进制转Ascii
     * @param hexStr
     * @return
     */
    public static String hexToAscii(String hexStr) {
        StringBuilder output = new StringBuilder("");
        for (int i = 0; i < hexStr.length(); i += 2) {
            String str = hexStr.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }
        return output.toString();
    }

    /***
     * 应该是单例的
     * activity 的context
     * @param ctx
     *
     */
    private AtBusModel(final Context ctx) {
        mCtx = ctx;
//        mHandler = handler;
    }

    /***
     * 设置桩号
     * @param no 状态
     */
    public void setStubNo(String no) {
        StringBuffer cmdStr = new StringBuffer(128);
        cmdStr.append(IAtCmd.REQ_STR).append("SN-").append(no);
        if (IAtCmd.withEnd) {
            cmdStr.append("\r\n");
        }
        try {
            //AtProtocal.genCheckCode(data);
            uartWorker.getWriter().writeData(cmdStr.toString().getBytes());
        }
        catch (Exception e) {
            android.util.Log.e("BusModelWriteError", e.getLocalizedMessage());
        }
    }

    public void stop() {
        if (uartWorker != null) {
            //uartWorker.stopCommunicate();
            uartWorker.release();
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
     * 信息查询
     */
    public void queryInfo() {
        StringBuffer cmdStr = new StringBuffer(64);
        cmdStr.append(IAtCmd.REQ_STR).append("SP?");
        if (IAtCmd.withEnd) {
            cmdStr.append("END\r\n");
        }
        try {
            //AtProtocal.genCheckCode(data);
            uartWorker.getWriter().writeData(cmdStr.toString().getBytes());
        }
        catch (Exception e) {
            android.util.Log.e("BusModelWriteError", e.getLocalizedMessage());
        }
    }

    /***
     * 设置充电桩参数
     * @param p
     */
    public void setParam(String p) {
        StringBuffer cmdStr = new StringBuffer(64);
        cmdStr.append(IAtCmd.REQ_STR).append("PARAM-").append(p);
        if (IAtCmd.withEnd) {
            cmdStr.append("\r\n");
        }
        try {
            uartWorker.getWriter().writeData(cmdStr.toString().getBytes());
        }
        catch (Exception e) {
            android.util.Log.e("BusModelWriteError", e.getLocalizedMessage());
        }
    }

    public void clear() {
        StringBuffer cmdStr = new StringBuffer(64);
        cmdStr.append(IAtCmd.REQ_STR).append("QCLEAR");
        if (IAtCmd.withEnd) {
            cmdStr.append("\r\n");
        }
        try {
            uartWorker.getWriter().writeData(cmdStr.toString().getBytes());
        }
        catch (Exception e) {
            android.util.Log.e("BusModelWriteError", e.getLocalizedMessage());
        }
    }

    public void getErr() {
        StringBuffer cmdStr = new StringBuffer(64);
        cmdStr.append(IAtCmd.REQ_STR).append("ERROR?");
        if (IAtCmd.withEnd) {
            cmdStr.append("\r\n");
        }
        try {
            uartWorker.getWriter().writeData(cmdStr.toString().getBytes());
        }
        catch (Exception e) {
            android.util.Log.e("BusModelWriteError", e.getLocalizedMessage());
        }
    }

    /*
     * tcp start
     */

    /**
     * 第 1 步：设置 APN
     * @param listener
     */
    public void setAPN(OnReceiveListener listener){
        setRspLs(listener); // spn
        //CID,上下文类型
        StringBuffer cmdStr = new StringBuffer("AT+QIPCSGP=1,1,\"CMNET\"");
        write(cmdStr);  // spn
        // OK
    }

    /**
     * 第 2 步：激活上下文
     * @param listener
     */
    public void actionQIP(OnReceiveListener listener){
        setRspLs(listener); // qip
        // CID,需与第 1 步的 CID 保持一致
        StringBuffer cmdStr = new StringBuffer("AT+QIPACT=1");
        write(cmdStr);  // spn
        // +QIPACTURC: 1,1,"10.155.69.240" //模块获取到 IP
        // +QIPACTURC: 1,1,"10.133.238.210"
    }

    /**
     * 第 3 步：建立 socket 连接，最多可连接 6 路
     * @param listener
     */
    public void crateSocket(OnReceiveListener listener){
        setRspLs(listener); // create1
        StringBuffer cmdStr = new StringBuffer(64);
//        // CID,socket ID,TCP 连接,服务器地址, 服务器端口,本地端口,连接类型为 TCP,访问模式为消息到来时直接上报
//        cmdStr.append("AT+QIPOPEN=1,1,\"TCP\",\"81.71.20.151\",22251,0"); // 只用一路链接直接填0就可以了
        cmdStr.append("AT+QIPOPEN=1,1,\"TCP\",\"116.211.154.180\",80,0"); // 只用一路链接直接填0就可以了
//        cmdStr.append("AT+QIPOPEN=1,1,\"TCP\",\"81.71.20.151\",22251,12341,1");
        write(cmdStr);  // create1
        // 返回 +QIPOPEN: 1,0

//        StringBuffer cmd2 = new StringBuffer(64);
        // CID,socket ID,TCP 连接,服务器地址, 服务器端口,本地端口,连接类型为 TCP,访问模式为消息到来时上报通知
//        cmd2.append("AT+QIPOPEN=2,2,\"TCP\",\"81.71.20.151\",22251,12342,0");
//        write(cmd2);  // create2
        // 返回 +QIPOPEN: 2,0
    }

    /**
     * 第 4 步：发送数据
     * @param listener
     * @param qIp 第几路；1~6
     */
    public void sendData(OnReceiveListener listener, String param, int qIp){
        setRspLs(listener); // send
        StringBuffer buffer = new StringBuffer();
        if (qIp == 1){
            buffer.append("AT+QIPSEND=1");        // 向第 1 路连接发送数据
            if (param != null){
                buffer.append(param);
            }else {
//            buffer.append(">1234567890<CTRL+Z>"); // 数据内容不回显
                buffer.append(">1234567890"); // 数据内容不回显
            }
            buffer.append("+QIPSEND:1,10");       // socket ID，发送数据长度
        } else if (qIp == 2) {
            buffer.append("AT+QIPSEND=2");        // 向第 2 路连接发送数据
            if (param != null) buffer.append(param);
            else buffer.append(">ABCDEFGHIJKLMNOPQRSTUVWXYZ"); //
            buffer.append("+QIPSEND:2,26");       //
        }
        else {
            Log.e(TAG, "sendData: not qid = " + qIp);
            return;
        }
        write(buffer); // send
    }

    /**
     * 第 5 步：接收数据
     * @param listener
     */
    public void readData(OnReceiveListener listener){
        setRspLs(data -> {
//            restRspLs();
            // QIPREAD: 10      //第 2 路连接有 10 个未读数据
            StringBuffer read2 = new StringBuffer(64);
            // AT+QIPREAD=2,10  //读取第 2 路的 10 个数据
            read2.append("AT+QIPREAD=2,").append(data.substring(data.indexOf(",") + 1));
           // read2.append("AT+QIPREAD=2,8"); //只读取前 8 个数据
            setRspLs(listener);
            write(read2);  // read1
        }); // read
        StringBuffer cmdStr = new StringBuffer(64);
        cmdStr.append("AT+QIPREAD=2");
        write(cmdStr);  // read1

        /*
         * AT+QIPREAD=2 //读取第 2 路剩余数据长度
         * +QIPREAD: 14 //还有 14 个数据未读
         */
    }

    /**
     * 第 6 步：关闭 socket 连接
     *
     */
    public void closeSocket(){
        StringBuffer buffer = new StringBuffer("AT+QIPCLOSE=1");
        setRspLs(data -> {
            // +QIPCLOSE: 2 OK 关闭成功
            Log.w(TAG, "closeSocket: = " + data.contains("OK") );
        });
        write(buffer); // closeSocket
    }

    /**
     * 第 7 步：断开 TCP/IP 连接
     */
    public void closeTcpIp(){
        StringBuffer buffer = new StringBuffer("AT+QIPDEACT=1");
        setRspLs(data -> {
            // +QIPACTURC: 1,0,"0.0.0.0 OK
            Log.w(TAG, "closeTcpIp: = " + data.contains("OK") );
        });
        write(buffer); // closeTcp
    }
    /*
    tcp end
     */

    // http start
    /**
     * 初始化HTTP服务
     * @param listener
     */
    public void http0Init(OnReceiveListener listener){
        setRspLs(listener); // http
        StringBuffer cmdStr = new StringBuffer("AT+HTTPINIT");
        write(cmdStr);  // http
    }

    /**
     * 设置URL等参数
     * @param listener
     * @param url
     */
    public void http1SetUrl(OnReceiveListener listener, String url){
        setRspLs(listener); // seturl
        //AT+HTTPPARA="URL","www.sust.xin/login"
        StringBuffer cmdStr = new StringBuffer("AT+HTTPPARA=\"URL\",");
        cmdStr.append("\"").append(url).append("\"");
        write(cmdStr);  // seturl
    }

    /**
     * 设置post请求的参数大小（字节）和输入时间（毫秒）
     * @param listener
     * @param length
     */
    public void httpTest(OnReceiveListener listener, int length){
        setRspLs(listener); // length
        // 设置post请求的参数大小（字节）和输入时间（毫秒）
//        StringBuffer cmdStr = new StringBuffer("AT+HTTPSND=0,0,0,http://203.156.205.55:8080/web/123.txt,'-H 'Connection: keep-alive");
        StringBuffer cmdStr = new StringBuffer("AT+HTTPSND=0,1,0,http://163.177.151.110/,\"-H'Connection:keep-alive'\"");
//        StringBuffer cmdStr = new StringBuffer("AT+HTTPSND=0,1,0,https://www.baidu.com/,\"-H'Connection:keep-alive'\"");
        write(cmdStr);  // length
        /*
            返回download之后 开始输入post参数
            成功返回 OK
         */
    }

    /**
     * 进行HTTP的post请求
     * @param action 0:GET 1:POST 2:HEAD
     */
    public void http3Action(OnReceiveListener listener, int action){
        setRspLs(listener); // http action
        StringBuffer cmdStr = new StringBuffer("AT+HTTPACTION=");
        cmdStr.append(action);
        write(cmdStr);  // http action
        /*
        成功返回 OK
        然后等待响应
        +HTTPACTION:1,400,2111
        其中400为HTTP状态码 2111为响应内容的大小
         */
    }

    /**
     * 读取响应
     * @param size 内容的大小
     */
    public void http4Read(OnReceiveListener listener, int size){
        setRspLs(listener); // http read
        // 2111为响应内容的大小
        StringBuffer cmdStr = new StringBuffer("AT+HTTPREAD=0,");
        cmdStr.append(size);
        write(cmdStr);  // http read
    }

    /**
     * 最后关闭HTTP服务
     * @param listener
     */
    public void http5Close(OnReceiveListener listener){
        StringBuffer cmdStr = new StringBuffer("AT+HTTPTERM");
        write(cmdStr);  // http close
    }

    // http end

    /**
     * 查询socket是否连接成功
     * @return
     */
    public boolean hasSocket(OnReceiveListener listener){
        setRspLs(listener);
        write(new StringBuffer("AT+QIPOPEN?"));
        return false;
    }


    /*
     check start
     */
    /***
     * 是否有扩展4G模块
     */
    public void checkHas4G(UartWorkerCH.OnReceiveListener listener) {
        setRspLs(listener); // has 4g
        StringBuffer cmdStr = new StringBuffer(64);
        cmdStr.append("AT");
        write(cmdStr);  // has 4g
    }

    /***
     * SIM 卡是否在位
     */
    public void checkInsertCard(UartWorkerCH.OnReceiveListener listener) {
        setRspLs(listener); // check insert
        StringBuffer cmdStr = new StringBuffer(64);
        cmdStr.append("AT+CPIN?");
        write(cmdStr);  // ck insert
    }

    /***
     * 模块信号 应为 10 以上
     * 范围0-31之间
     */
    public void checkCSQ(UartWorkerCH.OnReceiveListener listener) {
        // +CSQ: 30,99 //第 1 位参数应为 10 以上
        setRspLs(listener); // check csq
        StringBuffer cmdStr = new StringBuffer(64);
        cmdStr.append("AT+CSQ");
        write(cmdStr);  // ck csq
    }

    /***
     * 模块是否正常注册
     */
    public void checkSysInfo(UartWorkerCH.OnReceiveListener listener) {
        // ^SYSINFO: 2,3,0,9,1 模块注册在 4G，具体参数含义请参考 AT 手册
        setRspLs(listener); // check sysInfo
        StringBuffer cmdStr = new StringBuffer(64);
        cmdStr.append("AT^SYSINFO");
        write(cmdStr);  // ck sys
    }

    /* check End
    以上几条命令返回值正常后，再参照本文档各模式下的步骤操作建立连接及传输数据。
    若返回不对，应检查相关卡及天线是否正常可用。确保卡、天线及网络环境无误后，
    再操作建立连接及传输数据。
     */

    public boolean write(StringBuffer data) {
        if (IAtCmd.withEnd){
            data.append("\r\n");
        }
        return write(data.toString());
    }

    public boolean write(String data) {
        try {
            boolean isOk = uartWorker.getWriter().writeData(data.getBytes());
            Log.i(TAG, "write: 写入数据 data = " + data);
            return isOk;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


}
