package com.gymscale;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import android.content.Context;
import android.util.Log;
import java.util.Map;
import java.util.List;
import java.util.HashMap;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;

import com.facebook.react.bridge.Callback;

/*
Module to handle OTGSerial connection, it can :
	
	- openConnection
	- closeConnection
	- sendString

generate events :
		
	- onSerialConnect
	- onSerialDisconnect
	- onSerialData
*/

public class OTGModule extends ReactContextBaseJavaModule implements SerialInputOutputManager.Listener {
	private UsbSerialPort port = null;    
	ReactApplicationContext reactAppContext = null;

	OTGModule(ReactApplicationContext context) {
		super(context);
		reactAppContext = context;
	}

	@Override
	public String getName() {
		return "OTGModule";
	}

	private void sendEvent(ReactContext reactContext,
						   String eventName,
						   WritableMap params) {
		reactContext
			.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
			.emit(eventName, params);
	}

	@ReactMethod(isBlockingSynchronousMethod = true)
	public void openConnection() {
		try{
			Log.d("OTGModule","openConnection");
			// Find all available drivers from attached devices.
			UsbManager manager = (UsbManager) reactAppContext.getSystemService(Context.USB_SERVICE);
			List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
			if (availableDrivers.isEmpty()) {
				Log.d("OTGModule","openConnection no availableDrivers");
				return;
			}

			// Open a connection to the first available driver.
			UsbSerialDriver driver = availableDrivers.get(0);
			UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
			if (connection == null) {
				Log.d("OTGModule","openConnection connection is null");
				// add UsbManager.requestPermission(driver.getDevice(), ..) handling here
				return;
			}

			port = driver.getPorts().get(0); // Most devices have just one port (port 0)
			port.open(connection);
			port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

			SerialInputOutputManager usbIoManager = new SerialInputOutputManager(port, this);
			usbIoManager.start();

			WritableMap params = Arguments.createMap();
			sendEvent(reactAppContext, "onSerialConnect", params);
		}catch (Exception e){
			Log.e("OTGModule","Something went wrong inside openConnection", e);			
		}		
	}

	@ReactMethod(isBlockingSynchronousMethod = true)
	public void closeConnection(){
		Log.d("OTGModule","closeConnection");
		try{
			if(port != null){
				port.close();
			}
		}catch (Exception e){
			Log.e("OTGModule","Something went wrong inside closeConnection", e);
		}
	}

	@ReactMethod(isBlockingSynchronousMethod = true)
	public void sendString(String s){
		Log.d("OTGModule","sendString");
		try {
			if(port != null){
				port.write(s.getBytes(),500);
			}        
		}catch (Exception e){
			Log.e("OTGModule","Something went wrong inside sendString", e);			
		}	
	}

	@Override
	public void onNewData(byte[] data) {
		Log.d("OTGModule","onNewData");
        WritableMap params = Arguments.createMap();
		params.putString("data", new String(data));
		sendEvent(reactAppContext, "onSerialData", params);
	}

	@Override
	public void onRunError(Exception e) {
        Log.e("OTGModule","Error reported thru onRunError", e);
		WritableMap params = Arguments.createMap();
		params.putString("message", e.getMessage());
		sendEvent(reactAppContext, "onSerialDisconnect", params);
	}
	
}


