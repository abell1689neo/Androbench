package com.andromeda.androbench2;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import android.os.Environment;
import android.os.StatFs;

public class MemoryStatus {

        static final int ERROR = -1;
        
        // Check external memory is available
        static public boolean externalMemoryAvailable() {
            return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
        }
        
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
	                long blockSize = stat.getBlockSize();
	                long availableBlocks = stat.getAvailableBlocks();
	                return availableBlocks * blockSize;
	        	}else{
	        		return ERROR;
	        	}
        }
        
        static public long getTotalInternalMemorySize() {
                File path = Environment.getDataDirectory();
                StatFs stat = new StatFs(path.getPath());
                long blockSize = stat.getBlockSize();
                long totalBlocks = stat.getBlockCount();
                return totalBlocks * blockSize;
        }
        
        static public long getAvailableExternalMemorySize() {
                if(externalMemoryAvailable()) {
                        File path = Environment.getExternalStorageDirectory();
                        StatFs stat = new StatFs(path.getPath());
                        long blockSize = stat.getBlockSize();
                        long availableBlocks = stat.getAvailableBlocks();
                        return availableBlocks * blockSize;
                } else {
                        return ERROR;
                }
        }
        
        static public long getAvailableSDExternalMemorySize() {
        	File path = new File(getSDExternalPath());
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSize();
            long totalBlocks = stat.getBlockCount();
            return totalBlocks * blockSize;
        }
        
        static public long getTotalExternalMemorySize() {
            if(externalMemoryAvailable()) {
                    File path = Environment.getExternalStorageDirectory();
                    StatFs stat = new StatFs(path.getPath());
                    long blockSize = stat.getBlockSize();
                    long totalBlocks = stat.getBlockCount();
                    return totalBlocks * blockSize;
            } else {
                    return ERROR;
            }
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
        	
        	return "Unknown";
        }
        
        static public String getExternalFilesystem(){
        	try{
        		String[] cmd = {"/system/bin/sh", "-c", "cat /proc/mounts"};
        		       		
        		Runtime operator = Runtime.getRuntime();
        		Process process = operator.exec(cmd);
        		
        		BufferedReader buffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
        		
        		String temp;
        		String[] temp_split;
        		
        		while((temp = buffer.readLine()) != null){
        			temp_split = temp.split(" ");
        			if(temp_split[1].equals("/mnt/sdcard")){
        				return temp_split[2];
        			}
        		}
        	}catch(IOException e){
        	}
        	
        	return "Unknown";
        }
        
        static public String getSDExternalFilesystem(){
        	try{
        		String[] cmd = {"/system/bin/sh", "-c", "cat /proc/mounts"};
        		       		
        		Runtime operator = Runtime.getRuntime();
        		Process process = operator.exec(cmd);
        		
        		BufferedReader buffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
        		
        		String temp;
        		String[] temp_split;
        		
        		while((temp = buffer.readLine()) != null){
        			temp_split = temp.split(" ");
        			if(temp_split[1].equals("/mnt" + getSDExternalPath())){
        				return temp_split[2];
        			}
        		}
        	}catch(IOException e){
        	}
        	
        	return "Unknown";
        }
        
        static public String getSDExternalPath(){
        	try{
        		String[] cmd = {"/system/bin/sh", "-c", "cat /proc/mounts"};
        		
        		Runtime operator = Runtime.getRuntime();
        		Process process = operator.exec(cmd);
        		
        		BufferedReader buffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
        		
        		String temp;
        		String[] temp_split;
        		String temp_path;
        		String temp_link_path;
        		
        		while((temp = buffer.readLine()) != null){
        			temp_split = temp.split(" ");
        			if(temp_split[1].length()>=12){
            			temp_path = temp_split[1].substring(0,12);
            			if(temp_path.equals("/mnt/sdcard/") && !(temp_split[1].substring(0,13).equals("/mnt/sdcard/."))){
            				temp_link_path = temp_split[1].substring(4);
            				return temp_link_path;
            			}
        			}
        		}
        	}catch(IOException e){
        	}
        	return "Unknown";
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