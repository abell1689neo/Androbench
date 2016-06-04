package com.andromeda.androbench2;

import java.util.ArrayList;

import com.andromeda.androbench2.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.Html;
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
    
    TextView tv_filesize_info;
    TextView tv_read_fs;
    TextView tv_write_fs;
    TextView tv_seq_bs;
    TextView tv_rnd_bs;
    TextView tv_transaction;
        
    static final int KBYTE = 1024;
    static final int MBYTE = 1024*1024;
	
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);
                
        sp_Data = getSharedPreferences("Settings", Activity.MODE_PRIVATE);
        sp_e_Data = sp_Data.edit();
        
        listview = (ListView)findViewById(R.id.SettingListView);
        
        al_Settings=new ArrayList<Data>();
             
        String str_TargetDevice;
        //String str_IOType;
                               
        if(sp_Data.getInt("TargetDevice", 0) == 0){
        	str_TargetDevice = "/data/";
        }else{
        	str_TargetDevice ="/sdcard/";
        }
 
        al_Settings.add(new Data("Target Partition", "Select testing partition", str_TargetDevice));
        al_Settings.add(new Data("File Size", "Change read/write file size", "RD:" + sp_Data.getInt("FileSize_RD", 0) + "  WR:" + sp_Data.getInt("FileSize_WR", 0) + " MB"));
        al_Settings.add(new Data("Buffer Size", "Change buffer size for sequential & random access", "SEQ:" + sp_Data.getInt("BufferSize_SEQ", 0) + "  RND:" + sp_Data.getInt("BufferSize_RND", 0) + " KB"));
        al_Settings.add(new Data("Number of Transactions", "Change number of transaction for SQLite benchmark", "" + sp_Data.getInt("Num_Sqlite",0)));
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
        }else if(sp_Data.getInt("TargetDevice", 0) == 1){
        	str_TargetDevice = "/sdcard";
        }else{
        	str_TargetDevice = MemoryStatus.getSDExternalPath();
        }
		
        al_Settings.remove(0);
		al_Settings.add(0, new Data("Target Partition", "Select testing partition", str_TargetDevice));
		
		listview.setAdapter(adapter);
    }
    
    private void setTargetFileSize(int mTargetSize_RD, int mTargetSize_WR){
		sp_e_Data.remove("FileSize_RD");
		sp_e_Data.putInt("FileSize_RD", mTargetSize_RD);
			
		sp_e_Data.remove("FileSize_WR");
		sp_e_Data.putInt("FileSize_WR", mTargetSize_WR);
		
		sp_e_Data.commit();
							
	    al_Settings.remove(1);
		al_Settings.add(1, new Data("File Size", "Change read & write file size", "RD:" + sp_Data.getInt("FileSize_RD", 0) + "  WR:" + sp_Data.getInt("FileSize_WR", 0) + " MB"));
			
		listview.setAdapter(adapter);

		setTargetBufferSize(256,4);
    }
    
    private void setTargetBufferSize(int mTargetSize_SEQ, int mTargetSize_RND){
		sp_e_Data.remove("BufferSize_SEQ");
		sp_e_Data.putInt("BufferSize_SEQ", mTargetSize_SEQ);
		
		sp_e_Data.remove("BufferSize_RND");
		sp_e_Data.putInt("BufferSize_RND", mTargetSize_RND);
		
		sp_e_Data.commit();
							
	    al_Settings.remove(2);
		al_Settings.add(2, new Data("Buffer Size", "Change buffer size for sequential & random access", "SEQ:" + sp_Data.getInt("BufferSize_SEQ", 0) + "  RND:" + sp_Data.getInt("BufferSize_RND", 0) + " KB"));
			
		listview.setAdapter(adapter);
    }
    
    private void setTargetTransaction(int mTargetTransaction){
		sp_e_Data.remove("Num_Sqlite");
		sp_e_Data.putInt("Num_Sqlite", mTargetTransaction);
		
		sp_e_Data.commit();
							
	    al_Settings.remove(3);
		al_Settings.add(3, new Data("Number of Transaction", "Change number of transaction for SQLite benchmark", "" + sp_Data.getInt("Num_Sqlite",0)));
			
		listview.setAdapter(adapter);
    }
    
    void ItemClick(AdapterView<?> parent, View view, int position, long id){
    	if(position == 0){
    		
    		ArrayList<String> itemArray = new ArrayList<String>();
    		
    		itemArray.add("/data");
    		itemArray.add("/sdcard");
    		if(!MemoryStatus.getSDExternalPath().equals("Unknown")){
    			itemArray.add(MemoryStatus.getSDExternalPath());  			
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
    		aDialog.setTitle("File Size");
    		
    		tv_filesize_info = (TextView)layout.findViewById(R.id.et_setting_info);
    		tv_read_fs = (TextView)layout.findViewById(R.id.et_setting_readfilesize);
    		tv_read_fs.setText(Integer.toString(sp_Data.getInt("FileSize_RD", 0)));
    		tv_write_fs = (TextView)layout.findViewById(R.id.et_setting_writefilesize);
    		tv_write_fs.setText(Integer.toString(sp_Data.getInt("FileSize_WR", 0)));
    		
    		String FS_data;
    		String FS_sdcard;
    		
    		if(MemoryStatus.getAvailableInternalMemorySize()<0){
    			FS_data = "Free Space(/data) : Not available\n";
    		}else{
    			FS_data = "Free Space(/data) : " + (MemoryStatus.getAvailableInternalMemorySize()/(1024*1024)) + " MB\n";
    		}
    		
    		if(MemoryStatus.getAvailableExternalMemorySize()<0){
    			FS_sdcard = "Free Space(/sdcard) : Not available\n";
    		}else{
    			FS_sdcard = "Free Space(/sdcard) : " + (MemoryStatus.getAvailableExternalMemorySize()/(1024*1024)) + " MB\n";
    		}
    		
    		tv_filesize_info.setText(FS_data + FS_sdcard);
    		

    		aDialog.setPositiveButton("Apply", new DialogInterface.OnClickListener() {
    			 public void onClick(DialogInterface dialog, int which) {
    				 
    				setTargetFileSize(Integer.parseInt(tv_read_fs.getText().toString()),Integer.parseInt(tv_write_fs.getText().toString()));
    				    				
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
    				
    				setTargetBufferSize(Integer.parseInt(tv_seq_bs.getText().toString()),Integer.parseInt(tv_rnd_bs.getText().toString()));
    				
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
    		View layout = inflater.inflate(R.layout.dlg_setting_transaction,null);

    		AlertDialog.Builder aDialog = new AlertDialog.Builder(Setting.this);
    		aDialog.setView(layout);
    		aDialog.setTitle("# of Transaction");
    		
    		tv_transaction = (TextView)layout.findViewById(R.id.et_setting_transaction);
    		tv_transaction.setText(Integer.toString(sp_Data.getInt("Num_Sqlite", 0)));
    		

    		aDialog.setPositiveButton("Apply", new DialogInterface.OnClickListener() {
    			 public void onClick(DialogInterface dialog, int which) {			
    				
    				setTargetTransaction(Integer.parseInt(tv_transaction.getText().toString()));
    				
    			 }
    		});

    		aDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
    			 public void onClick(DialogInterface dialog, int which) {
    			 }
    		});
    		AlertDialog ad = aDialog.create();
    		    		
    		ad.show();
    	}else if(position == 4){
    		AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
        	alt_bld.setMessage("Please visit\nhttp://www.androbench.org/wiki/User_Guide").setCancelable(false).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    			
    			
    			public void onClick(DialogInterface dialog, int which) {
    				// TODO Auto-generated method stub
    				
    			}});
        	
        	AlertDialog alert = alt_bld.create();
        	alert.setTitle("Help");
        	alert.setCancelable(true);
        	alert.show();
    	}else if(position == 5){
    		AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
        	try {
				alt_bld.setMessage("AndroBench Version "+ getPackageManager().getPackageInfo(getPackageName(), 0).versionName +"\nDeveloped by Computer Systems Laboratory, Sungkyunkwan University\nhttp://www.androbench.com\nandrobench@gmail.com").setCancelable(false).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					
					
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						
					}});
			} catch (NameNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        	
        	AlertDialog alert = alt_bld.create();
        	alert.setTitle("About");

        	alert.setCancelable(true);
        	alert.show();
    	}
    }
   
    private class GroupAdapter extends ArrayAdapter<Object>{
    	private ArrayList<Data> item;
    	private Data temp;
    	
    	public GroupAdapter(Context ctx, int resourceID, ArrayList item){
    		super(ctx, resourceID, item);
    		this.item = item;
    	}
    	
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