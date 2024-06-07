package com.ilin.atelec;

import com.ilin.util.FLog;
import com.ilin.util.FileUtil;

import java.io.File;

/**
 * 继承这个主要目的是为了方便把停止运行的日志打印到文件中去.
 * @author june
 *
 */
public class ElectricApp extends android.app.Application
{

	private final String LOG_FILE_NAME = "ElectricApp.log";

	private File atFile;

	@Override
	public void onCreate() 
	{
		super.onCreate();
		init();

	}

	private void init() {
		CrashHandler.getInstance().init(this);

		File f = new File(android.os.Environment.getExternalStorageDirectory().getAbsoluteFile(), "hireLog");
		if (!f.isDirectory())
			f.mkdirs();

		File f2 = new File(f, LOG_FILE_NAME);
		if (f2.exists() && f2.length() > 500 * 1024) {
			f2.renameTo(new File(f, LOG_FILE_NAME + new java.util.Random(20).nextInt()));
			f2 = null;
			f2 = new File(f, LOG_FILE_NAME);
		}
		String fName = FileUtil.newFileName(0);

		atFile = new File(f, fName);

		String path = f2.getAbsolutePath();
		FLog.getInstance().setFile(path);
		f2 = null;
		f = null;
		FLog.getInstance().setRecordCls(FLog.ONLY_FILE);
		FLog.getInstance().start();
		FLog.getInstance().log("ElectricApp init.");
	}

	public File getAtFile() {
		return atFile;
	}
}
