package com.andromeda.androbench2;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.provider.Settings.Secure;
import android.util.Pair;

public class GetInfo {

        static final int ERROR = -1;
        static final int STATFS_SIZE = 7; 
        static final String UNKNOWN = "unknown"; 
        static final String ERROR_STR = "-1";
        static final int STATFS_BLOCKS		= 0;
        static final int STATFS_BFREE		= 1;
        static final int STATFS_BAVAIL		= 2;
        static final int STATFS_FILES		= 3;
        static final int STATFS_FFREE		= 4;
        static final int STATFS_BSIZE		= 5;
        static final int STATFS_FRSIZE		= 6;
        
        
        static {
    		System.loadLibrary("Interface_JNI");
    	}
        
        static public native int GET_STATFS_FROM_PATH(String path, long [] stat_arr);
    	//static public native int GET_STATFS_FROM_PATH(String path, long [] stat_arr);
    	
    	static public long [] getStatfsFromPath(String path){
    		long [] ret = new long[STATFS_SIZE];
    		GET_STATFS_FROM_PATH(path, ret);
    		
    		return ret;
    	}
    	
    	static public String getCpuHardware(){
        	String hardware = UNKNOWN;
        	try{
        		String[] cmd = {"/system/bin/sh", "-c", "cat /proc/cpuinfo"};
        		       		
        		Runtime operator = Runtime.getRuntime();
        		Process process = operator.exec(cmd);
        		
        		BufferedReader buffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
        		
        		String tmp;
        		String[] tmp_split;
        		
        		while((tmp = buffer.readLine()) != null){
        			tmp_split = tmp.split(" |\t");
        			if(tmp_split[0].equals("Hardware")){
        				hardware = "";
        				for(int i=2; i < tmp_split.length; i++)
        					hardware += tmp_split[i] + " ";
        				return hardware;
        			}
        		}
        	}catch(IOException e){
        	}
        	return hardware;
    	}
    	
    	
    	
    	// ex. mmcblk0
        static public String getInternalDataPartitionDevName(){
        	String tmp_dev_path = UNKNOWN;
        	try{
        		String[] cmd = {"/system/bin/sh", "-c", "cat /proc/mounts"};
        		       		
        		Runtime operator = Runtime.getRuntime();
        		Process process = operator.exec(cmd);
        		
        		BufferedReader buffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
        		
        		String tmp;
        		String[] tmp_split;
        		
        		while((tmp = buffer.readLine()) != null){
        			tmp_split = tmp.split(" ");
        			if(tmp_split[1].equals("/data")){
        				tmp_dev_path = tmp_split[0];
        			}
        		}
        	}catch(IOException e){
        	}
        	if( tmp_dev_path.equals(UNKNOWN) ) return tmp_dev_path;
        	
        	String partition_name = UNKNOWN;
        	try {
        		// canonical for ignoring link file
        		String dev_path = new File(tmp_dev_path).getCanonicalPath().toString();
        		System.out.println("dev_path : " + dev_path);
  				String[] dev_path_split = dev_path.split("/");
  				if(dev_path_split.length > 0){
  					partition_name = dev_path_split[dev_path_split.length - 1];
        		}
  				
  				// check the file is block special file
  				String[] cmd = {"/system/bin/sh", "-c", "ls -l " + dev_path};
	       		
        		Runtime operator = Runtime.getRuntime();
        		Process process = operator.exec(cmd);
        		
        		BufferedReader buffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
        		String tmp_ls_out;
        		
        		if((tmp_ls_out = buffer.readLine()) != null)
        			if(tmp_ls_out.charAt(0) != 'b') return UNKNOWN;  
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} 
        	return partition_name;
        }
        
        static public String getInternalDevName(){
        	String data_partition_dev_name = GetInfo.getInternalDataPartitionDevName();
    		int tmp_idx = data_partition_dev_name.lastIndexOf('p');
    		if (tmp_idx <= 0) return UNKNOWN;
    		
    		return data_partition_dev_name.substring(0, tmp_idx);
        }
        
        
        //now for only ext4 & internal device
        static public String getLifetimeWriteKbytes(){
        	
        	String dev_name = getInternalDataPartitionDevName();
        	
        	if( dev_name.equals(UNKNOWN)) return ERROR_STR;
        	if( !MemoryStatus.getInternalFilesystem().equals("ext4")) return ERROR_STR;
        	try{
        		String[] cmd = {"/system/bin/sh", "-c", "cat /sys/fs/ext4/" + dev_name +"/lifetime_write_kbytes"};
        		       		
        		Runtime operator = Runtime.getRuntime();
        		Process process = operator.exec(cmd);
        		
        		BufferedReader buffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
        		
        		String tmp;
        		
        		if((tmp = buffer.readLine()) != null){
        			return tmp;
        		}
        	}catch(IOException e){
        	}
        	return ERROR_STR;
        }  
     
        
        static public String info_to_str(Context context, int id_target){
        	ArrayList<Pair<String, String>> info_list = new ArrayList<Pair<String, String>>();
        	
        	String target;
        	String filesystem;
        	String io_scheduler;
        	String total_space;
        	String free_space;
        	String available_space;
        	String total_filenodes;
        	String free_filenodes;
        	String block_size;
        	String lifetime_write_kbytes;
        	long[] statfs_info;
        	
        	if( id_target == 0){
        		target = "/data";
        		filesystem = MemoryStatus.getInternalFilesystem();
        		io_scheduler = MemoryStatus.getInternalScheduler();

        		lifetime_write_kbytes = getLifetimeWriteKbytes();
        	}
        	else{
        		target = MemoryStatus.getSDExternalPath(context);
        		filesystem = MemoryStatus.getSDExternalScheduler();
        		io_scheduler = MemoryStatus.getSDExternalScheduler();
        		
        		lifetime_write_kbytes = ERROR_STR;
        	}
        	statfs_info = getStatfsFromPath(target);
    		
        	if( statfs_info[STATFS_BLOCKS] != -1){
        		total_space = Long.toString(statfs_info[STATFS_BLOCKS] * statfs_info[STATFS_BSIZE]);
        		free_space = Long.toString(statfs_info[STATFS_BFREE] * statfs_info[STATFS_BSIZE]);
        		available_space = Long.toString(statfs_info[STATFS_BAVAIL] * statfs_info[STATFS_BSIZE]);
        		total_filenodes = Long.toString(statfs_info[STATFS_FILES]);
        		free_filenodes = Long.toString(statfs_info[STATFS_FFREE]);
        		block_size = Long.toString(statfs_info[STATFS_BSIZE]);
        	}else{
        		total_space = ERROR_STR;
        		free_space = ERROR_STR;
        		available_space = ERROR_STR;
        		total_filenodes = ERROR_STR;
        		free_filenodes = ERROR_STR;
        		block_size = ERROR_STR;
        	}
        	
        	// [To-Do] info_list needed? 
        	info_list.add(new Pair<String, String>( "target", target));
        	info_list.add(new Pair<String, String>( "filesystem", filesystem));
        	info_list.add(new Pair<String, String>( "io_scheduler", io_scheduler));
        	info_list.add(new Pair<String, String>( "total_space", total_space));
        	info_list.add(new Pair<String, String>( "free_space", free_space));
        	info_list.add(new Pair<String, String>( "available_space", available_space));
        	info_list.add(new Pair<String, String>( "total_filenodes", total_filenodes));
        	info_list.add(new Pair<String, String>( "free_filenodes", free_filenodes));
        	info_list.add(new Pair<String, String>( "block_size", block_size));
        	info_list.add(new Pair<String, String>( "lifetime_write_kbytes", lifetime_write_kbytes));
        	info_list.add(new Pair<String, String>( "available_processors", Integer.toString( Runtime.getRuntime().availableProcessors())));
        	info_list.add(new Pair<String, String>( "governor", MemoryStatus.getGovernor()));
        	info_list.add(new Pair<String, String>( "io_is_busy", MemoryStatus.getGovernor_IO_IS_BUSY()));
        	info_list.add(new Pair<String, String>( "cpu_hardware", getCpuHardware()));
        	info_list.add(new Pair<String, String>( "charging_status", ChargingState.getChargingState(context)));
        	info_list.add(new Pair<String, String>( "charging_level", Integer.toString( ChargingState.getBatteryLevel(context))));
        	info_list.add(new Pair<String, String>( "android_id", Secure.getString(context.getContentResolver(), Secure.ANDROID_ID)));
        	try {
				info_list.add(new Pair<String, String>( "androbench_ver", context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName));
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				info_list.add(new Pair<String, String>( "androbench_ver", UNKNOWN));
			}
        	info_list.add(new Pair<String, String>( "kernel_ver", VersionCheck.getKernelVersion()));
        	info_list.add(new Pair<String, String>( "b_version_codename", Build.VERSION.CODENAME));
        	info_list.add(new Pair<String, String>( "b_version_incremental", Build.VERSION.INCREMENTAL));
        	info_list.add(new Pair<String, String>( "b_version_release", Build.VERSION.RELEASE ));
        	info_list.add(new Pair<String, String>( "b_version_sdk_int", Integer.toString( Build.VERSION.SDK_INT)));
        	info_list.add(new Pair<String, String>( "b_brand", Build.BRAND));
        	info_list.add(new Pair<String, String>( "b_device", Build.DEVICE));
        	info_list.add(new Pair<String, String>( "b_display", Build.DISPLAY));
        	info_list.add(new Pair<String, String>( "b_hardware", Build.HARDWARE));
        	info_list.add(new Pair<String, String>( "b_id", Build.ID));
        	info_list.add(new Pair<String, String>( "b_manufacturer", Build.MANUFACTURER));
        	info_list.add(new Pair<String, String>( "b_model", Build.MODEL));
        	info_list.add(new Pair<String, String>( "b_product", Build.PRODUCT));
        	
        	String ret = "";
        	for(int i=0;i<info_list.size();i++){
        		ret += "&" + info_list.get(i).first + "=" + info_list.get(i).second;
        		//System.out.println("info_list : (" + info_list.get(i).first + ", " + info_list.get(i).second + ")" );
        	}
        	return ret;
        }
        
        static public ArrayList<NameValuePair> info_to_Arrs(Context context, int id_target){
        	ArrayList<NameValuePair> infoData = new ArrayList<NameValuePair>();
        	
        	String target;
        	String filesystem;
        	String io_scheduler;
        	String total_space;
        	String free_space;
        	String available_space;
        	String total_filenodes;
        	String free_filenodes;
        	String block_size;
        	String lifetime_write_kbytes;
        	long[] statfs_info;
        	
        	if( id_target == 0){
        		target = "/data";
        		filesystem = MemoryStatus.getInternalFilesystem();
        		io_scheduler = MemoryStatus.getInternalScheduler();

        		lifetime_write_kbytes = getLifetimeWriteKbytes();
        	}
        	else{
        		target = MemoryStatus.getSDExternalPath(context);
        		filesystem = MemoryStatus.getSDExternalScheduler();
        		io_scheduler = MemoryStatus.getSDExternalScheduler();
        		
        		lifetime_write_kbytes = ERROR_STR;
        	}
        	statfs_info = getStatfsFromPath(target);
    		
        	if( statfs_info[STATFS_BLOCKS] != -1){
        		total_space = Long.toString(statfs_info[STATFS_BLOCKS] * statfs_info[STATFS_BSIZE]);
        		free_space = Long.toString(statfs_info[STATFS_BFREE] * statfs_info[STATFS_BSIZE]);
        		available_space = Long.toString(statfs_info[STATFS_BAVAIL] * statfs_info[STATFS_BSIZE]);
        		total_filenodes = Long.toString(statfs_info[STATFS_FILES]);
        		free_filenodes = Long.toString(statfs_info[STATFS_FFREE]);
        		block_size = Long.toString(statfs_info[STATFS_BSIZE]);
        	}else{
        		total_space = ERROR_STR;
        		free_space = ERROR_STR;
        		available_space = ERROR_STR;
        		total_filenodes = ERROR_STR;
        		free_filenodes = ERROR_STR;
        		block_size = ERROR_STR;
        	}
        	
        	// [To-Do] info_list needed? 
        	infoData.add(new BasicNameValuePair( "target", target));
        	infoData.add(new BasicNameValuePair( "filesystem", filesystem));
        	infoData.add(new BasicNameValuePair( "io_scheduler", io_scheduler));
        	infoData.add(new BasicNameValuePair( "total_space", total_space));
        	infoData.add(new BasicNameValuePair( "free_space", free_space));
        	infoData.add(new BasicNameValuePair( "available_space", available_space));
        	infoData.add(new BasicNameValuePair( "total_filenodes", total_filenodes));
        	infoData.add(new BasicNameValuePair( "free_filenodes", free_filenodes));
        	infoData.add(new BasicNameValuePair( "block_size", block_size));
        	infoData.add(new BasicNameValuePair( "lifetime_write_kbytes", lifetime_write_kbytes));
        	infoData.add(new BasicNameValuePair( "available_processors", Integer.toString( Runtime.getRuntime().availableProcessors())));
        	infoData.add(new BasicNameValuePair( "governor", MemoryStatus.getGovernor()));
        	infoData.add(new BasicNameValuePair( "io_is_busy", MemoryStatus.getGovernor_IO_IS_BUSY()));
        	infoData.add(new BasicNameValuePair( "cpu_hardware", getCpuHardware()));
        	infoData.add(new BasicNameValuePair( "charging_status", ChargingState.getChargingState(context)));
        	infoData.add(new BasicNameValuePair( "charging_level", Integer.toString( ChargingState.getBatteryLevel(context))));
        	infoData.add(new BasicNameValuePair( "android_id", Secure.getString(context.getContentResolver(), Secure.ANDROID_ID)));
        	try {
				infoData.add(new BasicNameValuePair( "androbench_ver", context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName));
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				infoData.add(new BasicNameValuePair( "androbench_ver", UNKNOWN));
			}
        	infoData.add(new BasicNameValuePair( "kernel_ver", VersionCheck.getKernelVersion()));
        	infoData.add(new BasicNameValuePair( "b_version_codename", Build.VERSION.CODENAME));
        	infoData.add(new BasicNameValuePair( "b_version_incremental", Build.VERSION.INCREMENTAL));
        	infoData.add(new BasicNameValuePair( "b_version_release", Build.VERSION.RELEASE ));
        	infoData.add(new BasicNameValuePair( "b_version_sdk_int", Integer.toString( Build.VERSION.SDK_INT)));
        	infoData.add(new BasicNameValuePair( "b_brand", Build.BRAND));
        	infoData.add(new BasicNameValuePair( "b_device", Build.DEVICE));
        	infoData.add(new BasicNameValuePair( "b_display", Build.DISPLAY));
        	infoData.add(new BasicNameValuePair( "b_hardware", Build.HARDWARE));
        	infoData.add(new BasicNameValuePair( "b_id", Build.ID));
        	infoData.add(new BasicNameValuePair( "b_manufacturer", Build.MANUFACTURER));
        	infoData.add(new BasicNameValuePair( "b_model", Build.MODEL.toString().replaceAll(" ", "%20")));
        	infoData.add(new BasicNameValuePair( "b_product", Build.PRODUCT));
        	
        	return infoData;
        }
}