package com.serial.opt;


import android.os.Handler;

import com.ilin.atelec.IAtCmd;
import com.ilin.util.FileUtil;
import com.serial.port.ByteUtils;

import java.io.File;

/**
 * 充电桩数据协议完整性校验
 * Created by fanjc on 2022/2/15 005.
 */
public class AtProtocal implements IUartProtocal {

    private static final JLog LOG = new JLog("AtProtocal", IAtCmd.isLogOn, JLog.TYPE_INFO);
    /**打错误信息而传的*/
    private Handler mHandler;

    private final int PKG_MIN_LEN = 6;

    private File serialFile;

    public void setSerialFile(File f) {
        serialFile = f;
    }

//    public void setHandler(Handler h) {
//        mHandler = h;
//    }

    @Override
    public int checkCompleteProtocal(final byte[] data, final int start, final int len) {
        // 校验包的完整性 收到的数据
        final int lenIndex = start + len - 1;
        if (len >= PKG_MIN_LEN) {
            if (data[lenIndex] == IAtCmd.RSP_END2 && data[lenIndex - 1] == IAtCmd.RSP_END1
                    //&& data[lenIndex - 2] == 'D' && data[lenIndex - 3] == 'N' && data[lenIndex - 4] == 'E'
            ) { // 命令返回
                return IUartProtocal.CHECK_COMPLETE;
            } else if (len >  350) {
                if (serialFile != null) {
                    FileUtil.append(serialFile, ByteUtils.byteArrayToHexString(data));
                }
                return IUartProtocal.CHECK_BAD_PROTOCAL;
            }
            else
                 return IUartProtocal.CHECK_UNCOMPLETE;
        }
        else
            return IUartProtocal.CHECK_UNCOMPLETE;
    }

    /***
     * 获取请求或响应头数据
     * @param data
     * @return
     */
    public static String getResult(byte[] data) {
        try {
            String str = new String(data, "utf-8");
            return str;
        }
        catch (Exception e) {
            LOG.print(e.getMessage());
            return "错误，字符编码不是UTF-8！";
        }
    }

}
