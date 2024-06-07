package com.ilin.bean;

import com.serial.port.ByteUtils;

/***
 * 充电桩查出来的信息
 */
public class StubInfo {
    /**桩标识*/
    private String id;
    /**桩类型*/
    private byte stubType;
    /**厂商 id*/
    private int factoryId;
    /**magic number*/
    private String magicNumber;
    /**桩信息*/
    private SoftInfo softInfo;
    /**充电模式*/
    private byte chargeMode;

    /**当前模式设置时间*/
    private ModeTime modeTime;
    /**音量大小*/
    private int vol;
    /**充电桩时间, 秒数*/
    private int time;
    /**（枪号+状态）的长度 2的倍数*/
    private int stateLen;
    /**枪状态:低字节为枪端口号，高字节为枪状态*/
    private byte[] noStates;

    /**输入电压A,分辨率：0.1v*/
    private int iva;
    /**输入电压B,分辨率：0.1v*/
    private int ivb;
    /**输入电压C,分辨率：0.1v*/
    private int ivc;

    /**输入电流A,分辨率：0.1A*/
    private int iaa;
    /**输入电流B,分辨率：0.1A*/
    private int iab;
    /**输入电流C,分辨率：0.1A*/
    private int iac;

    /**输出电压A,分辨率：0.1v*/
    private int ova;
    /**输出电压B,分辨率：0.1v*/
    private int ovb;
    /**输出电压C,分辨率：0.1v*/
    private int ovc;

    /**输出电流A,分辨率：0.1A*/
    private int oaa;
    /**输出电流B,分辨率：0.1A*/
    private int oab;
    /**输出电流C,分辨率：0.1A*/
    private int oac;

    /**CP输出占空比,单位：%,分辨率：0.1*/
    private int cpRate;
    /**CP输出幅值，分辨率：0.01v*/
    private int cpOut;

    /**CC电阻值，分辨率：1欧*/
    private int ccM;

    /**故障状态*/
    private int errCode;
    /**充电电量,分辨率：0.01kwh*/
    private int chargeCap;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public byte getStubType() {
        return stubType;
    }

    public void setStubType(byte stubType) {
        this.stubType = stubType;
    }

    public int getFactoryId() {
        return factoryId;
    }

    public void setFactoryId(int factoryId) {
        this.factoryId = factoryId;
    }

    public String getMagicNumber() {
        return magicNumber;
    }

    public void setMagicNumber(String magicNumber) {
        this.magicNumber = magicNumber;
    }

    public SoftInfo getSoftInfo() {
        return softInfo;
    }

    public void setSoftInfo(SoftInfo softInfo) {
        this.softInfo = softInfo;
    }

    public byte getChargeMode() {
        return chargeMode;
    }

    public void setChargeMode(byte chargeMode) {
        this.chargeMode = chargeMode;
    }

    public ModeTime getModeTime() {
        return modeTime;
    }

    public void setModeTime(ModeTime modeTime) {
        this.modeTime = modeTime;
    }

    public int getVol() {
        return vol;
    }

    public void setVol(int vol) {
        this.vol = vol;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getStateLen() {
        return stateLen;
    }

    public void setStateLen(int stateLen) {
        this.stateLen = stateLen;
    }

    public byte[] getNoStates() {
        return noStates;
    }

    public void setNoStates(byte[] noStates) {
        this.noStates = noStates;
    }

    public int getIva() {
        return iva;
    }

    public void setIva(int iva) {
        this.iva = iva;
    }

    public int getIvb() {
        return ivb;
    }

    public void setIvb(int ivb) {
        this.ivb = ivb;
    }

    public int getIvc() {
        return ivc;
    }

    public void setIvc(int ivc) {
        this.ivc = ivc;
    }

    public int getIaa() {
        return iaa;
    }

    public void setIaa(int iaa) {
        this.iaa = iaa;
    }

    public int getIab() {
        return iab;
    }

    public void setIab(int iab) {
        this.iab = iab;
    }

    public int getIac() {
        return iac;
    }

    public void setIac(int iac) {
        this.iac = iac;
    }

    public int getOva() {
        return ova;
    }

    public void setOva(int ova) {
        this.ova = ova;
    }

    public int getOvb() {
        return ovb;
    }

    public void setOvb(int ovb) {
        this.ovb = ovb;
    }

    public int getOvc() {
        return ovc;
    }

    public void setOvc(int ovc) {
        this.ovc = ovc;
    }

    public int getOaa() {
        return oaa;
    }

    public void setOaa(int oaa) {
        this.oaa = oaa;
    }

    public int getOab() {
        return oab;
    }

    public void setOab(int oab) {
        this.oab = oab;
    }

    public int getOac() {
        return oac;
    }

    public void setOac(int oac) {
        this.oac = oac;
    }

    public int getCpRate() {
        return cpRate;
    }

    public void setCpRate(int cpRate) {
        this.cpRate = cpRate;
    }

    public int getCpOut() {
        return cpOut;
    }

    public void setCpOut(int cpOut) {
        this.cpOut = cpOut;
    }

    public int getCcM() {
        return ccM;
    }

    public void setCcM(int ccM) {
        this.ccM = ccM;
    }

    public int getErrCode() {
        return errCode;
    }

    public void setErrCode(int errCode) {
        this.errCode = errCode;
    }

    public int getChargeCap() {
        return chargeCap;
    }

    public void setChargeCap(int chargeCap) {
        this.chargeCap = chargeCap;
    }
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(256);

        sb.append("桩号：").append(id).append('\n');
        sb.append("桩类型：").append(stubType).append('\n');
        sb.append("厂商：").append(factoryId).append('\n');
        sb.append("Magic Number：").append(magicNumber).append('\n');

        sb.append("通信协议版本，主版本：").append(softInfo.protocal & 0xff00 >> 8)
                .append("，次版本：").append(softInfo.protocal & 0xff).append('\n');

        sb.append("设备嵌入式软件版本，修订版本号:").append(softInfo.v1)
                .append("，次版本号：").append(softInfo.v2)
                .append("，主版本号：").append(softInfo.v3).append('\n');

        sb.append("硬件版本号：").append(softInfo.hdVer).append('\n');

        sb.append("充电模式：").append(chargeMode).append("(0立即, 1预约, 2免刷)").append('\n');

        sb.append("当前模式设置时间：").append(modeTime.hour).append(':').append(modeTime.minute).append('\n');

        sb.append("音量大小：").append(vol).append('\n');

        sb.append("充电桩时间：").append(time).append('\n');

        sb.append("枪号+状态的长度：").append(stateLen).append('\n');
        sb.append("枪状态：").append(ByteUtils.byteArrayToHexString(noStates)).append('\n');

        sb.append("输入电压A：").append(iva).append('\n');
        sb.append("输入电压B：").append(ivb).append('\n');
        sb.append("输入电压C：").append(ivc).append('\n');

        sb.append("输入电流A：").append(iaa).append('\n');
        sb.append("输入电流B：").append(iab).append('\n');
        sb.append("输入电流C：").append(iac).append('\n');

        sb.append("输出电压A：").append(ova).append('\n');
        sb.append("输出电压B：").append(ovb).append('\n');
        sb.append("输出电压C：").append(ovc).append('\n');

        sb.append("输出电流A：").append(oaa).append('\n');
        sb.append("输出电流B：").append(oab).append('\n');
        sb.append("输出电流C：").append(oac).append('\n');

        sb.append("CP输出占空比：").append(cpRate * 0.1).append('%').append('\n');
        sb.append("CP输出幅值：").append(cpOut * 0.01).append('V').append('\n');
        sb.append("CC电阻值：").append(ccM).append("欧").append('\n');

        sb.append("故障状态：").append(errCode).append('\n');
        sb.append("充电电量：").append(chargeCap * 0.01).append("kwh").append('\n');

        return sb.toString();
    }
}
