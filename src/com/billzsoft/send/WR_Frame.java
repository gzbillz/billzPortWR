package com.billzsoft.send;

/******************************************
 *　程序文件名称：WR_Frame
 *  功能：从串行口COM1中发送数据
 ******************************************/
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TooManyListenersException;

import javax.comm.CommPortIdentifier;
import javax.comm.PortInUseException;
import javax.comm.SerialPort;
import javax.comm.SerialPortEvent;
import javax.comm.SerialPortEventListener;
import javax.comm.UnsupportedCommOperationException;

import com.billzsoft.read.model.SmsServerOut;
import com.billzsoft.send.dao.WDbHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * @description 接收内网串口数据
 * @class WR_Frame
 * @author YHZ
 * @date 2013-6-24
 */
public class WR_Frame implements Runnable, SerialPortEventListener {
	
	/* 检测系统中可用的通讯端口类 */
	static CommPortIdentifier portId;
	/* Enumeration 为枚举型类,在util中 */
	@SuppressWarnings("rawtypes")
	static Enumeration portList;
	InputStream inputStream;
	/* RS-232的串行口 */
	SerialPort serialPort;
	Thread readThread;
 	List<Byte> listByte = null; 
	/* 设置判断要是否关闭串口的标志 */
	boolean mark;

	// 临时变量
	boolean isStart = false; // 开始获取数据
	boolean isEnd = false; // 结束标记
	List<String> ctx = new ArrayList<String>(); // 截取内容
	// end 临时变量
	
	/**
	 * 打开串口,并调用线程发送数据
	 */
	public void start() {
		/* 获取系统中所有的通讯端口 */
		portList = CommPortIdentifier.getPortIdentifiers();
		/* 用循环结构找出串口 */
		while (portList.hasMoreElements()) {
			/* 强制转换为通讯端口类型 */
			portId = (CommPortIdentifier) portList.nextElement();
			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				if (portId.getName().equals("COM2")) {
					try {
						serialPort = (SerialPort) portId.open("ReadComm", 2000);
					} catch (PortInUseException e) {
						e.printStackTrace();
					}

					/* 设置串口监听器 */
					try {
						serialPort.addEventListener(this);
					} catch (TooManyListenersException e) {
						e.printStackTrace();
					}
					/* 侦听到串口有数据,触发串口事件 */
					serialPort.notifyOnDataAvailable(true);
				}
			}
		}
		readThread = new Thread(this);
		readThread.start();// 线程负责每接收一次数据休眠20秒钟
	}
	
	/**
	 * 发送数据,休眠5秒钟后接收
	 */
	public void run() {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 串口监听器触发的事件，设置串口通讯参数，读取数据并写到文本区中 
	 */
	public void serialEvent(SerialPortEvent event) {
		/* 设置串口通讯参数：波特率、数据位、停止位、奇偶校验 */
		// BI - 通讯中断. CD - 载波检测.
		// CTS - 清除发送. DATA_AVAILABLE - 有数据到达. DSR - 数据设备准备好. FE - 帧错误.
		// OE - 溢位错误. OUTPUT_BUFFER_EMPTY - 输出缓冲区已清空. PE - 奇偶校验错. RI - 振铃指示.
		try {
			serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
		} catch (UnsupportedCommOperationException e) {
			e.printStackTrace();
		}
		try {
			inputStream = serialPort.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			int count = inputStream.available();
			System.out.println("count=" + count);
			byte[] readBuffer = new byte[count];
			int readCount = 0; // 已经成功读取的字节的个数
			/* 从线路上读取数据流 */
			while (readCount < count) {
				readCount += inputStream.read(readBuffer, readCount, count - readCount);
				for(byte bb : readBuffer){
					System.out.println("字节名：" + bb);
				}
 			}
			
			/* 接收到的数据存放到文本区中 */ 
			
			// 开始标记 { =byte=123
			if(isEnd == false && readBuffer.length > 0 && readBuffer[0] == 91){
				System.out.println("开始记录");
				isStart = true; // 开始记录
				listByte = new ArrayList<Byte>();
			}

			if(isStart){
				for(byte bt : readBuffer){
					if(bt != 0){
						listByte.add(bt);										
					}
				}	
			}
			// 结束标记
			if(isStart && readBuffer != null && readBuffer.length > 0 && readBuffer[readBuffer.length - 1] == 93){
				System.out.println("结束记录");
				isEnd = true; // 结束标记
				
				byte[] bts = new byte[listByte.size()];
				for(int i =0;i<listByte.size();i++){
					bts[i] = listByte.get(i);
				}
				String pmsg = new String(bts, "gbk").trim();
				ctx.add(pmsg);
				System.out.println("获取串口另一端发送过来的数据：" + pmsg);	
				System.out.println("上面数据字符长度[767]:" + pmsg.length());
				send(pmsg);
				listByte = null;
				bts = null;
				
				ctx.clear();
				isStart = false;
				isEnd = false;
			} else {
				System.out.println("没有");
			}
			// end 接收数据区域
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 发送信息到数据库
	 * @param msg
	 */
	private void send(String msg){
		for(String mg : ctx){
			System.out.println("截取内容：" + mg);
			Gson gson = new Gson();
			Type type = new TypeToken<List<SmsServerOut>>() {
			}.getType();
			List<SmsServerOut> list = gson.fromJson(msg, type);	
			new WDbHelper().insertSmsServerOutStatus(list);
		}
	}
	
	/**
	 * 测试
	 */
	public static void main(String[] args) {
		System.out.println("串口数据传输，获取串口数据，进程启动...");
		WR_Frame sf = new WR_Frame();
		sf.start();
	}
}
