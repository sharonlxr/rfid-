package com.zkc.pc700.helper;

import com.zkc.io.EmGpio;

public class ScanGpio {
	EmGpio gpio = new EmGpio();
	// 连接串口
	SerialPort serialPort = null;
	// 打开电源
	public void openPower() {
		try {
			if (true == gpio.gpioInit()) {
				// 电源调低
				gpio.setGpioOutput(111);
				gpio.setGpioDataLow(111);
				Thread.sleep(100);
				// 电源调高
				gpio.setGpioOutput(111);
				gpio.setGpioDataHigh(111);
				Thread.sleep(100);
			}
		} catch (Exception e) {
		}
	}

	// 打开扫描
	public void openScan() {
		// 开启扫描
		try {
			if (true == gpio.gpioInit()) {
				gpio.setGpioOutput(110);
				gpio.setGpioDataHigh(110);
				Thread.sleep(100);
				gpio.setGpioDataLow(110);
			}
			gpio.gpioUnInit();
		} catch (Exception e) {
		}
	}
}
