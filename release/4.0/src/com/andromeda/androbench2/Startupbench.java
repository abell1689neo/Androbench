package com.andromeda.androbench2;

import com.andromeda.androbench2.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
public class Startupbench extends Activity{
	
	Context mContext;
	SharedPreferences sp_Flag;
	SharedPreferences.Editor sp_e_Flag;
	
	SharedPreferences sp_Default;
	SharedPreferences.Editor sp_e_Default;
	
    AlertDialog dlg_proc_bench;
    AlertDialog.Builder dlg_bld_proc_bench;
    
    private final static int KBYTE = 1024;
    private final static int MBYTE = 1024 * 1024;
    
    boolean flag_microbench;
    boolean flag_sqlitebench;
    boolean flag_macro;
	
    
    @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.startupbench);
        
        mContext = getApplicationContext();
        
        sp_Flag = getSharedPreferences("Flags", Context.MODE_PRIVATE);
        sp_e_Flag = sp_Flag.edit();
        
        Button testAllBTN = (Button)findViewById(R.id.btnStartingBenchmarking);
        testAllBTN.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				flag_microbench = true;
				flag_sqlitebench = true;
				flag_macro = true;
				askProcess();
			}
		});
        
        Button testMicroBTN = (Button)findViewById(R.id.btnStartingMicroBenchmarking);
        testMicroBTN.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				flag_microbench = true;
				flag_sqlitebench = false;
				flag_macro = false;
				askProcess();
			}
		});
        
        Button testSqliteBTN = (Button)findViewById(R.id.btnStartingSqliteBenchmarking);
        testSqliteBTN.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				flag_microbench = false;
				flag_sqlitebench = true;
				flag_macro = false;
				askProcess();
			}
		});
        
        Button testMacroBTN = (Button)findViewById(R.id.btnStartingMacroBenchmarking);
        testMacroBTN.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				flag_microbench = false;
				flag_sqlitebench = false;
				flag_macro = true;
				askProcess();
			}
		});
    }
    
    private void askProcess(){
		int flag_process_type = 0;
		int test_file_flag;
		String alertMessage;
	    String typeFileSystem;
	    
		int test_target = sp_Flag.getInt("TargetDevice", 0);
		
		int test_num_thread = sp_Flag.getInt("Num_Thread", 0);
		int test_total_file_size = sp_Flag.getInt("One_FileSize", 0) * test_num_thread;
		
		int test_file_size_read = sp_Flag.getInt("FileSize_RD", 0);
		int test_file_size_write = sp_Flag.getInt("FileSize_WR", 0);
	    
    	dlg_bld_proc_bench = new AlertDialog.Builder(this);
    	dlg_bld_proc_bench.setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			
			
			public void onClick(DialogInterface dlg_bench_progress, int which) {
				// TODO Auto-generated method stub
				sp_e_Flag.remove("StartingBench");
				sp_e_Flag.putBoolean("StartingBench", true);
				sp_e_Flag.commit();
				
				Intent intent = new Intent();
				intent.setAction("com.androbench.CHANGE_TAB");
				intent.putExtra("CHANGE_TARGET", 1);
				intent.putExtra("MICRO", flag_microbench);
				intent.putExtra("SQLITE", flag_sqlitebench);
				intent.putExtra("MACRO", flag_macro);
				sendBroadcast(intent);
				
			}}).setNegativeButton("No", new DialogInterface.OnClickListener() {
				
			
			public void onClick(DialogInterface dlg_bench_progress, int which) {
				// TODO Auto-generated method stub
				sp_e_Flag.remove("StartingBench");
				sp_e_Flag.putBoolean("StartingBench", false);
				sp_e_Flag.commit();
			}});
	    
    	dlg_proc_bench = dlg_bld_proc_bench.create();
    	
    	if(flag_microbench && flag_sqlitebench && flag_macro){
    		alertMessage = "Are you sure you want to run all benchmarks? (recommended)";
    	}else if(flag_microbench && !flag_sqlitebench && !flag_macro){
    		alertMessage = "Are you sure you want to run micro-benchmark?";
    	}else if(!flag_microbench && flag_sqlitebench && !flag_macro){
    		alertMessage = "Are you sure you want to run SQLite benchmark?";
    	}else if(!flag_microbench && !flag_sqlitebench && flag_macro){
    		alertMessage = "Are you sure you want to run Macro benchmark?";
    	}else{
    		alertMessage = "?";
    	}
    	
		dlg_proc_bench.setTitle("Benchmarking");
		dlg_proc_bench.setIcon(R.drawable.dlgicon_accepte);
		
		if(flag_microbench){
    		// Exception Processing(1) Partition available & free space 
    		if(test_target == 0){
    			if(MemoryStatus.getAvailableInternalMemorySize() < 0){
        			alertMessage = "Your '/data' partition is not available!\n\nWould you like to reset the configuration?";
        			dlg_proc_bench.setIcon(R.drawable.dlgicon_cancel);
        			flag_process_type = 1;
        		}else{
        			if(test_total_file_size > (MemoryStatus.getAvailableInternalMemorySize())){
            			alertMessage = "Your '/data' partition has not enough free space!\n(Free space = " + (MemoryStatus.getAvailableInternalMemorySize()/MBYTE) +"MB)\n\nWould you like to reset the configuration?";
            			dlg_proc_bench.setIcon(R.drawable.dlgicon_cancel);
            			flag_process_type = 1;
            		}
        		}
    		}else{
    			if(MemoryStatus.getAvailableSDExternalMemorySize(mContext) < 0){
        			alertMessage = "Your '" + MemoryStatus.getSDExternalPath(mContext) + "' partition is not available!\n\nWould you like to reset the configuration?";
        			dlg_proc_bench.setIcon(R.drawable.dlgicon_cancel);
        			flag_process_type = 1;
        		}else{
        			if(test_total_file_size > (MemoryStatus.getAvailableSDExternalMemorySize(mContext))){
            			alertMessage = "Your '" + MemoryStatus.getSDExternalPath(mContext) + "' partition has not enough free space!\n(Free space = " + (MemoryStatus.getAvailableSDExternalMemorySize(mContext)/MBYTE) + "MB)\n\nWould you like to reset the configuration?";
            			dlg_proc_bench.setIcon(R.drawable.dlgicon_cancel);
            			flag_process_type = 1;
            		}
        		}
    		}
    		
    		if(test_target == 0){
    			typeFileSystem = MemoryStatus.getInternalFilesystem();
    		}else{
    			typeFileSystem = MemoryStatus.getSDExternalFilesystem(mContext);
    		}
    		
    		if(typeFileSystem.equals("yaffs") || typeFileSystem.equals("yaffs2") || typeFileSystem.equals("ext2")){
    			test_file_flag = 0;
    		}else{
    			test_file_flag = 1;
    		}

    		
   			if((test_file_flag == 0) && (flag_process_type == 0)){
//   				if(test_target == 0){ // /data
//   					if(((((MemoryStatus.getTotalMem()/KBYTE)/2) + 50)) > (MemoryStatus.getAvailableExternalMemorySize())){
//   						alertMessage = "File System does not support DIRECT_IO.\nYou need more than " + (((((MemoryStatus.getTotalMem()/KBYTE)/2) + 50))) +"MB in '/sdcard' partition!";
//   						Toast.makeText(this, alertMessage, Toast.LENGTH_LONG).show();
//   						flag_process_type = 2;
//   	   				}else{
//   	   					alertMessage = "File System does not support DIRECT_IO.\nIt will take long time.\n\nAre you sure you want to run benchmark?";
//   	   					dlg_proc_bench.setIcon(R.drawable.dlgicon_warning);
//   	   				}
//   				}else if(test_target == 1){ // /sdcard
//   					if(((((MemoryStatus.getTotalMem()/KBYTE)/2) + 50)) + (test_file_size_read + test_file_size_write) > (MemoryStatus.getAvailableExternalMemorySize())){
//   						alertMessage = "File System does not support DIRECT_IO.\nYou need more than " + (((((MemoryStatus.getTotalMem()/KBYTE)/2) + 50)) + ((test_file_size_read + test_file_size_write) / MBYTE)) +"MB in '/sdcard' partition!";
//   						Toast.makeText(this, alertMessage, Toast.LENGTH_LONG).show();
//   						flag_process_type = 2;
//   	   				}else{
//   	   					alertMessage = "File System does not support DIRECT_IO.\nIt will take many time.\n\nAre you sure you want to run benchmark?";
//   	   					dlg_proc_bench.setIcon(R.drawable.dlgicon_warning);
//   	   				}
//   				}else{
//   					if(((((MemoryStatus.getTotalMem()/KBYTE)/2) + 50)) + (test_file_size_read + test_file_size_write) > (MemoryStatus.getAvailableSDExternalMemorySize())){
//   						alertMessage = "File System is not support DIRECT_IO.\nYou need more than " + (((((MemoryStatus.getTotalMem()/KBYTE)/2) + 50)) + ((test_file_size_read + test_file_size_write) / MBYTE)) +"MB in '" + MemoryStatus.getSDExternalPath() + "' partition!";
//   						Toast.makeText(this, alertMessage, Toast.LENGTH_LONG).show();
//   						flag_process_type = 2;
//   	   				}else{
//   	   					alertMessage = "File System is not support DIRECT_IO.\nIt will take many time.\n\nAre you sure you want to run benchmark?";
//   	   					dlg_proc_bench.setIcon(R.drawable.dlgicon_warning);
//   	   				}
//   				}
   				alertMessage = "To run microbenchmarks\nthe file system should support Direct I/O";
   	  			dlg_proc_bench.setIcon(R.drawable.dlgicon_warning);
			}
		}
			
		if(flag_process_type != 2){
        	dlg_proc_bench.setMessage(alertMessage);
        	dlg_proc_bench.show();
		}else{
			sp_e_Flag.remove("StartingBench");
			sp_e_Flag.putBoolean("StartingBench", false);
			sp_e_Flag.commit();
		}
    }
}
