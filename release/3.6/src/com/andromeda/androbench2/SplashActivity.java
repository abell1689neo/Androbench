package com.andromeda.androbench2;

import com.andromeda.androbench2.R;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.WindowManager;

public class SplashActivity extends Activity
{
	
	public void onCreate(Bundle savedInstanceState)
	{
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        initialize();
	}

	private void initialize()
	{
    	Handler handler = new Handler(){
    		
    		public void handleMessage(Message msg){
    			finish();
    		}
    	};
    	
    	handler.sendEmptyMessageDelayed(0, 2000);
	}
}