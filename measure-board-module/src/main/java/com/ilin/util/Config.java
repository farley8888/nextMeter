package com.ilin.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 常量定义
 */
public class Config
{


	/**是否记录或打印日志*/
	public final static boolean BRECORD_LOG = true;
	/**司机数据保存的文件名称*/
	public final static String PREF = "dvData";

	/***
	 * 默认的波特率
	 */
	public final static int BATE = 115200;
	// 扩展的
	public final static int BATE_CH = 9600;

	/**
	 * 用于蓝牙
	 */
	public final static String SERIAL_COM = "/dev/ttyS1";

	/**
	 * 4G
	 */
	public final static String SERIAL_CH0 = "/dev/ttyWCH0";

	/**
	 * 计价版
	 */
	public final static String SERIAL_CH1 = "/dev/ttyWCH1";

	/**
	 * 外设1
	 * 单一收发
	 */
	public final static String SERIAL_CH2 = "/dev/ttyWCH2";

	/**
	 * 外设2
	 * 又分3
	 */
	public final static String SERIAL_CH3 = "/dev/ttyWCH3";

	/**
	 * 打印机命令数据,测试
	 * @return
	 */
	public static String getSPIData() {
		return  "02 02 02 00" +
				"20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20" + //empty line
				"20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20" + //empty line
				"80 81 20 20 20 20 20 54 41 58 49 20 4E 4F 20 20 20 20 20 20 20 20 %1$s FF FF 82 80 20 20 20 20 20 53 54 41 52 54 20 20 %2$s" +
				"FF FF 83 80 20 20 20 20 20 45 4E 44 20 20 20 20 %3$sFF FF 84 85 86 20 20 20 54 4F 54 41 4C 20 4B 4D 20 20 20 20 20 20 20 20 20 %4$s" +
				"FF FF FF 87 88 85 86 20 50 41 49 44 20 20 4B 4D 20 20 20 20 20 20 20 20 20 %5$sFF FF FF FF 87 88 89 8A 20 50 41 49 44 20 20 4D 49 4E 20 20 20 20 20 20 20 20 %6$s" +
				"FF FF FF FF 8B 8C 88 20 20 20 53 55 52 43 48 41 52 47 45 20 48 4B 24 20 20 20 %7$sFF FF FF 84 87 88 20 20 20 54 4F 54 41 4C 20 46 41 52 45 20 48 4B 24 20 20 %8$s" +
				"FF FF FF 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 8D 8D " +
				"8E 8F 20 54 48 41 4E 4B 20 59 4F 55 20 20 20 20 20 20 FF FF FF FF" +
				"20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20" + //empty line
				"20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20" + //empty line
				"20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20" + //empty line
				"20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20" + //empty line
				"03";
	}

	public static String getGrayscaleData(int grayscaleLevel) {
		String data = String.format("02 09 00 01 %02X 03", grayscaleLevel);
		return data;
	}


	public static String getSPIDateTime(Date dateTime) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		String dateTimeString = dateFormat.format(dateTime);
		String[] dateTimeSplit = dateTimeString.split(" ");
		String date = dateTimeSplit[0];
		String time = dateTimeSplit[1];
		StringBuilder sb = new StringBuilder();
		sb.append(getSPIDate(date));
		sb.append("20 ");
		sb.append(getSPITime(time));
		return sb.toString();
	}

	//write a convertor convert 12:34 to 30 39 3A 31 32
	private static String getSPITime(String time) {
		String[] times = time.split(":");
		String hour = times[0];
		String min = times[1];
		String result = "";
		for (int i = 0; i < hour.length(); i++) {
			result += Integer.toHexString(hour.charAt(i)) + " ";
		}
		result += "3A ";
		for (int i = 0; i < min.length(); i++) {
			result += Integer.toHexString(min.charAt(i)) + " ";
		}
		return result;
	}


	//write a convertor convert 09/12/2022 to 30 39 2F 31 32 2F 32 30 32 32 20
	public static String getSPIDate(String date) {
		String[] dates = date.split("/");
		String year = dates[2];
		String month = dates[1];
		String day = dates[0];
		String result = "";
		for (int i = 0; i < day.length(); i++) {
			result += Integer.toHexString(day.charAt(i)) + " ";
		}
		result += "2F ";
		for (int i = 0; i < month.length(); i++) {
			result += Integer.toHexString(month.charAt(i)) + " ";
		}
		result += "2F ";
		for (int i = 0; i < year.length(); i++) {
			result += Integer.toHexString(year.charAt(i)) + " ";
		}

		return result;
	}

	//write a convertor convert 1.6 to 31 2E 36
	public static String getSPIDecial(String decial) {
		String[] miles = decial.split("\\.");
		String result = "";
		for (int i = 0; i < miles[0].length(); i++) {
			result += Integer.toHexString(miles[0].charAt(i)) + " ";
		}
		result += "2E ";
		for (int i = 0; i < miles[1].length(); i++) {
			result += Integer.toHexString(miles[1].charAt(i)) + " ";
		}
		return result;
	}

	//write a convertor convert 1.6 to 31 2E 36
	public static String getSPIDecimal(String decimal, int expectedLength) {
		String[] miles = decimal.split("\\.");
		String result = "";
		// append 20 x (expectedLength - decimal length) times if needed
		if (decimal.length() < expectedLength) {
			for (int i = 0; i < expectedLength - decimal.length(); i++) {
				result += "20 ";
			}
		}
		for (int i = 0; i < miles[0].length(); i++) {
			result += Integer.toHexString(miles[0].charAt(i)) + " ";
		}
		result += "2E ";
		for (int i = 0; i < miles[1].length(); i++) {
			result += Integer.toHexString(miles[1].charAt(i)) + " ";
		}
		return result;
	}
}
