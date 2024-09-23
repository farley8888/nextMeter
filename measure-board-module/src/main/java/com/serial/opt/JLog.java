package com.serial.opt;

/**
 */
public class JLog {
    boolean isPrint;
    int mDefaultType = 1;
    private String iTag = "base";
    public static final int TYPE_VERBOSE = 0;
    public static final int TYPE_INFO = 1;
    public static final int TYPE_ERROR = 5;
    public static final int TYPE_DEBUG = 3;
    public static final int TYPE_WARN = 4;

    public JLog() {
    }

    public JLog(String aTag, boolean isOn, int aDefaultType) {
        setPrint(isOn);
        setTag(aTag);
    }

    public void setPrint(boolean isOn) {
        this.isPrint = isOn;
    }

    public void setTag(String aTag) {
        this.iTag = aTag;
    }

    public void setDefaultType(int aType) {
        this.mDefaultType = aType;
    }

    public void print(String str) {
        print(str, this.mDefaultType);
    }

    public void print(String str, int aType) {
        if (this.isPrint) {
            switch (aType) {
                case 5:
                    android.util.Log.e(this.iTag, str);
                    break;
                case 1:
                    android.util.Log.i(this.iTag, str);
                    break;
                case 0:
                    android.util.Log.v(this.iTag, str);
                    break;
                case 3:
                    android.util.Log.d(this.iTag, str);
                    break;
                case 2:
                case 4:
                    android.util.Log.w(this.iTag, str);
                    break;
            }
        }
    }

    public void print(int value) {
        print(String.valueOf(value));
    }

    public void print(Object o) {
        print(o.toString());
    }

    public void print(boolean b) {
        print(b ? "true" : "false");
    }

    public void print() {
        print("");
    }

    public void print(long l) {
        print(String.valueOf(l));
    }

    public void print(byte[] b) {
        print(b, 0, b.length);
    }

    public void print(byte[] b, int offset, int len) {
        if (this.isPrint) {
            StringBuffer strBuf = new StringBuffer();
            for (int i = offset; i < len; i++) {
                String hex = Integer.toHexString(b[i] & 0xFF);
                if (hex.length() == 1) {
                    hex = '0' + hex;
                }
                strBuf.append(hex.toUpperCase());
            }
            print(strBuf.toString());
        }
    }
}
