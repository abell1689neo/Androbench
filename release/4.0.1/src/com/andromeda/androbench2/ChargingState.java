package com.andromeda.androbench2;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public class ChargingState {
	static final int ERROR = -1;
	static final String UNKNOWN = "unknown";
	public static String getChargingState(Context context){
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent batteryStatus = context.registerReceiver(null, ifilter);
		
		// Are we charging / charged?
		int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		boolean isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING || 
				status == BatteryManager.BATTERY_STATUS_FULL);
		
		if( isCharging ){
			int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
			switch(chargePlug){
				case BatteryManager.BATTERY_PLUGGED_USB:
					return "usb";
				case BatteryManager.BATTERY_PLUGGED_AC:
					return "ac";
				case BatteryManager.BATTERY_PLUGGED_WIRELESS:
					return "wireless";
				default :
					return UNKNOWN;
			}
			
		}else{
			return "not-charging";
		}
	}
	
	public static int getBatteryLevel(Context context){
		Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, ERROR);
		int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, ERROR);
		
		if( level == -1 || scale == -1) return ERROR;
		
		return (int) (( level / (double) scale) * 100);
	}
}
