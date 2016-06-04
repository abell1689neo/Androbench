package com.andromeda.androbench2;

import java.io.File;
import java.util.ArrayList;

import com.andromeda.androbench2.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class Setting extends Activity
{
	
	private ArrayList<Data> al_Settings;
	private ListView listview;
	private GroupAdapter adapter;
	
    SharedPreferences sp_Data;
    SharedPreferences.Editor sp_e_Data;
    
    Context mContext;
    
    TextView tv_available_filesize_info;
    TextView tv_total_fs;
    TextView tv_one_fs;
    TextView tv_rnd_ts; //test size
    
    TextView tv_seq_bs;
    TextView tv_rnd_bs;
    
    TextView tv_thread;
    TextView tv_rnd_recs; // #of rnd ops
    
    TextView tv_transaction;
    
    // [To-Do] will be removed, (this for getting info)
    TextView tv_helpinfo;
    
    static final int KBYTE = 1024;
    static final int MBYTE = 1024*1024;
	static final String UNKNOWN = "unknown";
	
	static {
		System.loadLibrary("Interface_JNI");
	}
	// return fd when file open success, else return -1 
	static public native int FILE_OPEN(String path);
    
    @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);
                
        mContext = getApplicationContext();
        
        sp_Data = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        sp_e_Data = sp_Data.edit();
        
        listview = (ListView)findViewById(R.id.SettingListView);
        
        al_Settings=new ArrayList<Data>();
             
        String str_TargetDevice;
        //String str_IOType;
                               
        if(sp_Data.getInt("TargetDevice", 0) == 0){
        	str_TargetDevice = "/data";
        }else{
        	str_TargetDevice = UNKNOWN;
        }
 
        al_Settings.add(new Data("Target Partition", "Select partition to test", str_TargetDevice));
        al_Settings.add(new Data("File Size", "Change read/write file size", "" + sp_Data.getInt("OneFileSize", 0) / 1024 + " MB"));
        al_Settings.add(new Data("Buffer Size", "Change buffer size for sequential & random read/write", "SEQ: " + sp_Data.getInt("BufferSize_SEQ", 0) + " KB" + "  RND: " + sp_Data.getInt("BufferSize_RND", 0) + " KB"));
        al_Settings.add(new Data("Threads", "Change number of threads for random read/write", "Threads: " + sp_Data.getInt("Num_Thread",0)));
        al_Settings.add(new Data("SQLite Transactions", "Change number of transactions for SQLite", "" + sp_Data.getInt("Num_Sqlite",0)));
        al_Settings.add(new Data("Help", "How to use this application", ""));
        try {
			al_Settings.add(new Data("About", "About this application", "Ver" + getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        if(al_Settings.size()>0){
        	adapter = new GroupAdapter(this, R.layout.row_setting, al_Settings);
        	listview.setAdapter(adapter);
        }
        
        listview.setOnItemClickListener(new OnItemClickListener(){
        	
        	public void onItemClick(AdapterView<?> parent, View view, int position, long id){
        		ItemClick(parent, view, position, id);
        	}
        });
        
    }
       
    private void setTargetDevice(int which){
		sp_e_Data.remove("TargetDevice");
		sp_e_Data.putInt("TargetDevice", which);
		sp_e_Data.commit();
		
		String str_TargetDevice;
        
        if(sp_Data.getInt("TargetDevice", 0) == 0){
        	str_TargetDevice = "/data";
        }else{
        	str_TargetDevice = MemoryStatus.getSDExternalPath(mContext);
        }
		
        al_Settings.remove(0);
		al_Settings.add(0, new Data("Target Partition", "Select partition to test", str_TargetDevice));
		
		listview.setAdapter(adapter);
    }
    
    // unit of size is KB 
    private void setTargetOneFileSize(int mTargetOneFileSize){
    	int rnd_recs = sp_Data.getInt("TestRecs_RAND", 0);
    	int num_thread = sp_Data.getInt("Num_Thread", 0);
    	int buffer_size_seq = sp_Data.getInt("BufferSize_SEQ", 0);
    	int buffer_size_rnd = sp_Data.getInt("BufferSize_RND", 0);
    	int available_file_size = 0;
    	
    	if(sp_Data.getInt("TargetDevice", 0) == 0){ // /data
        	available_file_size = (int)(MemoryStatus.getAvailableInternalMemorySize() / 1024);
        }else if(sp_Data.getInt("TargetDevice", 0) == 1){ // /storage/extSdCard
        	available_file_size = (int)(MemoryStatus.getAvailableSDExternalMemorySize(mContext) / 1024);
        }
    	
    	
    	if(mTargetOneFileSize < buffer_size_seq){
    		AlertDialog.Builder alt_fs_error = new AlertDialog.Builder(this);
    		String tmp = "The file size per thread \nshould be larger than\nthe sequential buffer size (" + buffer_size_seq + "KB)"; 
    		alt_fs_error.setMessage(tmp).setCancelable(false).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    				
    			
				public void onClick(DialogInterface dialog, int which) {
    				// TODO Auto-generated method stub	
    			}});
        	
        	AlertDialog alert = alt_fs_error.create();
        	alert.setTitle("Help");
        	alert.setCancelable(true);
        	alert.show();
        	return ;
    	}
    	if ( mTargetOneFileSize < (rnd_recs * buffer_size_rnd) ){
    		AlertDialog.Builder alt_fs_error = new AlertDialog.Builder(this);
    		String tmp = "The file size per thread \nshould be larger than " + (rnd_recs * buffer_size_rnd) / 1024 + "MB";
    		alt_fs_error.setMessage(tmp).setCancelable(false).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    				
    			
				public void onClick(DialogInterface dialog, int which) {
    				// TODO Auto-generated method stub	
    			}});
        	
        	AlertDialog alert = alt_fs_error.create();
        	alert.setTitle("Help");
        	alert.setCancelable(true);
        	alert.show();
        	return ;
    	}
    	if ( available_file_size < (mTargetOneFileSize * num_thread) ){
    		AlertDialog.Builder alt_fs_error = new AlertDialog.Builder(this);
    		String tmp = "The total file size \nshould be less than\nthe Free Space (";
    		if( sp_Data.getInt("TargetDevice", 0) == 0){ // /data
    			tmp += "/data";
    		}else if( sp_Data.getInt("TargetDevice", 0) == 1){ // ex) /storage/extSdCard
    			tmp += MemoryStatus.getSDExternalPath(mContext);
    		}else{
    			tmp += UNKNOWN;
    		}
    		tmp += ": " + available_file_size / 1024 + "MB)";
    		
    		alt_fs_error.setMessage(tmp).setCancelable(false).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    				
    			
				public void onClick(DialogInterface dialog, int which) {
    				// TODO Auto-generated method stub	
    			}});
        	
        	AlertDialog alert = alt_fs_error.create();
        	alert.setTitle("Help");
        	alert.setCancelable(true);
        	alert.show();
        	return ;
    	}
		sp_e_Data.remove("OneFileSize");
		sp_e_Data.putInt("OneFileSize", mTargetOneFileSize);
		
		sp_e_Data.commit();
							
	    al_Settings.remove(1);
		al_Settings.add(1, new Data("File Size", "Change read/write file size", "" + sp_Data.getInt("OneFileSize", 0) / 1024 + " MB")); 
			
		listview.setAdapter(adapter);

    }
    
    private void setTargetBufferSize(int mTargetSize_SEQ, int mTargetSize_RND){
    	int one_file_size = sp_Data.getInt("OneFileSize", 0);
    	int rnd_recs = sp_Data.getInt("TestRecs_RAND", 0);
    	
    	if( one_file_size < mTargetSize_SEQ ){
    		AlertDialog.Builder alt_bs_error = new AlertDialog.Builder(this);
    		String tmp = "The sequential buffer size \nshould be less than\nthe file size per thread (" + one_file_size / 1024 + "MB)";
    		alt_bs_error.setMessage(tmp).setCancelable(false).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    			
    			
				public void onClick(DialogInterface dialog, int which) {
    				// TODO Auto-generated method stub
    			}});
        	
        	AlertDialog alert = alt_bs_error.create();
        	alert.setTitle("Help");
        	alert.setCancelable(true);
        	alert.show();
        	return ;
    	}
    	if( one_file_size < rnd_recs * mTargetSize_RND ){
    		AlertDialog.Builder alt_bs_error = new AlertDialog.Builder(this);
    		String tmp = "The random buffer size \nshould be less than " + one_file_size / rnd_recs + "KB";
    		alt_bs_error.setMessage(tmp).setCancelable(false).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    			
    			
				public void onClick(DialogInterface dialog, int which) {
    				// TODO Auto-generated method stub
    			}});
        	
        	AlertDialog alert = alt_bs_error.create();
        	alert.setTitle("Help");
        	alert.setCancelable(true);
        	alert.show();
        	return ;
    	}
    	if( mTargetSize_SEQ > 0 && (mTargetSize_SEQ & (mTargetSize_SEQ - 1)) != 0 ){
    		AlertDialog.Builder alt_bs_error = new AlertDialog.Builder(this);
    		String tmp = "The sequential buffer size \nshould be powers of 2 (KB)";
    		alt_bs_error.setMessage(tmp).setCancelable(false).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    			
    			
				public void onClick(DialogInterface dialog, int which) {
    				// TODO Auto-generated method stub
    			}});
        	
        	AlertDialog alert = alt_bs_error.create();
        	alert.setTitle("Help");
        	alert.setCancelable(true);
        	alert.show();
        	return ;
    	}
    	if( mTargetSize_RND > 0 && (mTargetSize_RND & (mTargetSize_RND - 1)) != 0 ){
    		AlertDialog.Builder alt_bs_error = new AlertDialog.Builder(this);
    		String tmp = "The random buffer size \nshould be powers of 2 (KB)";
    		alt_bs_error.setMessage(tmp).setCancelable(false).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    			
    			
				public void onClick(DialogInterface dialog, int which) {
    				// TODO Auto-generated method stub
    			}});
        	
        	AlertDialog alert = alt_bs_error.create();
        	alert.setTitle("Help");
        	alert.setCancelable(true);
        	alert.show();
        	return ;
    	}
    	
		sp_e_Data.remove("BufferSize_SEQ");
		sp_e_Data.putInt("BufferSize_SEQ", mTargetSize_SEQ);
		
		sp_e_Data.remove("BufferSize_RND");
		sp_e_Data.putInt("BufferSize_RND", mTargetSize_RND);
		
		sp_e_Data.commit();
							
	    al_Settings.remove(2);
		al_Settings.add(2, new Data("Buffer Size", "Change buffer size for sequential & random access", "SEQ: " + sp_Data.getInt("BufferSize_SEQ", 0) + " KB" + "  RND: " + sp_Data.getInt("BufferSize_RND", 0) + " KB"));
			
		listview.setAdapter(adapter);
    }
    
    private void setTargetNumThread(int mTargetThread){
    	if( !( 1 <= mTargetThread && mTargetThread <= 32) ){
    		AlertDialog.Builder alt_fs_error = new AlertDialog.Builder(this);
    		alt_fs_error.setMessage("The number of threads \nshould be a positive number\nless than or equal to 32").setCancelable(false).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    			
    			
				public void onClick(DialogInterface dialog, int which) {
    				// TODO Auto-generated method stub
    			}});
        	
        	AlertDialog alert = alt_fs_error.create();
        	alert.setTitle("Help");
        	alert.setCancelable(true);
        	alert.show();
        	return ;
    	}
		sp_e_Data.remove("Num_Thread");
		sp_e_Data.putInt("Num_Thread", mTargetThread);
		
		sp_e_Data.commit();
							
	    al_Settings.remove(3);
		al_Settings.add(3, new Data("Threads", "Change number of threads for random read/write", "Threads:" + sp_Data.getInt("Num_Thread",0)));
			
		listview.setAdapter(adapter);
    }
    
    private void setTargetTransaction(int mTargetTransaction){
    	if( !( 1 <= mTargetTransaction) ){
    		AlertDialog.Builder alt_error = new AlertDialog.Builder(this);
    		alt_error.setMessage("The number of transactions \nshould be a positive number").setCancelable(false).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    			
    			
				public void onClick(DialogInterface dialog, int which) {
    				// TODO Auto-generated method stub
    			}});
        	
        	AlertDialog alert = alt_error.create();
        	alert.setTitle("Help");
        	alert.setCancelable(true);
        	alert.show();
        	return ;
    	}
		sp_e_Data.remove("Num_Sqlite");
		sp_e_Data.putInt("Num_Sqlite", mTargetTransaction);
		
		sp_e_Data.commit();
							
	    al_Settings.remove(4);
		al_Settings.add(4, new Data("Transactions", "Change number of transactions for SQLite", "" + sp_Data.getInt("Num_Sqlite",0)));
			
		listview.setAdapter(adapter);
    }
    
    void ItemClick(AdapterView<?> parent, View view, int position, long id){
    	if(position == 0){
    		
    		ArrayList<String> itemArray = new ArrayList<String>();
    		
    		itemArray.add("/data");
    		
    		String tmp_external_path = MemoryStatus.getSDExternalPath(mContext);
    		if(!tmp_external_path.equals(UNKNOWN)){
    			String test_path = getExternalFilesDir(null).toString();
    			// replace emulated primary external path to real sdcard path
    			test_path = test_path.replaceFirst( Environment.getExternalStorageDirectory().getAbsolutePath(),
    					tmp_external_path);
    			// check directory before test_path exists, if not generate it
    			File file = new File(test_path);
    			if(!file.exists()){
    				file.mkdirs();
    			}
    			// pre-open test files on the test_path
    			if(FILE_OPEN(test_path) > 0)
    				itemArray.add(tmp_external_path);
   
    		}
    		
    		ArrayAdapter<String> items = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, itemArray);//android.R.layout., itemArray);
    		
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setTitle("Target Partition");
    		
    		builder.setSingleChoiceItems(items, sp_Data.getInt("TargetDevice", 0), new DialogInterface.OnClickListener() {
				
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					
					setTargetDevice(which);
					dialog.dismiss();
				}
			});
    		
    		AlertDialog alert = builder.create();
    		alert.show();
    		
    		
    	}else if(position == 1){
    		Context mContext = getApplicationContext();
    		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
    		View layout = inflater.inflate(R.layout.dlg_setting_filesize,null);

    		AlertDialog.Builder aDialog = new AlertDialog.Builder(Setting.this);
    		aDialog.setView(layout);
    		aDialog.setTitle("File Size / Thread");
    		
    		tv_available_filesize_info = (TextView)layout.findViewById(R.id.et_setting_info);
    		tv_total_fs = (TextView)layout.findViewById(R.id.et_setting_totalfilesize);
    		
    		tv_one_fs	= (TextView)layout.findViewById(R.id.et_setting_onefilesize);
    		tv_one_fs.setText(Integer.toString(sp_Data.getInt("OneFileSize",0) / 1024));
    		
    		String FS_data;
    		String FS_extsdcard = "";
    	   
    		if(MemoryStatus.getAvailableInternalMemorySize() < 0){
    			FS_data = "Free Space (/data): Not available\n";
    		}else{
    			FS_data = "Free Space (/data): " + (MemoryStatus.getAvailableInternalMemorySize()/(1024*1024)) + " MB\n";
    		}
    		
    		long tmp_available_external_mem_size = MemoryStatus.getAvailableSDExternalMemorySize(mContext);
    		if(tmp_available_external_mem_size < 0){
   				FS_extsdcard = "";
       		}else{
       			FS_extsdcard = "Free Space (/storage/extSdCard): " + (tmp_available_external_mem_size/(1024*1024)) + " MB\n";
       		}
    		
    		
    		tv_total_fs.setText("Total File Size: " + sp_Data.getInt("Num_Thread",0) * (sp_Data.getInt("OneFileSize",0) / 1024)  + " MB\n");
    		tv_available_filesize_info.setText(FS_data + FS_extsdcard);
    		

    		aDialog.setPositiveButton("Apply", new DialogInterface.OnClickListener() {
    			 
				public void onClick(DialogInterface dialog, int which) {
    				// input to KB
    				// "0" + ..... for when input is null 
    				setTargetOneFileSize(Integer.parseInt("0" + tv_one_fs.getText().toString()) * 1024); 
    				    				
    			}
    		});

    		aDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
    			 
				public void onClick(DialogInterface dialog, int which) {
    			}
    		});
    		AlertDialog ad = aDialog.create();
    		    		
    		ad.show();
    	}else if(position == 2){
    		Context mContext = getApplicationContext();
    		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
    		View layout = inflater.inflate(R.layout.dlg_setting_buffersize,null);

    		AlertDialog.Builder aDialog = new AlertDialog.Builder(Setting.this);
    		aDialog.setView(layout);
    		aDialog.setTitle("Buffer Size");
    		
    		tv_seq_bs = (TextView)layout.findViewById(R.id.et_setting_seq_buffersize);
    		tv_seq_bs.setText(Integer.toString(sp_Data.getInt("BufferSize_SEQ", 0)));
    		tv_rnd_bs = (TextView)layout.findViewById(R.id.et_setting_rnd_buffersize);
    		tv_rnd_bs.setText(Integer.toString(sp_Data.getInt("BufferSize_RND", 0)));
    		

    		aDialog.setPositiveButton("Apply", new DialogInterface.OnClickListener() {
    			 
				public void onClick(DialogInterface dialog, int which) {			
    				
    				setTargetBufferSize(Integer.parseInt("0" + tv_seq_bs.getText().toString()), Integer.parseInt("0" + tv_rnd_bs.getText().toString()));
    				
    			 }
    		});

    		aDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
    			 
				public void onClick(DialogInterface dialog, int which) {
    			 }
    		});
    		AlertDialog ad = aDialog.create();
    		    		
    		ad.show();
    	}else if(position == 3){
    		Context mContext = getApplicationContext();
    		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
    		View layout = inflater.inflate(R.layout.dlg_setting_numthread,null);

    		AlertDialog.Builder aDialog = new AlertDialog.Builder(Setting.this);
    		aDialog.setView(layout);
    		aDialog.setTitle("Threads");
    		
    		tv_thread = (TextView)layout.findViewById(R.id.et_setting_thread);
    		tv_thread.setText(Integer.toString(sp_Data.getInt("Num_Thread", 0)));

    		aDialog.setPositiveButton("Apply", new DialogInterface.OnClickListener() {
    			 
				public void onClick(DialogInterface dialog, int which) {			
    				
    				setTargetNumThread(Integer.parseInt("0" + tv_thread.getText().toString()) );
    				
    			 }
    		});

    		aDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
    			 
				public void onClick(DialogInterface dialog, int which) {
    			 }
    		});
    		AlertDialog ad = aDialog.create();
    		    		
    		ad.show();
    	}else if(position == 4){
    		Context mContext = getApplicationContext();
    		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
    		View layout = inflater.inflate(R.layout.dlg_setting_transaction,null);

    		AlertDialog.Builder aDialog = new AlertDialog.Builder(Setting.this);
    		aDialog.setView(layout);
    		aDialog.setTitle("SQLite Transactions");
    		
    		tv_transaction = (TextView)layout.findViewById(R.id.et_setting_transaction);
    		tv_transaction.setText(Integer.toString(sp_Data.getInt("Num_Sqlite", 0)));
    		

    		aDialog.setPositiveButton("Apply", new DialogInterface.OnClickListener() {
    			 
				public void onClick(DialogInterface dialog, int which) {			
    				
    				setTargetTransaction(Integer.parseInt("0" + tv_transaction.getText().toString()));
    				
    			 }
    		});

    		aDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
    			 
				public void onClick(DialogInterface dialog, int which) {
    			 }
    		});
    		AlertDialog ad = aDialog.create();
    		    		
    		ad.show();
    		
    	}else if(position == 5){
    		AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
    		
    		String tmp_html = "<p>Please visit<br> <a href=\"http://www.androbench.org/wiki/User_Guide\">http://www.androbench.org/wiki/User_Guide </a> </p>";
    		alt_bld.setMessage(Html.fromHtml(tmp_html) ).setCancelable(false).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			
			
				
				public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				
				}});
    		
    		alt_bld.setTitle("Help");
    		alt_bld.setCancelable(true);
    		
    		alt_bld.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
    			
				public void onClick(DialogInterface dialog, int which) {
   			 	}
    		});
        	AlertDialog alert = alt_bld.create();
        	
        	alert.show();
        	((TextView)alert.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
        	
    	}else if(position == 6){
    		AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
        	try {
        		String tmp_html = "<p>AndroBench Version "+ getPackageManager().getPackageInfo(getPackageName(), 0).versionName + "<br>Developed by<br>Computer Systems Laboratory,<br>Sungkyunkwan University<br>"+  "<a href=\"http://www.androbench.com\">http://www.androbench.com</a>" + "<br>androbench@gmail.com</p>";
				alt_bld.setMessage(Html.fromHtml(tmp_html)).setCancelable(false).setPositiveButton("Ok", new DialogInterface.OnClickListener() {

					
					
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						
					}});
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
        	AlertDialog alert = alt_bld.create();
        	alert.setTitle("About");

        	alert.setCancelable(true);
        	alert.show();
        	((TextView)alert.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    	}
    }
   
    private class GroupAdapter extends ArrayAdapter<Object>{
    	private ArrayList<Data> item;
    	private Data temp;
    	
    	public GroupAdapter(Context ctx, int resourceID, ArrayList item){
    		super(ctx, resourceID, item);
    		this.item = item;
    	}
    	
    	@Override
		public View getView(int position, View convertView, ViewGroup parent){
    		View v = convertView;
    		
    		temp = item.get(position);
    		
    		if(v == null){
    			LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    			v = vi.inflate(R.layout.row_setting, null);
    		}
    		
    		if(temp != null){
    			TextView tv_title = (TextView)v.findViewById(R.id.row_setting_title);
    			tv_title.setText(temp.getTitle());
    			TextView tv_value = (TextView)v.findViewById(R.id.row_setting_value);
    			tv_value.setText(temp.getValue());
    			TextView tv_explain = (TextView)v.findViewById(R.id.row_setting_explain);
    			tv_explain.setText(temp.getExplain());
    		}
    		return v;
    	}
    }
}

class Data{
	String title;
	String value;
	String explain;
	Data(String title, String explain, String value){
		this.title = title;
		this.value = value;
		this.explain = explain;
	}
	public String getTitle(){
		return title;
	}
	public String getValue(){
		return value;
	}
	public String getExplain(){
		return explain;
	}
}