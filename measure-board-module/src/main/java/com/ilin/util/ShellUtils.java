package com.ilin.util;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/***
 * 工具类
 */
public class ShellUtils {

    private final static String TAG = "ShellUtils";


    public static String execScriptCmd(String command, String path, boolean root) {
//        int userMode = UserModeManager.getCurrentUserMode();
//        UserModeManager.switchToUserMode(UserModeManager.SUPER_USER_MODE);
        File tempFile = null;
        String result = "";
        Log.i("execScriptCmd", command);
        try {
            tempFile = new File(path);
            tempFile.deleteOnExit();
            BufferedWriter br = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(tempFile)));
            br.write("#!/system/bin/sh\n");
            br.write(command);
            br.close();
            execShellCmd("su root chmod 777 "
                    + tempFile.getAbsolutePath());
            result = execShellCmd((root ? "su root " : "")
                    + tempFile.getAbsolutePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
//        UserModeManager.switchToUserMode(userMode);
        return result;
    }

    public static void execEcho(String command) {
        echo(new String[]{command});
    }
    public static void echo(String[] commands) {
        try {
            Process process = Runtime.getRuntime().exec("sh");
            DataOutputStream outputStream = new DataOutputStream(process.getOutputStream());
            for (String tmpCmd : commands) {
                Log.d(TAG,"tmpCmd: " +tmpCmd);
                outputStream.writeBytes(tmpCmd + "\n");
            }
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String execShellCmd(String command) {
        String result = "";
        try {
            Process process = Runtime.getRuntime().exec(command + "\n");
            DataOutputStream stdin = new DataOutputStream(
                    process.getOutputStream());
            DataInputStream stdout = new DataInputStream(
                    process.getInputStream());
            DataInputStream stderr = new DataInputStream(
                    process.getErrorStream());
            String line;
            while ((line = stdout.readLine()) != null) {
                result += line + "\n";
            }
            if (result.length() > 0) {
                result = result.substring(0, result.length() - 1);
            }
            while ((line = stderr.readLine()) != null) {
                Log.e("EXEC", line);
            }
            process.waitFor();
        } catch (Exception e) {
            e.getMessage();
        }
        Log.i(TAG, "execShellCmd= " + command +"; result =" + result);
        return result;
    }


}
