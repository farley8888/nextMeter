package android_serialport_api;

/**
 * 上位机计价器相关指令
 */
public class Command {

    // 开始与结束位
    public static final String FLAG = "55AA";

    /**
     * 初始化
     */
    public static final String CMD_INIT = "55 AA 00 05 00 00 10 A9 90 2C 55 AA".replace(" ", "");

    /**
     * 营运开始
     *
     */
    public static final String CMD_START = "55 AA 00 0B 00 00 10 A0 20 23 02 25 20 47 30 C8 55 AA".replace(" ", "");


    /**
     * extras
     *
     */
    public static final String CMD_EXTRAS_1 = "55 AA 00 07 00 00 10 A2 00 01 00 B4 55 AA".replace(" ", "");


    /**
     * extras
     *
     */
    public static final String CMD_EXTRAS_10 = "55 AA 00 07 00 00 10 A2 00 10 00 A5 55 AA".replace(" ", "");


    /**
     * 暂停营运
     *
     */
    public static final String CMD_PAUSE = "55 AA 00 05 00 00 10 A1 01 B5 55 AA".replace(" ", "");


    /**
     * 继续运营
     *
     */
    public static final String CMD_CONTINUE = "55 AA 00 05 00 00 10 A1 00 B4 55 AA".replace(" ", "");


    /**
     * 营运结束
     *
     */
    public static final String CMD_END = "55 AA 00 0B 00 00 10 A3 20 23 02 25 20 47 01 FA 55 AA".replace(" ", "");

    /**
     * 营运数据应答
     * 收到运营数据时 55AA0031020100E4415830303031202020200002202302252047202302252119000000020000000000162500130000000000130001A355AA
     * result.startsWith("55AA") && result.endsWith("55AA") && result.length() > 16 && result.startsWith("E4", 14)
     * 发送
     */
    public static final String CMD_END_RESPONSE = "55 AA 00 05 00 00 00 E4 90 71 55 AA".replace(" ", "");

    public static final String CMD_PARAMETERS_ENQUIRY = "55 AA 00 0A 00 00 10 A4 20 23 02 25 20 03 B9 55 AA".replace(" ", "");

    public static final String CMD_UNLOCK = "55 AA 00 05 00 00 10 AA 90 B9 55 AA".replace(" ", "");

    public String dummyData = "55 AA 00 20 02 01 00 E2 80 41 58 30 30 30 31 20 20 20 20 23 02 02 01 23 01 01 A0 00 00 00 20 23 02 27 12 51 9D 55 AA";


//    /**
//     * 通过量获取打米命令
//     * @param num 单位g
//     *  自由设置必须大于100克
//     * @return
//     */
//    public static String getWeightCommand(int num){
//        String str = "01060050" ;
//        str += StringUtils.byteToHex(num);
//        str += CRC.getCRC(str);
//        return str.toUpperCase();
//    }


}
