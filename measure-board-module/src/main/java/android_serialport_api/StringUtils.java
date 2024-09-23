package android_serialport_api;

public class StringUtils {

    /**
     * 将一个整形化为十六进制，并以字符串的形式返回
     */
    public static String byteToHex(int n) {
        String s = Integer.toHexString(n);
        StringBuffer sb = new StringBuffer(s);
        String strInsert = "";
        if (s.length() < 4) {
            int len = 4 - s.length();
            for (int i = 0; i < len; i++) {
                strInsert += "0";
            }
        }
        sb.insert(0, strInsert);
        return sb.toString();
    }

    public static String bytesToHexString(byte[] bArr) {
        StringBuffer sb = new StringBuffer(bArr.length);
        String sTmp;
        for (int i = 0; i < bArr.length; i++) {
            sTmp = Integer.toHexString(0xFF & bArr[i]);
            if (sTmp.length() < 2) sb.append(0);
            sb.append(sTmp.toUpperCase());
        }
        return sb.toString();
    }

    public static String addZeroForNum(String str, int strLength) {
        int strLen = str.length();
        if (strLen < strLength) {
            while (strLen < strLength) {
                StringBuffer sb = new StringBuffer();
                sb.append("0").append(str);// 左补0
                // sb.append(str).append("0");//右补0
                str = sb.toString();
                strLen = str.length();
            }
        }

        return str;
    }

    public static String hexStrToByte(String hex) {
        //字符串形式的:16进制!
        //字符串形式十进制--作为桥梁!
        int sint= Integer.valueOf(hex, 16);
        //十进制在转换成二进制的字符串形式输出!
        String bin= Integer.toBinaryString(sint);
        String str = addZeroForNum(bin, 16);
        //输出!
//        Log.e("perryn", str);
        return str;
    }

    /**
     * 16进制转10进制
     * @param: [hex]
     * @return: int
     * @description: 按位计算，位值乘权重
     */
    public static int hexToDecimal(String hex){
        int outcome = 0;
        for(int i = 0; i < hex.length(); i++){
            char hexChar = hex.charAt(i);
            outcome = outcome * 16 + charToDecimal(hexChar);
        }
        return outcome;
    }

    /**
     * @param: [c]
     * @return: int
     * @description:将字符转化为数字
     */
    public static int charToDecimal(char c){
        if(c >= 'A' && c <= 'F')
            return 10 + c - 'A';
        else
            return c - '0';
    }

}
