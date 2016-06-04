package com.andromeda.androbench2;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

public class MemoryStatus {

        static final int ERROR = -1;
        static final String UNKNOWN = "unknown";
        static final String ERROR_STR = "-1";
        
        // Check external memory is available
        static public boolean externalMemoryAvailable() {
            return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
        }
        
        @SuppressWarnings("deprecation")
		static public long getAvailableInternalMemorySize() {
        	
        		boolean availDataPath = false;
        	
	        	try{
	        		String[] cmd = {"/system/bin/sh", "-c", "cat /proc/mounts"};
	        		       		
	        		Runtime operator = Runtime.getRuntime();
	        		Process process = operator.exec(cmd);
	        		
	        		BufferedReader buffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
	        		
	        		String temp;
	        		String[] temp_split;
	        		
	        		while((temp = buffer.readLine()) != null){
	        			temp_split = temp.split(" ");
	        			if(temp_split[1].equals("/data")){
	        				availDataPath = true;
	        			}
	        		}
	        	}catch(IOException e){
	        	}
        	
	        	if(availDataPath){
	                File path = Environment.getDataDirectory();
	                StatFs stat = new StatFs(path.getPath());
	                
	                long blockSize;
	                long availableBlocks;
	                
	                if( Build.VERSION.SDK_INT < 18){
	                	blockSize = stat.getBlockSize();
	                	availableBlocks = stat.getAvailableBlocks();
	                	return availableBlocks * blockSize;
	                }else{
	                	return stat.getAvailableBytes();
	                }
	        	}else{
	        		return ERROR;
	        	}
        }
        
        @SuppressWarnings("deprecation")
		static public long getTotalInternalMemorySize() {
                File path = Environment.getDataDirectory();
                StatFs stat = new StatFs(path.getPath());
                long blockSize = stat.getBlockSize();
                long totalBlocks = stat.getBlockCount();
                return totalBlocks * blockSize;
        }
        
        @SuppressWarnings("deprecation")
		static public long getTotalSDExternalMemorySize(Context context) {
        	if (getSDExternalPath(context) != UNKNOWN){
        		File path = new File(getSDExternalPath(context));
                StatFs stat = new StatFs(path.getPath());
                long blockSize = stat.getBlockSize();
                long totalBlocks = stat.getBlockCount();
                return totalBlocks * blockSize;
        	}
        	return ERROR;
        }
        
        @SuppressWarnings("deprecation")
		static public long getAvailableSDExternalMemorySize(Context context) {
        	if (getSDExternalPath(context) != UNKNOWN){
        		File path = new File(getSDExternalPath(context));
                StatFs stat = new StatFs(path.getPath());
                long blockSize;
                long availableBlocks;
                
                if( Build.VERSION.SDK_INT < 18){
                	blockSize = stat.getBlockSize();
                	availableBlocks = stat.getAvailableBlocks();
                	return availableBlocks * blockSize;
                }else{
                	return stat.getAvailableBytes();
                }
        	}
        	return ERROR;
        }
       
        
        static public int getTotalMem(){
        	try{
        		String[] cmd = {"/system/bin/sh", "-c", "cat /proc/meminfo"};
        		
        		Runtime operator = Runtime.getRuntime();
        		Process process = operator.exec(cmd);
        		
        		BufferedReader buffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
        		
        		String temp;
        		String[] temp_split;
        		String[] space_split;
        		       		
        		temp = buffer.readLine();
        		temp_split = temp.split(":");
        		space_split = temp_split[1].split("kB");
        		
        		int total_mem = Integer.parseInt(space_split[0].trim());
        		
        		return total_mem;
        		
        	}catch(IOException e){
        	}
        	
        	return 0;
        }
        
        
        static public String getInternalFilesystem(){
        	try{
        		String[] cmd = {"/system/bin/sh", "-c", "cat /proc/mounts"};
        		       		
        		Runtime operator = Runtime.getRuntime();
        		Process process = operator.exec(cmd);
        		
        		BufferedReader buffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
        		
        		String temp;
        		String[] temp_split;
        		
        		while((temp = buffer.readLine()) != null){
        			temp_split = temp.split(" ");
        			if(temp_split[1].equals("/data")){
        				return temp_split[2];
        			}
        		}
        	}catch(IOException e){
        	}
        	
        	return UNKNOWN;
        }
        
        static public String getSDExternalFilesystem(Context context){
        	try{
        		String[] cmd = {"/system/bin/sh", "-c", "cat /proc/mounts"};
        		       		
        		Runtime operator = Runtime.getRuntime();
        		Process process = operator.exec(cmd);
        		
        		BufferedReader buffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
        		
        		String temp;
        		String[] temp_split;
        		String sd_external_path = getSDExternalPath(context);
        		
        		while((temp = buffer.readLine()) != null){
        			temp_split = temp.split(" ");
        			if(temp_split[1].equals(sd_external_path)){
        				return temp_split[2];
        			}
        		}
        	}catch(IOException e){
        	}
        	
        	return UNKNOWN;
        }
        
        static public String getInternalScheduler(){
        	try{
        		
        		
        		String blk_dev_name = GetInfo.getInternalDevName();
        		//System.out.println("block dev name : " + blk_dev_name);
        		
        		String[] cmd = {"/system/bin/sh", "-c", "cat /sys/block/" + blk_dev_name + "/queue/scheduler"};
        		
        		Runtime operator = Runtime.getRuntime();
        		Process process = operator.exec(cmd);
        		
        		BufferedReader buffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
        		
        		String temp;
        		String[] temp_split;
        		
        		while((temp = buffer.readLine()) != null){
        			temp_split = temp.split(" ");
        			
        			for(int i = 0;i<temp_split.length;i++){
        				if( temp_split[i].charAt(0) == '['){
        					return temp_split[i].substring(1, temp_split[i].length() - 1);
        				}
        			}
        		}
        	}catch(IOException e){
        	}
        	return UNKNOWN;
        }
        
        static public String getSDExternalScheduler(){
        	try{
        		// [To-Do] fix hardcoding (mmcblk1)
        		String[] cmd = {"/system/bin/sh", "-c", "cat /sys/block/mmcblk1/queue/scheduler"};
        		
        		Runtime operator = Runtime.getRuntime();
        		Process process = operator.exec(cmd);
        		
        		BufferedReader buffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
        		
        		String temp;
        		String[] temp_split;
        		
        		while((temp = buffer.readLine()) != null){
        			temp_split = temp.split(" ");
        			
        			for(int i = 0;i<temp_split.length;i++){
        				if( temp_split[i].charAt(0) == '['){
        					return temp_split[i].substring(1, temp_split[i].length() - 1);
        				}
        			}
        		}
        	}catch(IOException e){
        	}
        	return UNKNOWN;
        }
        
        static public String getGovernor(){
        	try{
        		String[] cmd = {"/system/bin/sh", "-c", "cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"};
        		
        		Runtime operator = Runtime.getRuntime();
        		Process process = operator.exec(cmd);
        		
        		BufferedReader buffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
        		
        		String temp;
        		
        		temp = buffer.readLine();
        		if( temp != null ) return temp;
        	}catch(IOException e){
        	}
        	return UNKNOWN;
        }
        
        static public String getGovernor_IO_IS_BUSY(){
        	try{
        		String governor = getGovernor();
        		if( !governor.equals("-")){
        			String[] cmd = {"/system/bin/sh", "-c", "cat /sys/devices/system/cpu/cpufreq/" + governor + "/io_is_busy"};
            		
            		Runtime operator = Runtime.getRuntime();
            		Process process = operator.exec(cmd);
            		
            		BufferedReader buffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
            		
            		String temp;
            		
            		temp = buffer.readLine();
            		
            		if( temp != null) return temp;
        		}
        	}catch(IOException e){
        	}
        	return ERROR_STR;
        }
        
        static public String getSDExternalPath(Context context){  	
        	if( Build.VERSION.SDK_INT >= 19){
        		File[] tmp_files = context.getExternalFilesDirs(null);
        		if( tmp_files.length > 1){
        			String common_path = tmp_files[0].toString().replaceFirst(Environment.getExternalStorageDirectory().getAbsolutePath(), "");
        			
        			return tmp_files[1].toString().replaceFirst(common_path, "");
        		}
        	}
    		// when api level (SDK_INT) < 19, only micro_sdcard on /storage/extSdCard can return path
        	try{
        		String[] cmd = {"/system/bin/sh", "-c", "cat /proc/mounts"};
        		
        		Runtime operator = Runtime.getRuntime();
        		Process process = operator.exec(cmd);
        		
        		BufferedReader buffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
        		
        		String temp;
        		String[] temp_split;
        		
        		while((temp = buffer.readLine()) != null){
        			temp_split = temp.split(" ");
        			if(temp_split[1].equals("/storage/extSdCard")){
        				return temp_split[1];
        			}
        		}
        	}catch(IOException e){
        	}
        	return UNKNOWN;
        }
        
        static public String formatSize(long size) {
                String suffix = null;
        
                if (size >= 1024) {
                        suffix = "KiB";
                        size /= 1024;
                        if (size >= 1024) {
                                suffix = "MiB";
                                size /= 1024;
                        }
                }
        
                StringBuilder resultBuffer = new StringBuilder(Long.toString(size));
        
                int commaOffset = resultBuffer.length() - 3;
                while (commaOffset > 0) {
                        resultBuffer.insert(commaOffset, ',');
                        commaOffset -= 3;
                }
        
                if (suffix != null)
                        resultBuffer.append(suffix);
                return resultBuffer.toString();
        }
}