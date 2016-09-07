package com.fsl.cimei.rfid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.exception.RfidWifiException;

public class CommonUtility {

	@SuppressLint("SimpleDateFormat")
	public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");

	public static boolean isEmpty(String s) {
		if (null == s) {
			return true;
		} else {
			return s.isEmpty();
		}
	}

	public static String copyConfigFile(Context myContext, String assetName) {
		String errorMsg = null;
		InputStream is = null;
		FileOutputStream fos = null;
		try {
			is = myContext.getResources().getAssets().open(assetName);
			fos = myContext.openFileOutput(assetName, Context.MODE_PRIVATE);
			byte[] buffer = new byte[1024];
			int count = 0;
			while ((count = is.read(buffer)) > 0) {
				fos.write(buffer, 0, count);
			}
		} catch (Exception e) {
			errorMsg = "CommonUtility copy: " + e.toString();
		} finally {
			if (null != fos) {
				try {
					fos.close();
				} catch (IOException e) {
					errorMsg = "CommonUtility copy: " + e.toString();
				}
			}
			if (null != is) {
				try {
					is.close();
				} catch (IOException e) {
					errorMsg = "CommonUtility copy: " + e.toString();
				}
			}
		}
		return errorMsg;
	}

	public static Map<String, String> readConfig(Context myContext, String ASSETS_NAME) {
		Map<String, String> result = new HashMap<String, String>();
		FileInputStream fis = null;
		BufferedReader br = null;
		try {
			fis = myContext.openFileInput(ASSETS_NAME);
			br = new BufferedReader(new InputStreamReader(fis));
			String line;
			while ((line = br.readLine()) != null) {
				if (!line.trim().startsWith("#")) {
					String[] temp = line.split("=");
					if (temp.length == 2) {
						result.put(temp[0], temp[1]);
					} else if (temp.length == 1) {
						result.put(temp[0], "");
					}
				}
			}
		} catch (Exception e) {
			Log.v("CommonUtility readConfig", e.toString());
		} finally {
			if (null != br) {
				try {
					br.close();
				} catch (IOException e) {
					Log.v("CommonUtility readConfig", e.toString());
				}
			}
			if (null != fis) {
				try {
					fis.close();
				} catch (IOException e) {
					Log.v("CommonUtility readConfig", e.toString());
				}
			}
		}
		return result;
	}

	public static boolean isEmpty(java.util.Collection<?> collection) {
		if (collection == null) {
			return true;
		}
		return collection.isEmpty();
	}

	public static boolean isValidNumber(String str) {
		// String regex = "[0-9]*";
		String regex = "^-?\\d+$";
		return str.matches(regex);
	}

	public static boolean isValidHexNumber(String str) {
		String regex = "[0-9ABCDEFabcdef]*";
		return str.matches(regex);
	}
	
	public static String formStrFromSet(Set<String> strSet) {
		StringBuilder builder = new StringBuilder();
		for (String temp : strSet) {
			builder.append(", ").append(temp);
		}
		if (strSet.size() > 0) {
			return builder.toString().substring(2);
		} else {
			return "";
		}
	}

	public static Map<String, String> parseCommand(String command) {
		// "CMD/A=CMD_ACK MID/A=LDDBD_04 TID/U4=105410 ECD/A=0 ETX/A=\"\" ATTRIBUTES/A[5]=[LOT_NUMBER=\"KLMHA21GVF00\" DEVC_NUMBER=\"99SC900652FAK\" START_TIME=\"2013-11-20 18:29:25\" START_QTY=\"2880\" CARRIER_LIST=\"\"]";
		String k = "";
		String v = "";
		Map<String, String> resultMap = new HashMap<String, String>();
		if (command == null || command.trim().equals("")) {
			return resultMap;
		}
		command = command.trim();
		boolean isK = true; // is key
		boolean isQ = false; // has "
		boolean isSquareBrackets = false; // has [ or ]
		for (int i = 0; i < command.length(); i++) {
			if (command.charAt(i) == '[') {
				isSquareBrackets = true;
				if (isK) {
					k = k + "[";
				}
			} else if (command.charAt(i) == ']') {
				isSquareBrackets = false;
				if (isK) {
					k = k + "]";
				}
			} else if (command.charAt(i) == '=') {
				if (isSquareBrackets) {
					v = v + command.charAt(i);
				} else if (isQ) {
					v = v + command.charAt(i);
				} else {
					isK = false;
				}
			} else if (command.charAt(i) == '\"') {
				if (isSquareBrackets) {
					v = v + "\"";
				} else if (isQ) {
					isQ = false;
				} else {
					isQ = true;
				}
			} else if (command.charAt(i) == ' ') {
				if (isSquareBrackets) {
					v = v + command.charAt(i);
				} else if (isQ) {
					v = v + command.charAt(i);
				} else {
					resultMap.put(k, v);
					k = "";
					v = "";
					isK = true;
				}
			} else {
				if (isK) {
					k = k + command.charAt(i);
				} else {
					v = v + command.charAt(i);
				}
			}
		}
		resultMap.put(k, v);
		return resultMap;
	}

	@SuppressLint("DefaultLocale")
	public static String intToIp(int i) {
		return String.format("%d.%d.%d.%d", (i & 0xff), (i >> 8 & 0xff), (i >> 16 & 0xff), (i >> 24 & 0xff));
	}

	public static void pingHost(String hostname) throws RfidWifiException {
		StringBuilder sb = new StringBuilder();
		try {
			// Process process = new ProcessBuilder(new String[]{"ping","liriver.ap.freescale.net"}).redirectErrorStream(true).start();
			// InputStream stdin = process.getInputStream();
			// BufferedReader reader = new BufferedReader(new InputStreamReader(stdin));
			// String a;
			// while ((a = reader.readLine()) != null) {
			// log(a);
			// }
			Process p = Runtime.getRuntime().exec("ping -c 3 " + hostname);
			BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = null;
			line = in.readLine();
			while (line != null) {
				sb.append(line).append("|");
				line = in.readLine();
			}
		} catch (IOException e) {
			Log.e(Constants.APP_NAME, e.toString());
			logError(e.toString(), Constants.LOG_FILE_WIFI);
			throw new RfidWifiException("连接不到服务器");
		}
		if (CommonUtility.isEmpty(sb.toString())) {
			Log.e(Constants.APP_NAME, "No ping result");
			logError("No ping result", Constants.LOG_FILE_WIFI);
			throw new RfidWifiException("连接不到服务器");
		} else {
			// Log.v("RFID", sb.toString());
			// logError(sb.toString(), Constants.LOG_FILE_WIFI);
			// PING 10.192.130.159 (10.192.130.159) 56(84) bytes of data.--- 10.192.130.159 ping statistics ---3 packets transmitted, 0 received, 100% packet loss, time 2001ms
			// if (sb.toString().contains("3 packets transmitted, 0 received, 100% packet loss")) {
			// throw new RfidWifiException("连接不到服务器 " + sb.toString());
			// }
			String reg = "([0-9]{1}) packets transmitted, ([0-9]{1}) received,";
			Pattern pattern = Pattern.compile(reg);
			Matcher matcher = pattern.matcher(sb.toString());
			if (matcher.find()) {
				String b = matcher.group(1);
				String c = matcher.group(2);
				Log.e(Constants.APP_NAME, b + " packets transmitted, " + c + " received");
				logError(b + " packets transmitted, " + c + " received", Constants.LOG_FILE_WIFI);
				if (c.equals("0")) {
					throw new RfidWifiException("连接不到服务器 " + sb.toString());
				}
			}
		}
	}

	@SuppressLint("SimpleDateFormat")
	public static synchronized void logError(String msg, String filename) {
		Log.e(Constants.APP_NAME + " " + filename, msg);
		File file = Environment.getExternalStorageDirectory();
		File logFolder = new File(file.getAbsolutePath() + "/rfid-log/");
		boolean canWrite = logFolder.exists();
		if (!canWrite) {
			canWrite = logFolder.mkdirs();
		}
		if (canWrite) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss");
			Date date = new Date();
			File logFile = new File(file.getAbsolutePath() + "/rfid-log/" + filename + "_" + sdf.format(date) + ".txt");
			canWrite = logFile.exists();
			if (!canWrite) {
				try {
					canWrite = logFile.createNewFile();
				} catch (IOException e) {
					Log.e(Constants.APP_NAME, e.toString());
					canWrite = false;
				}
			}
			if (canWrite) {
				FileOutputStream fos = null;
				BufferedWriter bw = null;
				try {
					fos = new FileOutputStream(logFile, true);
					bw = new BufferedWriter(new OutputStreamWriter(fos));
					bw.append(sdf2.format(new Date()) + "\r\n");
					bw.append(msg + "\r\n");
					bw.flush();
					bw.close();
				} catch (Exception e) {
					Log.e(Constants.APP_NAME, e.toString());
				} finally {
					if (null != bw) {
						try {
							bw.close();
						} catch (IOException e) {
							Log.e(Constants.APP_NAME, e.toString());
						}
					}
					if (null != fos) {
						try {
							fos.close();
						} catch (IOException e) {
							Log.e(Constants.APP_NAME, e.toString());
						}
					}
				}
			}
		}
	}

	public static boolean updateSettingFile() {
		Properties p = new Properties();
		if (Constants.configFileName.equals(Constants.CONFIG_PROD)) {
			p.put("env", "prod");
		} else {
			p.put("env", "test");
		}
		if (Constants.carrierAssignInputOutput) {
			p.put("carrier_assign_in_out", "Y");
		} else {
			p.put("carrier_assign_in_out", "N");
		}
		if (Constants.manualDeassign) {
			p.put("manual_deassign", "Y");
		} else {
			p.put("manual_deassign", "N");
		}
		if (Constants.msgFilter) {
			p.put("message_filter", "Y");
		} else {
			p.put("message_filter", "N");
		}
		if (Constants.carrierAssignLoc) {
			p.put("carrier_assign_loc", "Y");
		} else {
			p.put("carrier_assign_loc", "N");
		}
		if (Constants.alarmUnsetMenu) {
			p.put("alarm_unset_menu", "Y");
		} else {
			p.put("alarm_unset_menu", "N");
		}
		if (Constants.autoLogout) {
			p.put("auto_logout", "Y");
		} else {
			p.put("auto_logout", "N");
		}
		p.put("baud", "" + BaseActivity.choosed_buad);
		return storePropertiesFile(p, "setting.conf");
	}

	public static boolean storePropertiesFile(Properties p, String filename) {
		File file = Environment.getExternalStorageDirectory();
		File settingFolder = new File(file.getAbsolutePath() + "/rfid-config/");
		File settingFile = new File(file.getAbsolutePath() + "/rfid-config/" + filename);
		boolean success = true;
		if (!settingFolder.exists()) {
			success = settingFolder.mkdir();
		}
		if (success) {
			if (settingFile.exists()) {
				success = settingFile.delete();
			}
		} else {
			// fail to create setting file's folder
			logError("fail to create setting file's folder", Constants.LOG_FILE_ERR);
			return false;
		}
		if (success) {
			try {
				success = settingFile.createNewFile();
			} catch (IOException e) {
				logError("Fail to create new setting file " + e.toString(), Constants.LOG_FILE_ERR);
				return false;
			}
		} else {
			// fail to delete old setting file
			logError("fail to delete old setting file", Constants.LOG_FILE_ERR);
			return false;
		}
		if (success) {
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(settingFile);
				p.store(fos, null);
				return true;
			} catch (IOException e) {
				return false;
			} finally {
				if (null != fos) {
					try {
						fos.close();
					} catch (IOException e) {
					}
				}
			}
		} else {
			// fail to create new setting file
			logError("fail to create new setting file", Constants.LOG_FILE_ERR);
			return false;
		}
	}

	private final static String actionUrl = "/RFID/servlet/UploadLogFile";
	public static void uploadFile(String serverIP, String serverPort, String uploadFile, String newName) {
		String end = "\r\n";
		String Hyphens = "--";
		String boundary = "*****";
		try {
			URL url = new URL("http://" + serverIP + ":" + serverPort + actionUrl);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			/* 允许Input、Output，不使用Cache */
			con.setDoInput(true);
			con.setDoOutput(true);
			con.setUseCaches(false);
			/* 设定传送的method=POST */
			con.setRequestMethod("POST");
			/* setRequestProperty */
			con.setRequestProperty("Connection", "Keep-Alive");
			con.setRequestProperty("Charset", "UTF-8");
			con.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
			/* 设定DataOutputStream */
			DataOutputStream ds = new DataOutputStream(con.getOutputStream());
			ds.writeBytes(Hyphens + boundary + end);
			ds.writeBytes("Content-Disposition: form-data; " + "name=\"file1\";filename=\"" + newName + "\"" + end);
			ds.writeBytes(end);
			/* 取得文件的FileInputStream */
			FileInputStream fStream = new FileInputStream(uploadFile);
			/* 设定每次写入1024bytes */
			int bufferSize = 1024;
			byte[] buffer = new byte[bufferSize];
			int length = -1;
			/* 从文件读取数据到缓冲区 */
			while ((length = fStream.read(buffer)) != -1) {
				/* 将数据写入DataOutputStream中 */
				ds.write(buffer, 0, length);
			}
			ds.writeBytes(end);
			ds.writeBytes(Hyphens + boundary + Hyphens + end);
			fStream.close();
			ds.flush();
			/* 取得Response内容 */
			InputStream is = con.getInputStream();
			int ch;
			StringBuffer b = new StringBuffer();
			while ((ch = is.read()) != -1) {
				b.append((char) ch);
			}
			ds.close();
		} catch (Exception e) {
			Log.e("uploadFile", e.getMessage());
			logError("uploadFile " + e.getMessage(), Constants.LOG_FILE_ERR);
		}
	}

	public static String[] translateMsg(String msg) {
		/*
		 * 1. "Current $portType Carrier $carrierName $lotMsg" set lotMsg "ScheduleId $carrierGroupId" set lotMsg "Lot $carrierLotNumber" 2. Started Lot TJMEA13PX200 successfully! 3. Ended Lot
		 * $lotNumber successfully! Ended Lot $lotNumber at machine $machId successfully!
		 */
		String result = msg;
		Matcher msgMatcher11 = msgPtn11.matcher(msg);
		if (msgMatcher11.find()) {
			String portType = msgMatcher11.group(1);
			String carrierName = msgMatcher11.group(2);
			String carrierGroupId = msgMatcher11.group(3);
			if (portType.equalsIgnoreCase("input")) {
				portType = "上料";
			} else if (portType.equalsIgnoreCase("output")) {
				portType = "下料";
			}
			return new String[] { "当前" + portType + "提篮" + carrierName + "， Schedule ID为" + carrierGroupId, Constants.TYPE_MSG };
		}
		Matcher msgMatcher12 = msgPtn12.matcher(msg);
		if (msgMatcher12.find()) {
			String portType = msgMatcher12.group(1);
			String carrierName = msgMatcher12.group(2);
			String carrierLotNumber = msgMatcher12.group(3);
			if (portType.equalsIgnoreCase("input")) {
				portType = "上料";
			} else if (portType.equalsIgnoreCase("output")) {
				portType = "下料";
			}
			return new String[] { "当前" + portType + "弹夹" + carrierName + "， lot为" + carrierLotNumber, Constants.TYPE_MSG };
		}
		Matcher msgMatcher13 = msgPtn13.matcher(msg);
		if (msgMatcher13.find()) {
			String portType = msgMatcher13.group(1);
			String carrierName = msgMatcher13.group(2);
			if (portType.equalsIgnoreCase("input")) {
				portType = "上料";
			} else if (portType.equalsIgnoreCase("output")) {
				portType = "下料";
			}
			return new String[] { "当前" + portType + "弹夹" + carrierName, Constants.TYPE_MSG };
		}
		Matcher msgMatcher2 = msgPtn2.matcher(msg);
		if (msgMatcher2.find()) {
			String lotNumber = msgMatcher2.group(1);
			return new String[] { "成功开批 " + lotNumber + "", Constants.TYPE_STEP };
		}
		Matcher msgMatcher32 = msgPtn32.matcher(msg);
		if (msgMatcher32.find()) {
			String lotNumber = msgMatcher32.group(1);
			String machId = msgMatcher32.group(2);
			return new String[] { "在机台 " + machId + " 成功结批 " + lotNumber, Constants.TYPE_STEP };
		}
		Matcher msgMatcher33 = msgPtn33.matcher(msg);
		if (msgMatcher33.find()) {
			String lotNumber = msgMatcher33.group(1);
			String input = msgMatcher33.group(2);
			String output = msgMatcher33.group(3);
			return new String[] { "成功结批 " + lotNumber + " ，上料弹夹：" + input + " ， 下料弹夹：" + output, Constants.TYPE_STEP };
		}
		Matcher msgMatcher31 = msgPtn31.matcher(msg);
		if (msgMatcher31.find()) {
			String lotNumber = msgMatcher31.group(1);
			return new String[] { "成功结批 " + lotNumber, Constants.TYPE_STEP };
		}
		Matcher msgMatcher4 = msgPtn4.matcher(msg);
		if (msgMatcher4.find()) {
			String lotNumber = msgMatcher4.group(1);
			String mach = msgMatcher4.group(2);
			String carrierInList = msgMatcher4.group(3);
			return new String[] { "lot[" + lotNumber + "]，机台号为 " + mach + "，上料弹夹：" + carrierInList, Constants.TYPE_MSG };
		}
		Matcher msgMatcher5 = msgPtn5.matcher(msg);
		if (msgMatcher5.find()) {
			String lotNumber = msgMatcher5.group(1);
			String mach = msgMatcher5.group(2);
			String carrierInList = msgMatcher5.group(3);
			return new String[] { "lot[" + lotNumber + "]，机台号为 " + mach + "，下料弹夹：" + carrierInList, Constants.TYPE_MSG };
		}
		Matcher msgMatcher6 = msgPtn6.matcher(msg);
		if (msgMatcher6.find()) {
			String lotNumber = msgMatcher6.group(1);
			return new String[] { "lot[" + lotNumber + "]可以结批了", Constants.TYPE_MSG };
		}
		Matcher msgMatcher7 = msgPtn7.matcher(msg);
		if (msgMatcher7.find()) {
			String lotNumber = msgMatcher7.group(1);
			String mach = msgMatcher7.group(2);
			return new String[] { "lot[" + lotNumber + "]在机台 " + mach + " 结批成功", Constants.TYPE_STEP };
		}
		Matcher msgMatcher8 = msgPtn8.matcher(msg);
		if (msgMatcher8.find()) {
			String lotNumber = msgMatcher8.group(1);
			return new String[] { "请将lot[" + lotNumber + "]在当前站结批！", Constants.TYPE_STEP };
		}
		Matcher msgMatcher9 = msgPtn9.matcher(msg);
		if (msgMatcher9.find()) {
			String mach = msgMatcher9.group(1);
			return new String[] { "清报警后才能开机 " + mach + "", Constants.TYPE_MSG };
		}

		Matcher errMatcher1 = errPtn1.matcher(msg);
		if (errMatcher1.find()) {
			String detail = errMatcher1.group(2);
			return new String[] { "错误1：机台开批失败，详细错误信息为：" + detail, Constants.TYPE_END };
		}
		Matcher errMatcher2 = errPtn2.matcher(msg);
		if (errMatcher2.find()) {
			String lotNumber = errMatcher2.group(2);
			String machId = errMatcher2.group(3);
			return new String[] { "错误2：lot[" + lotNumber + "]不在当前" + machId + "所在的Step上", Constants.TYPE_END };
		}
		Matcher errMatcher3 = errPtn3.matcher(msg);
		if (errMatcher3.find()) {
			String lotNumber = errMatcher3.group(2);
			return new String[] { "错误3：lot[" + lotNumber + "]的状态为HOLD。", Constants.TYPE_END };
		}
		Matcher errMatcher4_1 = errPtn4_1.matcher(msg);
		if (errMatcher4_1.find()) {
			String carrierName = errMatcher4_1.group(2);
			String lotNumber1 = errMatcher4_1.group(3);
			String lotNumber2 = errMatcher4_1.group(4);
			String assigned = errMatcher4_1.group(6);
			String input = errMatcher4_1.group(7);
			String inputMissing = errMatcher4_1.group(8);
			String output = errMatcher4_1.group(9);
			String outputMissing = errMatcher4_1.group(10);
			return new String[] {
					"错误4：弹夹 " + carrierName + " 绑定的lot[" + lotNumber1 + "]与当前机台开批的lot[" + lotNumber2 + "]不同。lot[" + lotNumber2 + "]弹夹检查结果：已绑定" + assigned + "个，上料"
							+ input + "个，缺少：" + inputMissing + "；下料" + output + "个，缺少：" + outputMissing, Constants.TYPE_END };
		}
		Matcher errMatcher4_2 = errPtn4_2.matcher(msg);
		if (errMatcher4_2.find()) {
			String carrierName = errMatcher4_2.group(2);
			String lotNumber1 = errMatcher4_2.group(3);
			String lotNumber2 = errMatcher4_2.group(4);
			return new String[] { "错误4：弹夹 " + carrierName + " 绑定的lot[" + lotNumber1 + "]与当前机台开批的lot[" + lotNumber2 + "]不同。", Constants.TYPE_END };
		}
		// Matcher errMatcher5 = errPtn5.matcher(msg);
		// if (errMatcher5.find()) {
		// String carrierName = errMatcher5.group(1);
		// String lotNumber1 = errMatcher5.group(2);
		// String lotNumber2 = errMatcher5.group(3);
		// return "错误5：新弹夹 " + carrierName + " lot  " + lotNumber1 + " ，lot " + lotNumber2 + " 不全。";
		// }
		Matcher errMatcher6 = errPtn6.matcher(msg);
		if (errMatcher6.find()) {
			String carrierName = errMatcher6.group(2);
			String machId = errMatcher6.group(3);
			return new String[] { "错误6：弹夹 " + carrierName + " 注册机台 " + machId + " 失败", Constants.TYPE_END };
		}
		Matcher errMatcher7_1 = errPtn7_1.matcher(msg);
		if (errMatcher7_1.find()) {
			String lotNumber = errMatcher7_1.group(2);
			String carrierName = errMatcher7_1.group(3);
			String machId = errMatcher7_1.group(4);
			String errText = errMatcher7_1.group(5);
			return new String[] { "错误7：下料：在机台 " + machId + " 绑定弹夹 " + carrierName + " 到lot[" + lotNumber + "]失败，" + errText, Constants.TYPE_END };
		}
		Matcher errMatcher7 = errPtn7.matcher(msg);
		if (errMatcher7.find()) {
			String lotNumber = errMatcher7.group(2);
			String carrierName = errMatcher7.group(3);
			String machId = errMatcher7.group(4);
			return new String[] { "错误7：下料：在机台 " + machId + " 绑定弹夹 " + carrierName + " 到lot[" + lotNumber + "]失败", Constants.TYPE_END };
		}
		Matcher errMatcher8 = errPtn8.matcher(msg);
		if (errMatcher8.find()) {
			String carrierName = errMatcher8.group(2);
			return new String[] { "错误8：Carrier " + carrierName + " 未绑定lot", Constants.TYPE_ERROR };
		}
		// Matcher errMatcher9_1 = errPtn9_1.matcher(msg);
		// if (errMatcher9_1.find()) {
		// String lotNumber = errMatcher9_1.group(2);
		// String assigned = errMatcher9_1.group(3);
		// String input = errMatcher9_1.group(4);
		// String inputMissing = errMatcher9_1.group(5);
		// String output = errMatcher9_1.group(6);
		// String outputMissing = errMatcher9_1.group(7);
		// return new String[] { "错误9：lot[" + lotNumber + "]弹夹检查结果：已绑定" + assigned + "个，上料" + input + "个，缺少：" + inputMissing + ";下料" + output + "个，缺少：" + outputMissing,
		// Constants.TYPE_ERROR };
		// }
		// Matcher errMatcher9_2 = errPtn9_2.matcher(msg);
		// if (errMatcher9_2.find()) {
		// String lotNumber = errMatcher9_2.group(2);
		// String assigned = errMatcher9_2.group(3);
		// String output = errMatcher9_2.group(4);
		// String outputMissing = errMatcher9_2.group(5);
		// return new String[] { "错误9：lot[" + lotNumber + "]弹夹检查结果：已绑定" + assigned + "个，下料" + output + "个，缺少：" + outputMissing,
		// Constants.TYPE_ERROR };
		// }
		// Matcher errMatcher9_3 = errPtn9_3.matcher(msg);
		// if (errMatcher9_3.find()) {
		// String lotNumber = errMatcher9_3.group(2);
		// String assigned = errMatcher9_3.group(3);
		// String input = errMatcher9_3.group(4);
		// String inputMissing = errMatcher9_3.group(5);
		// return new String[] { "错误9：lot[" + lotNumber + "]弹夹检查结果：已绑定" + assigned + "个，上料" + input + "个，缺少：" + inputMissing,
		// Constants.TYPE_ERROR };
		// }
		// Matcher errMatcher10 = errPtn10.matcher(msg);
		// if (errMatcher10.find()) {
		// String lotNumber = errMatcher10.group(2);
		// return new String[] { "错误：lot[" + lotNumber + "]的recipe与机台上recipe不符", Constants.TYPE_ERROR };
		// }
		// Matcher errMatcher11 = errPtn11.matcher(msg);
		// if (errMatcher11.find()) {
		// String lotNumber = errMatcher11.group(1);
		// String mach = errMatcher11.group(2);
		// return new String[] { "错误：这批料 [" + lotNumber + "]不在当前站，机台号为 " + mach, Constants.TYPE_ERROR };
		// }
		Matcher errMatcher12 = errPtn12.matcher(msg);
		if (errMatcher12.find()) {
			String port = errMatcher12.group(1);
			String carrierName = errMatcher12.group(2);
			if (port.equalsIgnoreCase("input")) {
				port = "上料";
			} else if (port.equalsIgnoreCase("output")) {
				port = "下料";
			}
			return new String[] { "错误：当前" + port + "弹夹 " + carrierName + " 未绑定lot", Constants.TYPE_ERROR };
		}
		// Matcher errMatcher13 = errPtn13.matcher(msg);
		// if (errMatcher13.find()) {
		// String carrierName = errMatcher13.group(1);
		// String mach = errMatcher13.group(2);
		// return new String[] { "错误：在绑定弹夹 " + carrierName + " 时，在机台 " + mach + " 结批失败", Constants.TYPE_ERROR };
		// }
		Matcher errMatcher13_2 = errPtn13_2.matcher(msg);
		if (errMatcher13_2.find()) {
			// String type = errMatcher13_2.group(1);
			String lot = errMatcher13_2.group(2);
			String errText = errMatcher13_2.group(3);
			return new String[] { "结批失败[" + lot + "]：" + errText, Constants.TYPE_ERROR };
		}
		// Matcher errMatcher14 = errPtn14.matcher(msg);
		// if (errMatcher14.find()) {
		// String port = errMatcher14.group(1);
		// String mach = errMatcher14.group(2);
		// if (port.equalsIgnoreCase("input")) {
		// port = "上料";
		// } else if (port.equalsIgnoreCase("output")) {
		// port = "下料";
		// }
		// return new String[] { "机台 " + mach + " " + port + "RFID未读到！", Constants.TYPE_ERROR };
		// }
		// Matcher errMatcher15 = errPtn15.matcher(msg);
		// if (errMatcher15.find()) {
		// String port = errMatcher15.group(1);
		// String mach = errMatcher15.group(2);
		// if (port.equalsIgnoreCase("input")) {
		// port = "上料";
		// } else if (port.equalsIgnoreCase("output")) {
		// port = "下料";
		// }
		// return new String[] { "机台 " + mach + " " + port + "EI信息未读到！", Constants.TYPE_ERROR };
		// }
		Matcher errMatcher15_2 = errPtn15_2.matcher(msg);
		if (errMatcher15_2.find()) {
			String lot = errMatcher15_2.group(2);
			String cname = errMatcher15_2.group(3);
			String mach = errMatcher15_2.group(4);
			return new String[] { "在机台 " + mach + " 将弹夹" + cname + "从lot[" + lot + "]解绑失败", Constants.TYPE_ERROR };
		}
		// Matcher errMatcher16 = errPtn16.matcher(msg);
		// if (errMatcher16.find()) {
		// return new String[] { "主辅die验证失败", Constants.TYPE_ERROR };
		// }
		// Matcher errMatcher17 = errPtn17.matcher(msg);
		// if (errMatcher17.find()) {
		// String carrier = errMatcher17.group(1);
		// return new String[] { "弹夹 " + carrier + " 未绑定lot", Constants.TYPE_ERROR };
		// }
		Matcher errMatcher17_1 = errPtn17_1.matcher(msg);
		if (errMatcher17_1.find()) {
			String mach = errMatcher17_1.group(1);
			return new String[] { "没有EI，请检查机台：" + mach, Constants.TYPE_ERROR };
		}
		Matcher errMatcher17_2 = errPtn17_2.matcher(msg);
		if (errMatcher17_2.find()) {
			String type = errMatcher17_2.group(1);
			String mach = errMatcher17_2.group(2);
			if (type.equalsIgnoreCase("input")) {
				type = "上料";
			} else if (type.equalsIgnoreCase("output")) {
				type = "下料";
			}
			return new String[] { "没有RFID，请在机台" + mach + "检查当前" + type + "弹夹", Constants.TYPE_END };
		}
		Matcher errMatcher17_3 = errPtn17_3.matcher(msg);
		if (errMatcher17_3.find()) {
			String portType = errMatcher17_3.group(1);
			String mach = errMatcher17_3.group(2);
			if (portType.equalsIgnoreCase("input")) {
				portType = "上料";
			} else if (portType.equalsIgnoreCase("output")) {
				portType = "下料";
			}
			return new String[] { "没有Sensor，请在机台" + mach + "检查当前" + portType + "弹夹", Constants.TYPE_ERROR };
		}
		Matcher errMatcher17_4 = errPtn17_4.matcher(msg);
		if (errMatcher17_4.find()) {
			String temp = errMatcher17_4.group(1);
			String arr[] = temp.split(" ");
			if (arr.length == 3) {
				String mach = arr[0];
				String portType = arr[1];
				if (portType.equalsIgnoreCase("input")) {
					portType = "上料";
				} else if (portType.equalsIgnoreCase("output")) {
					portType = "下料";
				}
				String carrierID = arr[2];
				return new String[] { "机台" + mach + " " + portType + " " + carrierID + "没有EI信号", Constants.TYPE_ERROR };
			}
		}
		// Matcher errMatcher18 = errPtn18.matcher(msg);
		// if (errMatcher18.find()) {
		// String lot = errMatcher18.group(1);
		// return new String[] { "验证lot[" + lot + "]失败", Constants.TYPE_ERROR };
		// }
		Matcher errMatcher18_2 = errPtn18_2.matcher(msg);
		if (errMatcher18_2.find()) {
			String cname = errMatcher18_2.group(1);
			String lot1 = errMatcher18_2.group(2);
			String lot2 = errMatcher18_2.group(3);
			return new String[] { "弹夹" + cname + "，lot[" + lot1 + "]开批失败，因与已开批的lot[" + lot2 + "]的DEVICE不同", Constants.TYPE_END };
		}
		// Matcher errMatcher19 = errPtn19.matcher(msg);
		// if (errMatcher19.find()) {
		// String mach = errMatcher19.group(1);
		// return new String[] { "在机台 " + mach + " 开批失败", Constants.TYPE_ERROR };
		// }
		Matcher errMatcher19_2 = errPtn19_2.matcher(msg);
		if (errMatcher19_2.find()) {
			return new String[] { "请补扫上料弹夹", Constants.TYPE_END };
		}
		// Matcher errMatcher20 = errPtn20.matcher(msg);
		// if (errMatcher20.find()) {
		// return new String[] { "主辅die验证失败", Constants.TYPE_ERROR };
		// }
		Matcher errMatcher20_1 = errPtn20_1.matcher(msg);
		if (errMatcher20_1.find()) {
			String mach = errMatcher20_1.group(1);
			return new String[] { "机台" + mach + "缺少recipe ID", Constants.TYPE_END };
		}
		Matcher errMatcher20_2 = errPtn20_2.matcher(msg);
		if (errMatcher20_2.find()) {
			String lot = errMatcher20_2.group(1);
			String temp = errMatcher20_2.group(2);
			if (temp.contains(" ")) {
				String[] arr = temp.split(" ");
				if (arr.length == 2) {
					return new String[] { "机台" + arr[0] + "的recipe " + arr[1] + " 与lot[" + lot + "]所对应的recipe不符", Constants.TYPE_END };
				}
			}
		}
		Matcher errMatcher21_1 = errPtn21_1.matcher(msg);
		if (errMatcher21_1.find()) {
			return new String[] { "TX信号丢失", Constants.TYPE_END };
		}
		Matcher errMatcher21_2 = errPtn21_2.matcher(msg);
		if (errMatcher21_2.find()) {
			String readerID = errMatcher21_2.group(1);
			return new String[] { "Reader " + readerID + " 信号丢失", Constants.TYPE_END };
		}
		Matcher errMatcher30 = errPtn30.matcher(msg);
		if (errMatcher30.find()) {
			String gid = errMatcher30.group(2);
			return new String[] { "在当前站没有" + gid + "的lot", Constants.TYPE_ERROR };
		}
		Matcher errMatcher31 = errPtn31.matcher(msg);
		if (errMatcher31.find()) {
			String errText = errMatcher31.group(1);
			return new String[] { "机台结批失败：" + errText, Constants.TYPE_ERROR };
		}
		Matcher errMatcher32 = errPtn32.matcher(msg);
		if (errMatcher32.find()) {
			String cid = errMatcher32.group(2);
			return new String[] { "提篮" + cid + "目前没有合适的辅料", Constants.TYPE_ERROR };
		}
		Matcher errMatcher33 = errPtn33.matcher(msg);
		if (errMatcher33.find()) {
			String errText = errMatcher33.group(1);
			return new String[] { "wafer lot在机台结批失败：" + errText, Constants.TYPE_ERROR };
		}
		Matcher errMatcher35 = errPtn35.matcher(msg);
		if (errMatcher35.find()) {
			String errText = errMatcher35.group(1);
			return new String[] { "wafer lot在机台开批失败：" + errText, Constants.TYPE_ERROR };
		}
		Matcher errMatcher36 = errPtn36.matcher(msg);
		if (errMatcher36.find()) {
			String cid = errMatcher36.group(1);
			return new String[] { "弹夹" + cid + "未绑定lot", Constants.TYPE_ERROR };
		}
		Matcher errMatcher37 = errPtn37.matcher(msg);
		if (errMatcher37.find()) {
			String lot = errMatcher37.group(1);
			return new String[] { "lot[" + lot + "]验证失败", Constants.TYPE_ERROR };
		}
		Matcher errMatcher40 = errPtn40.matcher(msg);
		if (errMatcher40.find()) {
			String lot = errMatcher40.group(1);
			String mach = errMatcher40.group(2);
			String carrierID = errMatcher40.group(3);
			return new String[] { "机台" + mach + "上已开批的lot[" + lot + "]不属于提篮[" + carrierID + "]", Constants.TYPE_ERROR };
		}
		Matcher errMatcher41 = errPtn41.matcher(msg);
		if (errMatcher41.find()) {
			// String lot = errMatcher41.group(1);
			String mach = errMatcher41.group(2);
			return new String[] { "机台" + mach + "上没有可用的lot", Constants.TYPE_END };
		}
		Matcher errMatcher42 = errPtn42.matcher(msg);
		if (errMatcher42.find()) {
			String errTxt = errMatcher42.group(1);
			return new String[] { "DualDie 验证失败：" + errTxt, Constants.TYPE_END };
		}
		Matcher errMatcher43 = errPtn43.matcher(msg);
		if (errMatcher43.find()) {
			String errTxt = errMatcher43.group(1);
			return new String[] { "AssignWlotCarrier失败：" + errTxt, Constants.TYPE_END };
		}
		Matcher errMatcher44 = errPtn44.matcher(msg);
		if (errMatcher44.find()) {
			return new String[] { "没有主Die", Constants.TYPE_END };
		}
		Matcher errMatcher45 = errPtn45.matcher(msg);
		if (errMatcher45.find()) {
			return new String[] { "在绑定新提篮或弹匣在机器之前不能正确地清除原有提篮或弹匣信息", Constants.TYPE_END };
		}
		Matcher errMatcher46 = errPtn46.matcher(msg);
		if (errMatcher46.find()) {
			return new String[] { "package code 未配置", Constants.TYPE_END };
		}
		Matcher errMatcher50 = errPtn50.matcher(msg);
		if (errMatcher50.find()) {
			String mach = errMatcher50.group(2);
			String cname = errMatcher50.group(3);
			return new String[] { "机台" + mach + "没有开批的lot，无法绑定carrier[" + cname + "]", Constants.TYPE_END };
		}
		Matcher errMatcher51 = errPtn51.matcher(msg);
		if (errMatcher51.find()) {
			String lot = errMatcher51.group(2);
			return new String[] { "所有弹夹已被绑定到lot[" + lot + "]，请先结批！", Constants.TYPE_ERROR };
		}
		Matcher errMatcher53 = errPtn53.matcher(msg);
		if (errMatcher53.find()) {
			String mach = errMatcher53.group(1);
			return new String[] { mach + " RunOneMagazine超时", Constants.TYPE_ERROR };
		}
		Matcher errMatcher54 = errPtn54.matcher(msg);
		if (errMatcher54.find()) {
			String mach = errMatcher54.group(1);
			return new String[] { mach + " RunOneMagazine失败", Constants.TYPE_ERROR };
		}
		Matcher errMatcher55 = errPtn55.matcher(msg);
		if (errMatcher55.find()) {
			String mach = errMatcher55.group(1);
			return new String[] { mach + " Reset count超时", Constants.TYPE_ERROR };
		}
		Matcher errMatcher56 = errPtn56.matcher(msg);
		if (errMatcher56.find()) {
			String mach = errMatcher56.group(1);
			return new String[] { mach + " Reset count失败", Constants.TYPE_ERROR };
		}
		Matcher errMatcher57 = errPtn57.matcher(msg);
		if (errMatcher57.find()) {
			String mach = errMatcher57.group(1);
			return new String[] { mach + " query good qty on Machine 超时", Constants.TYPE_ERROR };
		}
		Matcher errMatcher60 = errPtn60.matcher(msg);
		if (errMatcher60.find()) {
			String mach = errMatcher60.group(1);
			return new String[] { mach + " 开了多批料，请结批", Constants.TYPE_ERROR };
		}
		return new String[] { result, Constants.TYPE_MSG };
	}

	private static final String msgFormat11 = "^Current (.+) Carrier (.+) ScheduleId (.+)";
	private static final Pattern msgPtn11 = Pattern.compile(msgFormat11);
	private static final String msgFormat12 = "^Current (.+) Carrier (.+) Lot (.+)";
	private static final Pattern msgPtn12 = Pattern.compile(msgFormat12);
	private static final String msgFormat13 = "^Current (.+) Carrier (.+)";
	private static final Pattern msgPtn13 = Pattern.compile(msgFormat13);
	private static final String msgFormat2 = "^Started Lot (.+) successfully!$";
	private static final Pattern msgPtn2 = Pattern.compile(msgFormat2);
	private static final String msgFormat31 = "^Ended Lot (.+) successfully!$";
	private static final Pattern msgPtn31 = Pattern.compile(msgFormat31);
	private static final String msgFormat32 = "^Ended Lot (.+) at machine (.+) successfully!$";
	private static final Pattern msgPtn32 = Pattern.compile(msgFormat32);
	private static final String msgFormat33 = "^Ended Lot (.+) successfully! INPUT: (.+). OUTPUT: (.+).";
	private static final Pattern msgPtn33 = Pattern.compile(msgFormat33);
	private static final String msgFormat4 = "^LOT (.+) on (.+) input carriers: (.+)";
	private static final Pattern msgPtn4 = Pattern.compile(msgFormat4);
	private static final String msgFormat5 = "^LOT (.+) on (.+) output carriers: (.+)";
	private static final Pattern msgPtn5 = Pattern.compile(msgFormat5);
	private static final String msgFormat6 = "^Lot (.+) is ready for endLotatStep!$";
	private static final Pattern msgPtn6 = Pattern.compile(msgFormat6);
	private static final String msgFormat7 = "^Lot (.+) end on machine (.+) successfully!$";
	private static final Pattern msgPtn7 = Pattern.compile(msgFormat7);
	// Ended Lot TJMEA1SL7B00 on all machines, please end at step. ----- 请将lot TJMEA1SL7B00在当前站结批！
	private static final String msgFormat8 = "^Ended Lot (.+) on all machines, please end at step.$";
	private static final Pattern msgPtn8 = Pattern.compile(msgFormat8);
	private static final String msgFormat9 = "Cannot start machine (.+), please clear alarm on tablet";
	private static final Pattern msgPtn9 = Pattern.compile(msgFormat9);

	private static final String errFormat1 = "^ERROR 1: \\((.+)\\) Failed to lot start on machine: (.+)";
	private static final Pattern errPtn1 = Pattern.compile(errFormat1);
	private static final String errFormat2 = "^ERROR 2: \\((.+)\\) Lot (.+) registered is not at Machine (.+) process step";
	private static final Pattern errPtn2 = Pattern.compile(errFormat2);
	private static final String errFormat3 = "^ERROR 3: \\((.+)\\) Lot (.+) registered is on HOLD status. Please release it to continue.$";
	private static final Pattern errPtn3 = Pattern.compile(errFormat3);
	private static final String errFormat4_1 = "^ERROR 4: (.+) Carrier (.+) Lot (.+) is different from Lot (.+) started. Lot (.+) Carrier Check: Assigned \\((.+)\\), Input \\((.+)\\), Missing: (.+), Output \\((.+)\\), Missing: (.+)";
	private static final Pattern errPtn4_1 = Pattern.compile(errFormat4_1);
	private static final String errFormat4_2 = "^ERROR 4: (.+) Carrier (.+) Lot (.+) is different from Lot (.+) started$";
	private static final Pattern errPtn4_2 = Pattern.compile(errFormat4_2);
	// private static String errFormat5 = "^ERROR 5: \\(IN\\) New carrier (.+) Lot (.+) comes, while not all carriers input for started Lot (.+)\\.$";
	// private static Pattern errPtn5 = Pattern.compile(errFormat5);
	private static String errFormat6 = "^ERROR 6: \\((.+)\\) Failed to register Carrier (.+) on Machine (.+)";
	private static Pattern errPtn6 = Pattern.compile(errFormat6);
	private static final String errFormat7 = "^ERROR 7: \\((.+)\\) Failed to assign Lot (.+) to Carrier (.+) on machine (.+)";
	private static final Pattern errPtn7 = Pattern.compile(errFormat7);
	private static final String errFormat7_1 = "^ERROR 7: \\((.+)\\) Failed to assign Lot (.+) to Carrier (.+) on machine (.+):(.+)";
	private static final Pattern errPtn7_1 = Pattern.compile(errFormat7_1);
	private static final String errFormat8 = "^ERROR 8: (.+) Free magazine (.+) is not assigned to any lot";
	private static final Pattern errPtn8 = Pattern.compile(errFormat8);
	// private static final String errFormat9_1 = "^ERROR 9: (.+) Lot (.+) Carrier Check: Assigned \\((.+)\\), Input \\((.+)\\), Missing: (.+), Output \\((.+)\\), Missing: (.+)";
	// private static final Pattern errPtn9_1 = Pattern.compile(errFormat9_1);
	// private static final String errFormat9_2 = "^ERROR 9: (.+) Lot (.+) Carrier Check: Assigned \\((.+)\\), Output \\((.+)\\), Missing: (.+)";
	// private static final Pattern errPtn9_2 = Pattern.compile(errFormat9_2);
	// private static final String errFormat9_3 = "^ERROR 9: (.+) Lot (.+) Carrier Check: Assigned \\((.+)\\), Input \\((.+)\\), Missing: (.+)";
	// private static final Pattern errPtn9_3 = Pattern.compile(errFormat9_3);
	// private static final String errFormat10 = "^error MatlMgr_Pic_(.+) \\{Recipe pattern for lot \\((.+)\\) does not match the recipe on the machine.\\}";
	// private static final Pattern errPtn10 = Pattern.compile(errFormat10);
	// private static final String errFormat11 = "^Lot (.+) registered is not at Machine (.+) process step$";
	// private static final Pattern errPtn11 = Pattern.compile(errFormat11);
	private static final String errFormat12 = "^Free Magazine: Current (.+) Carrier (.+) is not assigned to any lot$";
	private static final Pattern errPtn12 = Pattern.compile(errFormat12);
	// private static final String errFormat13 = "^ERROR: Failed to End Lot when Assign Carrier (.+) on machine (.+)";
	// private static final Pattern errPtn13 = Pattern.compile(errFormat13);
	private static final String errFormat13_2 = "^ERROR 13: \\((.+)\\) Failed to end Lot (.+): (.+)";
	private static final Pattern errPtn13_2 = Pattern.compile(errFormat13_2);
	// ERROR: RFID CANNOT Read Tag! Please check current OUTPUT carrier on BWB-146. – 机台BWB-146上料（下料）RFID未读到！
	// private static final String errFormat14 = "^ERROR: RFID CANNOT Read Tag! Please check current (.+) carrier on (.+).";
	// private static final Pattern errPtn14 = Pattern.compile(errFormat14);
	// ERROR: No EI/Sensor Single! Please check current INPUT carrier on BWB-146. --机台BWB-146上料（下料）EI信息未读到！
	// private static final String errFormat15 = "^ERROR: No EI/Sensor Single! Please check current (.+) carrier on (.+).";
	// private static final Pattern errPtn15 = Pattern.compile(errFormat15);
	private static final String errFormat15_2 = "^ERROR 15: \\((.+)\\) Failed to deassign Lot (.+) from Carrier (.+) on machine (.+)";
	private static final Pattern errPtn15_2 = Pattern.compile(errFormat15_2);
	// private static final String errFormat16 = "^DualDie verification fail.$";
	// private static final Pattern errPtn16 = Pattern.compile(errFormat16);
	// private static final String errFormat17 = "^CARRIER (.+) didn't assign to any lot$";
	// private static final Pattern errPtn17 = Pattern.compile(errFormat17);
	private static final String errFormat17_1 = "^ERROR 17: No EI! Please check (.+).";
	private static final Pattern errPtn17_1 = Pattern.compile(errFormat17_1);
	private static final String errFormat17_2 = "^ERROR 17: No RFID! Please check current (.+) carrier on (.+).";
	private static final Pattern errPtn17_2 = Pattern.compile(errFormat17_2);
	private static final String errFormat17_3 = "^ERROR 17: No Sensor! Please check current (.+) carrier on (.+).";
	private static final Pattern errPtn17_3 = Pattern.compile(errFormat17_3);
	private static final String errFormat17_4 = "^ERROR 17: No EI matched! (.+)";
	private static final Pattern errPtn17_4 = Pattern.compile(errFormat17_4);
	// private static final String errFormat18 = "^Validation Lots (.+) Failure$";
	// private static final Pattern errPtn18 = Pattern.compile(errFormat18);
	private static final String errFormat18_2 = "^ERROR 18: \\(IN\\) Carrier (.+) Lot (.+) cant start due to different DEVICE with Lot (.+) started";
	private static final Pattern errPtn18_2 = Pattern.compile(errFormat18_2);
	// private static final String errFormat19 = "^Failed to lot start on machine: (.+)";
	// private static final Pattern errPtn19 = Pattern.compile(errFormat19);
	private static final String errFormat19_2 = "ERROR 19: No input carrier load on (.+) before machine start up";
	private static final Pattern errPtn19_2 = Pattern.compile(errFormat19_2);
	// private static final String errFormat20 = "^Dualdie verify fail$";
	// private static final Pattern errPtn20 = Pattern.compile(errFormat20);
	private static final String errFormat20_1 = "^ERROR 20: error MatlMgr_Pic_WBKNSError \\{recipeId \\(\\) is require (.+) \\}";
	private static final Pattern errPtn20_1 = Pattern.compile(errFormat20_1);
	private static final String errFormat20_2 = "^ERROR 20: error MatlMgr_Pic_WBKNSError \\{Recipe pattern for lot \\((.+)\\) does not match the recipe on the machine (.+)\\}";
	private static final Pattern errPtn20_2 = Pattern.compile(errFormat20_2);
	private static final String errFormat21_1 = "^ERROR 21: TX Lost$";
	private static final Pattern errPtn21_1 = Pattern.compile(errFormat21_1);
	private static final String errFormat21_2 = "^ERROR 21: Reader (.+) Lost$";
	private static final Pattern errPtn21_2 = Pattern.compile(errFormat21_2);
	private static final String errFormat30 = "^ERROR 30: No lot at Machine (.+) process step for Schedule (.+)";
	private static final Pattern errPtn30 = Pattern.compile(errFormat30);
	private static final String errFormat31 = "^ERROR 31: Failed to lot end on machine: (.+)";
	private static final Pattern errPtn31 = Pattern.compile(errFormat31);
	private static final String errFormat32 = "^ERROR 32: No Wafer lot at machine (.+) process step for carrier (.+)";
	private static final Pattern errPtn32 = Pattern.compile(errFormat32);
	private static final String errFormat33 = "^ERROR 33: Failed to wafer lot end on machine: (.+)";
	private static final Pattern errPtn33 = Pattern.compile(errFormat33);
	private static final String errFormat35 = "^ERROR 35: Failed to wafer lot start on machine: (.+)";
	private static final Pattern errPtn35 = Pattern.compile(errFormat35);
	private static final String errFormat36 = "^ERROR 36: CARRIER (.+) didnt assign to any lot";
	private static final Pattern errPtn36 = Pattern.compile(errFormat36);
	private static final String errFormat37 = "^ERROR 37: Validation Lots (.+) Failure";
	private static final Pattern errPtn37 = Pattern.compile(errFormat37);
	private static final String errFormat40 = "^ERROR 40: Lot (.+) started on (.+) is not belonged to Carrier (.+)";
	private static final Pattern errPtn40 = Pattern.compile(errFormat40);
	private static final String errFormat41 = "^ERROR 41: No lot available for Schedule (.+) at Machine (.+)";
	private static final Pattern errPtn41 = Pattern.compile(errFormat41);
	private static final String errFormat42 = "^ERROR 42: DualDie verification fail:(.+)";
	private static final Pattern errPtn42 = Pattern.compile(errFormat42);
	private static final String errFormat43 = "^ERROR 43: Failed to AssignWlotCarrier: (.+)";
	private static final Pattern errPtn43 = Pattern.compile(errFormat43);
	private static final String errFormat44 = "^ERROR 44: No first die on machine.";
	private static final Pattern errPtn44 = Pattern.compile(errFormat44);
	private static final String errFormat45 = "^ERROR 45: \\((.+)\\) Failed to unregister Carrier from Machine (.+).";
	private static final Pattern errPtn45 = Pattern.compile(errFormat45);
	private static final String errFormat46 = "^ERROR 46: No magazineSize configuration on Package (.+). Please contact CIM support";
	private static final Pattern errPtn46 = Pattern.compile(errFormat46);
	private static final String errFormat50 = "^ERROR 50: \\((.+)\\) No lot started on machine (.+), cannot assign carrier (.+)";
	private static final Pattern errPtn50 = Pattern.compile(errFormat50);
	private static final String errFormat51 = "^ERROR 51: \\((.+)\\) All magazines had been assigned to the lot (.+). Please end the lot (.+) first";
	private static final Pattern errPtn51 = Pattern.compile(errFormat51);
	private static final String errFormat53 = "^ERROR 53: TIMEOUT: Can not RunOneMagazine on Machine (.+)";
	private static final Pattern errPtn53 = Pattern.compile(errFormat53);
	private static final String errFormat54 = "^ERROR 54: RunOneMagazine on Machine (.+) failed";
	private static final Pattern errPtn54 = Pattern.compile(errFormat54);
	private static final String errFormat55 = "^ERROR 55: TIMEOUT: Can not Reset count on Machine (.+)";
	private static final Pattern errPtn55 = Pattern.compile(errFormat55);
	private static final String errFormat56 = "^ERROR 56: Reset count on Machine (.+) failed";
	private static final Pattern errPtn56 = Pattern.compile(errFormat56);
	private static final String errFormat57 = "^ERROR 57: TIMEOUT: Can not query good qty on Machine (.+)";
	private static final Pattern errPtn57 = Pattern.compile(errFormat57);
	private static final String errFormat60 = "^ERROR 60: More than 1 lot started on machine (.+)";
	private static final Pattern errPtn60 = Pattern.compile(errFormat60);

}
