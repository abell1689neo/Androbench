package com.andromeda.androbench2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class VersionCheck {
    static public String getKernelVersion(){
    	try{
    		String[] cmd = {"/system/bin/sh", "-c", "cat /proc/sys/kernel/osrelease"};
    		       		
    		Runtime operator = Runtime.getRuntime();
    		Process process = operator.exec(cmd);
    		
    		BufferedReader buffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
    		   		
    		String temp;
    		
    		while((temp = buffer.readLine()) != null){
   				return temp;
    		}
    	}catch(IOException e){
    	}
    	
    	return "Unknown";
    }
}
