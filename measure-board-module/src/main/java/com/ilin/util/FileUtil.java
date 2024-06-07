package com.ilin.util;

import android.os.Environment;

import java.io.File;

/**
 * 文件工具类
 * Created by fanjc on 2016/7/11 011.
 */
public class FileUtil {

    public static final String EXT_DEFAULT = ".ili";
    /***
     * 不识别的can文件扩展名
     */
    public static final String EXT_CAN = ".can";
    /***行程文件扩展名*/
    public static final String EXT_TRAVEL = ".tra";
    /***测试结果文件名*/
    public static final String EXT_VOLT = ".rslt";
    /***
     * 文件名
     * @param type 0 默认, 1 can文件, 2 行程文件, 3 电压文件
     * @return
     */
    public static String newFileName(int type) {
        switch (type) {
            case 0:
                return System.currentTimeMillis() + EXT_DEFAULT;
            case 1:
                return System.currentTimeMillis() + EXT_CAN;
            case 2:
                return System.currentTimeMillis() + EXT_TRAVEL;
            case 3:
                return System.currentTimeMillis() + EXT_VOLT;
            default:
                return System.currentTimeMillis() + EXT_DEFAULT;
        }
    }

    /***
     * 获取文件，用于写测试结果
     * @param name
     * @return
     */
    public static File getFile(String name) {
        File f = new File(Environment.getExternalStorageDirectory(), name + EXT_VOLT);
        return f;
    }

    /***
     * 追加内容到文件中
     * @param f
     * @param content
     * @return
     */
    public static boolean append(java.io.File f, byte[] content) {
        if (null == content || content.length < 1)
            return true;
        java.io.RandomAccessFile randomFile = null;
        try {
            randomFile = new java.io.RandomAccessFile(f, "rws");
            long len = randomFile.length();
            randomFile.seek(len);
            randomFile.write(content);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (randomFile != null)
                try {
                    randomFile.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
        return true;
    }

    /***
     * 追加内容到文件中, 以行为单位即最后会加一个换行符
     * @param f
     * @param content
     * @return
     */
    public static boolean append(java.io.File f, String content) {
        if (null == content || content.length() < 1)
            return true;
        java.io.RandomAccessFile randomFile = null;
        try {
            String lineEnd = System.getProperty("line.separator");
            randomFile = new java.io.RandomAccessFile(f, "rw");
            long len = randomFile.length();
            randomFile.seek(len);
            randomFile.writeBytes(content + lineEnd);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (randomFile != null)
                try {
                    randomFile.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
        return true;
    }

    /***
     * 追加内容到文件中, 以行为单位即最后会加一个换行符
     * @param f
     * @param content
     * @return
     */
    public static boolean appendNoClose(java.io.RandomAccessFile f, String content) {
        if (null == content || content.length() < 1)
            return true;
        try {
            String lineEnd = System.getProperty("line.separator");
            long len = f.length();
            f.seek(len);
            f.writeBytes(content + lineEnd);
            //f.writeChars(content + lineEnd);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
    /***
     * 追加内容到文件中
     * @param f
     * @param content
     * @return
     */
    public static boolean appendNoClose(java.io.RandomAccessFile f, byte[] content) {
        if (null == content || content.length < 1)
            return true;
        try {
            long len = f.length();
            f.seek(len);
            f.write(content);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    /***
     * 写入数据,并加换行符, 暂不关闭流
     * @param out
     * @param content
     */
    public static void writeLineNoClose(java.io.OutputStream out, String content) {
        try {
            out.write(content.getBytes());
            String lineEnd = System.getProperty("line.separator");
            if (lineEnd != null)
                out.write(lineEnd.getBytes());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /***
     * 判断目录是否存在若不存在则创建, 否则不做事情
     * @param dir
     */
    public static void createDir(String dir) {
        java.io.File f = new java.io.File(dir);
        if (!f.exists())
            f.mkdirs();
        f = null;
    }

    /***
     * 判断目录是否存在若不存在则创建, 否则不做事情
     * @param dir
     */
    public static void createDir(java.io.File dir) {
        if (!dir.exists())
            dir.mkdirs();
    }

    /***
     * 拷贝文件
     * @param src
     * @param dst
     */
    public static void copy(java.io.RandomAccessFile src, java.io.FileOutputStream dst) {
        byte[] buffer = new byte[1024];
        try {
            int len = 0;
            while ((len = src.read(buffer)) != -1) {
                dst.write(buffer, 0, len);
            }
            dst.flush();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            buffer = null;
            if (src != null)
                try {
                    src.close();
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }
            if (dst != null)
                try {
                    dst.close();
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }
        }
    }

    /***
     * 获取URL中的文件名称
     * @param url
     * @return
     */
    public static String getUrlFileName(String url) {
        if (null == url)
            return null;
        int start = url.lastIndexOf('/');
        if (start > -1) {
            start += 1;
        }
        else
            start = 0;
        String name = url.substring(start);
        return name;
    }

    /***
     * 读取数据从文件中
     * @param f
     * @param buff
     * @return 实际读取的长度, -2表示出异常了, -1表示到结尾了
     */
    public static int readData(java.io.InputStream f, byte[] buff) throws Exception {
        int len = f.read(buff);
        return len;
    }
    private static java.text.SimpleDateFormat sf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static String getTimeStr() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        return sf.format(c.getTime());
    }

    /***
     * 追加到文件
     * @param msg
     * @param f
     */
    public static void appendToFile(String msg, java.io.File f) {
        try {
            java.io.RandomAccessFile file = new java.io.RandomAccessFile(f, "rw");
            StringBuilder sb = new StringBuilder();
            sb.append(getTimeStr() + ":::").append(msg).append("\n");
            appendNoClose(file, sb.toString().getBytes());

            file.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /***
     * 追加内容到文件中, 以行为单位即最后会加一个换行符
     * @param randomFile
     * @param content
     * @return
     */
    public static void append(java.io.RandomAccessFile randomFile, String content) {
        if (null == content || content.length() < 1)
            return ;
        try {
            String lineEnd = System.getProperty("line.separator");
            randomFile.writeBytes(content + lineEnd);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
