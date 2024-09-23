package com.serial.opt;

/**
 * Created by fanjc on 2016/7/5 005.
 */
public interface IUartProtocal
{
    int CHECK_COMPLETE = 1;
    int CHECK_UNCOMPLETE = -1;
    int CHECK_BAD_PROTOCAL = -2;
    int CHECK_INGORE = -3;

    /***
     * 校验包的完整性
     * @param data
     * @param start
     * @param len
     * @return
     */
    public int checkCompleteProtocal(final byte[] data, final int start, final int len);
}
