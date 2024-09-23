package com.ilin.atelec;

public interface ICmd {

    boolean isLogOn = true;

    byte HEAD_4 = (byte)0xAA;
    byte END_4 = (byte)0x55;
    /**发送命令的请求头*/
    byte REQ_HEAD = (byte)0x68;
    /**发送命令的响应头*/
    byte RSP_HEAD = (byte)0xAA;
    /**签名参数*/
    byte SIGN_D = 0x00;
    /**查询系统信息*/
    byte CMD_QUERY_INFO = (byte)0xB1;
    /**立即充电*/
    byte CMD_IM_CHARGE = (byte)0xB4;
    /**结束充电*/
    byte CMD_END_CHARGE = (byte)0xB6;
    /**启动远程升级*/
    byte CMD_REMOTE_UPGRADE = (byte)0xB9;
    /**请求下传升级包*/
    byte CMD_DOWN_PKG = (byte)0xBA;
    /**升级包下传成功*/
    byte CMD_DOWN_OK = (byte)0xBB;
    /**请求历史故障码*/
    byte CMD_HIS_ERR = (byte)0xBE;
    /**显示错误信息*/
    int W_MSG_DISPLAY = 99;

    byte CODE_OK = 0;
    /**请求格式错误，解析出错*/
    byte CODE_ST_ERR = 0x11;
    /**未授权/无权限，权限验证时使用*/
    byte CODE_NOT_ALLOW = 0x12;
    /**拒绝提供服务，一般存在以下情况：
     1. 设备做CheckSum校验不通过
     2. 充电桩号识别不出*/
    byte CODE_REFUSE = 0x13;
    /**指令不存在*/
    byte CODE_NOT_EXIST = 0x14;
    /**当前设备不支持该指令*/
    byte CODE_NO_SUPPORT = 0x15;
    /**设备内部出错*/
    byte CODE_DV_ERR = 0x50;
    /**指令执行失败*/
    byte CODE_EX_FAIL = 0x51;
}
