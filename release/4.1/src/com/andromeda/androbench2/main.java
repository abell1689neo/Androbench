package com.andromeda.androbench2;

import com.andromeda.androbench2.R;

import android.app.Activity;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TabHost.OnTabChangeListener;



public class main extends TabActivity
implements OnTabChangeListener
{
	// Variable for resource of tab
	static TabHost tabHost;
	Resources res;
	TabHost.TabSpec spec;
	Intent intent;
	
	// Variable for environment of benchmarking
    SharedPreferences sp_Data;
    SharedPreferences.Editor sp_e_Data;
    SharedPreferences sp_Flag;
    SharedPreferences.Editor sp_e_Flag;
    
    static final int KBYTE = 1024;
    static final int MBYTE = 1024*1024;
    
    @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainactivity);
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // View Splash Activity
        startActivity(new Intent(this, SplashActivity.class));      
        
        // Initialize data values from default data
        sp_Data = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        sp_e_Data = sp_Data.edit();
        
        
        // every size saved as KB unit.
        sp_e_Data.putInt("TargetDevice", 0);
        sp_e_Data.putInt("IOType", 0);
        sp_e_Data.putInt("OneFileSize", 64 * 1024);
        sp_e_Data.putInt("TestRecs_RAND", 2048);
        
        sp_e_Data.putInt("BufferSize_SEQ",32 * 1024); 
        sp_e_Data.putInt("BufferSize_RND", 4);
        sp_e_Data.putInt("Num_Thread", 8 );
        sp_e_Data.putInt("Num_Sqlite", 1024);
        sp_e_Data.commit();
        
        // Changing status of Benchmarking(Not run benchmarking from tab click)
        sp_Flag = getSharedPreferences("Flags", Context.MODE_PRIVATE);
		sp_e_Flag = sp_Flag.edit();
		
		sp_e_Flag.putBoolean("StartingBench", false);
		sp_e_Flag.commit();
        
		
		// Setting Tab Environment
		res = getResources();
		tabHost = getTabHost();
		
		intent = new Intent().setClass(this, Startupbench.class);
		spec = tabHost.newTabSpec("tab_1").setIndicator(
				new MyTabView(this, "Measure"))
				.setContent(intent);
		tabHost.addTab(spec);
		
		intent = new Intent().setClass(this, Result.class);
		spec = tabHost.newTabSpec("tab_2").setIndicator(
				new MyTabView(this, "Results"))
				.setContent(intent);
		tabHost.addTab(spec);
		
		intent = new Intent().setClass(this, History.class);
		spec = tabHost.newTabSpec("tab_3").setIndicator(
				new MyTabView(this, "History"))
				.setContent(intent);
		tabHost.addTab(spec);
		
		intent = new Intent().setClass(this, Setting.class);
		spec = tabHost.newTabSpec("tab_4").setIndicator(
				new MyTabView(this, "Setting"))
				.setContent(intent);
		tabHost.addTab(spec);
		
		tabHost.setOnTabChangedListener(this);
    }
    
    
    @Override
	public void onResume(){
    	super.onResume();
    	IntentFilter mFilter = new IntentFilter("com.androbench.CHANGE_TAB");
    	registerReceiver(br_changeTab, mFilter);
    }
    
    // Define broadcast receiver for changing tab
    BroadcastReceiver br_changeTab = new BroadcastReceiver(){
    	@Override
		public void onReceive(Context context, Intent intent){
    		int changeTarget = 0;
    		
    		//intent's CHANGE_TARGET = which want change
    		changeTarget = intent.getIntExtra("CHANGE_TARGET", 0);
    		
            sp_Flag = getSharedPreferences("Flags", Context.MODE_PRIVATE);
    		sp_e_Flag = sp_Flag.edit();
    		sp_e_Flag.putBoolean("MicroBench", intent.getBooleanExtra("MICRO", false));
    		sp_e_Flag.putBoolean("SqliteBench", intent.getBooleanExtra("SQLITE", false));
    		sp_e_Flag.putBoolean("MacroBench", intent.getBooleanExtra("MACRO", false));
    		sp_e_Flag.commit();    		
    		
    		tabHost.setCurrentTab(changeTarget);
    	}
    };
    
    
    @Override
	public void onStop(){
    	super.onStop();
    	
    	// Unregister broadcast receiver for changing tab
    	unregisterReceiver(br_changeTab);
    }
	
	public void onTabChanged(String tabId) {
	    // Control changing tab which want change
		if(tabId == "tab_1"){
			tabHost.getTabWidget().setBackgroundResource(R.drawable.tab_benchmarking);
		}else if(tabId == "tab_2"){
			tabHost.getTabWidget().setBackgroundResource(R.drawable.tab_result);
		}else if(tabId == "tab_3"){
			tabHost.getTabWidget().setBackgroundResource(R.drawable.tab_history);
		}else if(tabId == "tab_4"){
			tabHost.getTabWidget().setBackgroundResource(R.drawable.tab_setting);
		}
	}
	
	private class MyTabView extends LinearLayout {
		public MyTabView(Context c, String label) {
			super(c);
			TextView tv = new TextView(c);
			tv.setText("\n\n" + label);
			tv.setTextColor(Color.WHITE);
			tv.setGravity(0x01);
			setOrientation(LinearLayout.VERTICAL);
			addView(tv);
		}
	}
}