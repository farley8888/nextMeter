package com.serial.port;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

/**
 * Byte转换工具
 * 
 * @author yangle
 */
public class ByteUtils {

	/**
	 * 十六进制字符串转byte[]
	 * 
	 * @param hex
	 *            十六进制字符串
	 * @return byte[]
	 */
	public static byte[] hexStr2Byte(String hex) {
		if (hex == null) {
			return new byte[] {};
		}

		// 奇数位补0
		if (hex.length() % 2 != 0) {
			hex = "0" + hex;
		}

		int length = hex.length();
		ByteBuffer buffer = ByteBuffer.allocate(length / 2);
		for (int i = 0; i < length; i++) {
			String hexStr = hex.charAt(i) + "";
			i++;
			hexStr += hex.charAt(i);
			byte b = (byte) Integer.parseInt(hexStr, 16);
			buffer.put(b);
		}
		return buffer.array();
	}

	/**
	 * byte[]转十六进制字符串
	 * 
	 * @param array
	 *            byte[]
	 * @return 十六进制字符串
	 */
	public static String byteArrayToHexString(byte[] array) {
		if (array == null) {
			return "";
		}
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < array.length; i++) {
			buffer.append(byteToHex(array[i]));
		}
		return buffer.toString();
	}

	/**
	 * byte转十六进制字符
	 * 
	 * @param b
	 *            byte
	 * @return 十六进制字符
	 */
	public static String byteToHex(byte b) {
		String hex = Integer.toHexString(b & 0xFF);
		if (hex.length() == 1) {
			hex = '0' + hex;
		}
		return hex.toUpperCase(Locale.getDefault());
	}

	/**
	 * 将小端bytes数据转化为大端数据
	 * <p>
	 * 默认网络传输字节为大端，java 全部为大端（与平台无关）
	 * 关于 “Little-Endian and Big-Endian”,详情请参考：
	 *
	 * @param bytes
	 * @return
	 * </p>
	 */
	public static ByteBuffer bytesToBigEndian(byte[] bytes) {
		if (bytes == null || bytes.length < 1)
			return null;

		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		buffer.order(ByteOrder.BIG_ENDIAN);
		return buffer;
	}

	public static void main(String[] args) {
		//System.out.println(byteArrayToHexString("AT+SP?"));
	}
}
