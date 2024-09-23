package com.ilin.atelec;

public interface IAtCmd {

    boolean isLogOn = true;

    boolean withEnd = true;

    /**显示错误信息*/
    int W_MSG_DISPLAY = 100;
    /**发送命令的响应头*/
    byte RSP_END1 = (byte)'\r';
    /**发送命令的响应头*/
    byte RSP_END2 = (byte)'\n';
    /**AT指令开头*/
    String REQ_STR = "AT+";
    /**设置桩号OK*/
    String SET_NO_OK = "ATSNOK END";

    /**查询故障时没有故障的回复*/
    String ERR_OK = "ErrorMatrix=0 END";

    /**授权成功 Authrization true END\r\n*/
    String CHARGE_AUTH_OK = "Authrization true END";




}
