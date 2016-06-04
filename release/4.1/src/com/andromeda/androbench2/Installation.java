package com.andromeda.androbench2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

import android.content.Context;


// not used, (for when android_id not worked);
public class Installation {
	private static String deviceID = null;
	private static final String INSTALLATION = ".INSTALLATION";
	
	public synchronized static String getId(Context context){
		if( deviceID == null){
			File installation = new File(context.getFilesDir(), INSTALLATION);
			try{
				if(!installation.exists()){
					writeInstallationFile(installation);
				}
				deviceID = readInstallationFile(installation);
			}catch(Exception e){
				throw new RuntimeException(e);
			}
		}
		return deviceID;
	}
	
	private static String readInstallationFile(File installation) throws IOException {
		RandomAccessFile f = new RandomAccessFile(installation, "r");
		byte[] bytes = new byte[(int) f.length()];
		f.readFully(bytes);
		f.close();
		return new String(bytes);
	}
	
	private static void writeInstallationFile(File installation) throws IOException {
		FileOutputStream out = new FileOutputStream(installation);
		String id = UUID.randomUUID().toString();
		out.write(id.getBytes());
		out.close();
	}
}
