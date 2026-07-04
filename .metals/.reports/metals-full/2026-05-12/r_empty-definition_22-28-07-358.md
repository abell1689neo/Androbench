error id: file://<WORKSPACE>/release/3.4/src/com/andromeda/androbench2/Result.java:_empty_/MemoryStatus#getSDExternalPath#
file://<WORKSPACE>/release/3.4/src/com/andromeda/androbench2/Result.java
empty definition using pc, found symbol in pc: _empty_/MemoryStatus#getSDExternalPath#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 34435
uri: file://<WORKSPACE>/release/3.4/src/com/andromeda/androbench2/Result.java
text:
```scala
package com.andromeda.androbench2;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import com.andromeda.androbench2.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

/* 
 * Control benchmark (Result class)
 * Micro benchmark & SQLite benchmark & Macro benchmark
 */

public class Result extends Activity {
	Context mContext;
    Toast toast;
    
    // dialog for processing benchmark
    AlertDialog dlg_proc_bench;
    AlertDialog.Builder dlg_bld_proc_bench;
    
    // dialog for send result to server message
    AlertDialog dlg_snd_result;
    AlertDialog.Builder dlg_bld_snd_result;
    
    // dialog for progress bar of benchmark
    ProgressDialog dlg_bench_progress;

    // shared preference for environment data
    SharedPreferences sp_Data;
    SharedPreferences.Editor sp_e_Data;
    SharedPreferences sp_Flag;
    SharedPreferences.Editor sp_e_Flag;
    
    // environment data for benchmarks
    int test_target;
    int test_file_flag;
    int test_buffer_size_seq;
    int test_buffer_size_rnd;
    int test_file_size_read;
    int test_file_size_write;
    int test_random_seed;
    int test_num_sqlite;
	String test_macro_name;
	int test_macro_length = 0;
    String typeFileSystem;
    boolean processing;
    boolean starting;
    boolean flag_microbench = false;
    boolean flag_sqlitebench = false;
    boolean flag_macro = false;
    
    // result of send to server
    boolean send_success_micro;
    boolean send_success_sqlite;
    boolean send_success_macro;
    
    private final static int number_of_testing = 3;
    private final static int KBYTE = 1024;
    private final static int MBYTE = 1024*1024;
    private final static String TARGET_PATH = "/.androbench2";
    private final static int PROC_INIT = 0;
    private final static int PROC_SEQ_READ = 1;
    private final static int PROC_SEQ_WRITE = 2;
    private final static int PROC_RND_READ = 3;
    private final static int PROC_RND_WRITE = 4;
    private final static int PROC_MAKE_GRAPH = 5;
    private final static int PROC_INIT_PURGE = 6;
    private final static int PROC_PURGE_CACHE = 7;
    private final static int PROC_SQLITE_INSERT = 8;
    //private final static int PROC_SQLITE_SELECT = 9;
    private final static int PROC_SQLITE_UPDATE = 10;
    private final static int PROC_SQLITE_DELETE = 11;
    private final static int PROC_MACRO = 12;
    
    // benchmarks listview items
	private ArrayList<Testing_Data> alTesting;
	private ListView lvTesting;
	private TestingAdapter taTesting;
    
    int flag_process_type = 0;
    
    // Micro benchmark values
    long[] seq_read_time = new long[number_of_testing];
    long[] seq_write_time = new long[number_of_testing];
    long[] rnd_read_time = new long[number_of_testing];
    long[] rnd_write_time = new long[number_of_testing];
   
    double perf_mbps = 0;
    double perf_iops = 0;
    double total_perf_mbps = 0;
    double total_perf_iops = 0;
    
    double avg_perf_mbps_sr = 0;
    double avg_perf_mbps_sw = 0;
    double avg_perf_mbps_rr = 0;
    double avg_perf_mbps_rw = 0;
    double avg_perf_iops_rr = 0;
    double avg_perf_iops_rw = 0;

    // SQLite benchmarks values
    long sqlite_insert_time = 0;
    long sqlite_update_time = 0;
    long sqlite_delete_time = 0;
    
    double sec_sqlite_insert = 0;
    double sec_sqlite_update = 0;
    double sec_sqlite_delete = 0;
    
    double perf_sqlite_insert = 0;
    double perf_sqlite_update = 0;
    double perf_sqlite_delete = 0;
    
    // Macro benchmarks values    
    long[] macro_browser_fb_time = new long[number_of_testing];
    long[] macro_browser_eb_time = new long[number_of_testing];
    long[] macro_browser_az_time = new long[number_of_testing];
    long[] macro_market_time = new long[number_of_testing];
    long[] macro_camera_time = new long[number_of_testing];
    long[] macro_camcorder_time = new long[number_of_testing];
    
    double avg_macro_browser = 0;
    double avg_macro_market = 0;
    double avg_macro_camera = 0;
    double avg_macro_camcorder = 0;
    
  	// Thread :: Benchmark Testing
    Thread_Benchmark mBenchmark;
    
    // Thread :: Send Result to server
    Thread_SND_Result mSendResult;
    
	// DTATBASE Interface for SQLite Testing
	private SQLiteDatabase dbTesting;
	private Testing_SQLite_DBHelper testSQLite;
		
	// DATABASE Interface
	private HistoryDB historyDBHelper;
	private DeviceDB deviceDBHelper;
	
	static{
		System.loadLibrary("Interface_JNI");
	}
	
	int cntTesting;
	
	// JNI Interfaces (for micro & macro benchmarks)	
	public native int INIT_READ(int target, String path, int file_flag, int reclen, int filesize);
	public native int INIT_PURGE_CACHE(int totalMem);
	public native void PURGE_CACHE(int totalMem);
	public native int FINAL(int target, String path);
	public native long SEQ_READ(int target, String path, int file_flag, int reclen, int filesize);
	public native long SEQ_WRITE(int target, String path, int file_flag, int reclen, int filesize);
	public native long RND_READ(int target, String path, int file_flag, int reclen, int filesize, int rndSeed);
	public native long RND_WRITE(int target, String path, int file_flag, int reclen, int filesize, int rndSeed);
	public native long MACRO(int target, String path, String script_file, int numScripts, int script_line);
	
	
	protected void onRestoreInstanceState(Bundle savedState){
		super.onRestoreInstanceState(savedState);
		processing = savedState.getBoolean("processing");
	}
	
	
	protected void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		outState.putBoolean("processing", processing);
	}
    
	/** Called when the activity is first created. */
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.result);
               
        mContext = getApplicationContext();

        // benchmark progress dialog
        dlg_bench_progress = new ProgressDialog(this);
        
        // DB Helper for saving history & device_rnd number
        historyDBHelper = new HistoryDB(this, null, null);
        deviceDBHelper = new DeviceDB(this, null, null);
        
        sp_Data = getSharedPreferences("Settings", Activity.MODE_PRIVATE);
        sp_Flag = getSharedPreferences("Flags", Activity.MODE_PRIVATE);
		sp_e_Flag = sp_Flag.edit();
		
		// create benchmark listview item
        lvTesting = (ListView)findViewById(R.id.TestingView);
        lvTesting.setOnItemClickListener(new OnItemClickListener(){
        	
        	public void onItemClick(AdapterView<?> parent, View view, int position, long id){
        		ItemClick(parent, view, position, id);
        	}
        });	
        alTesting = new ArrayList<Testing_Data>();
        alTesting.clear();
        alTesting.add(new Testing_Data("Sequential Read", "n/a"));
        alTesting.add(new Testing_Data("Sequential Write", "n/a"));
        alTesting.add(new Testing_Data("Random Read", "n/a"));
        alTesting.add(new Testing_Data("Random Write", "n/a"));
        alTesting.add(new Testing_Data("SQLite Insert", "n/a"));
        alTesting.add(new Testing_Data("SQLite Update", "n/a"));
        alTesting.add(new Testing_Data("SQLite Delete", "n/a"));
        alTesting.add(new Testing_Data("Browser", "n/a"));
        alTesting.add(new Testing_Data("Market", "n/a"));
        alTesting.add(new Testing_Data("Camera", "n/a"));
        alTesting.add(new Testing_Data("Camcorder", "n/a"));
       	taTesting = new TestingAdapter(this, R.layout.row_testing, alTesting);
        lvTesting.setAdapter(taTesting);
    }
    
    
    public void onDestroy(){
    	super.onDestroy();
    	historyDBHelper.close();
    	deviceDBHelper.close();
    }
    
    
    protected void onResume(){
    	super.onResume();
   	   	
    	// Question dialog for processing benchmark
    	dlg_bld_proc_bench = new AlertDialog.Builder(this);
    	dlg_bld_proc_bench.setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dlg_bench_progress, int which) {
				// TODO Auto-generated method stub
				if(flag_process_type == 0){
		            startingDialog();
	    			doBenchmarking();
				}else{
					sp_e_Flag.remove("StartingBench");
					sp_e_Flag.putBoolean("StartingBench", false);
					sp_e_Flag.commit();
					Intent intent = new Intent();
					intent.setAction("com.androbench.CHANGE_TAB");
					intent.putExtra("CHANGE_TARGET", 3);
					sendBroadcast(intent);
				}
			}}).setNegativeButton("No", new DialogInterface.OnClickListener() {
				
			
			public void onClick(DialogInterface dlg_bench_progress, int which) {
				// TODO Auto-generated method stub
				clearResult();
			}});
    	
    	// Question dialog for sending result of benchmarks
    	dlg_bld_snd_result = new AlertDialog.Builder(this);
    	dlg_bld_snd_result.setCancelable(false)
    		.setTitle("Send Results")
    		.setMessage("Benchmark results will be sent to the server for research purpose. No personally identifiable information will not be transmitted.\n\nDo you want to continue?")
    		.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
			
			
			public void onClick(DialogInterface dlg_bench_progress, int which) {
				// TODO Auto-generated method stub
		    	mSendResult = new Thread_SND_Result();
		    	mSendResult.start();
			}}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				
			
			public void onClick(DialogInterface dlg_bench_progress, int which) {
				// TODO Auto-generated method stub
				// Nothing to do
			}});
    	
    	
    	// Initializing benchmarks flags
    	starting = sp_Flag.getBoolean("StartingBench", false);
    	flag_microbench = sp_Flag.getBoolean("MicroBench", false);
    	flag_sqlitebench = sp_Flag.getBoolean("SqliteBench", false);
    	flag_macro = sp_Flag.getBoolean("MacroBench", false);
    	if((!processing)&&(starting)){
    		initBenchENV();
            startingDialog();
			doBenchmarking();
    	}
    }
       
    // Ranking service for each benchmarks with listview
    void ItemClick(AdapterView<?> parent, View view, int position, long id){
    	Intent itRank = new Intent(this, Ranking.class);

    	if(position == 0){
    		itRank.putExtra("type", "v_avg_sr");
    		if(avg_perf_mbps_sr != 0){
    			itRank.putExtra("flag_test", true);
    			itRank.putExtra("result", avg_perf_mbps_sr);
    		}else{
    			itRank.putExtra("flag_test", false);
    		}
    		startActivity(itRank); 
    	}else if(position == 1){
    		itRank.putExtra("type", "v_avg_sw");
    		if(avg_perf_mbps_sw != 0){
    			itRank.putExtra("flag_test", true);
    			itRank.putExtra("result", avg_perf_mbps_sw);
    		}else{
    			itRank.putExtra("flag_test", false);
    		}
    		startActivity(itRank);  
    	}else if(position == 2){
    		itRank.putExtra("type", "v_avg_rr");
    		if(avg_perf_iops_rr != 0){
    			itRank.putExtra("flag_test", true);
    			itRank.putExtra("result", avg_perf_iops_rr);
    		}else{
    			itRank.putExtra("flag_test", false);
    		}
    		startActivity(itRank);  
    	}else if(position == 3){
    		itRank.putExtra("type", "v_avg_rw");
    		if(avg_perf_iops_rw != 0){
    			itRank.putExtra("flag_test", true);
    			itRank.putExtra("result", avg_perf_iops_rw);
    		}else{
    			itRank.putExtra("flag_test", false);
    		}
    		startActivity(itRank);  
    	}else if(position == 4){
    		itRank.putExtra("type", "v_perf_insert");
    		if(perf_sqlite_insert != 0){
    			itRank.putExtra("flag_test", true);
    			itRank.putExtra("result", perf_sqlite_insert);
    		}else{
    			itRank.putExtra("flag_test", false);
    		}
    		startActivity(itRank);
    	}else if(position == 5){
    		itRank.putExtra("type", "v_perf_update");
    		if(perf_sqlite_update != 0){
    			itRank.putExtra("flag_test", true);
    			itRank.putExtra("result", perf_sqlite_update);
    		}else{
    			itRank.putExtra("flag_test", false);
    		}
    		startActivity(itRank); 
    	}else if(position == 6){
    		itRank.putExtra("type", "v_perf_delete");
    		if(perf_sqlite_delete != 0){
    			itRank.putExtra("flag_test", true);
    			itRank.putExtra("result", perf_sqlite_delete);
    		}else{
    			itRank.putExtra("flag_test", false);
    		}
    		startActivity(itRank); 
    	}else if(position == 7){
    		itRank.putExtra("type", "v_avg_browser");
    		if(avg_macro_browser != 0){
    			itRank.putExtra("flag_test", true);
    			itRank.putExtra("result", avg_macro_browser);
    		}else{
    			itRank.putExtra("flag_test", false);
    		}
    		startActivity(itRank); 
    	}else if(position == 8){
    		itRank.putExtra("type", "v_avg_market");
    		if(avg_macro_market != 0){
    			itRank.putExtra("flag_test", true);
    			itRank.putExtra("result", avg_macro_market);
    		}else{
    			itRank.putExtra("flag_test", false);
    		}
    		startActivity(itRank); 
    	}else if(position == 9){
    		itRank.putExtra("type", "v_avg_camera");
    		if(avg_macro_camera != 0){
    			itRank.putExtra("flag_test", true);
    			itRank.putExtra("result", avg_macro_camera);
    		}else{
    			itRank.putExtra("flag_test", false);
    		}
    		startActivity(itRank); 
    	}else if(position == 10){
    		itRank.putExtra("type", "v_avg_camcorder");
    		if(avg_macro_camcorder != 0){
    			itRank.putExtra("flag_test", true);
    			itRank.putExtra("result", avg_macro_camcorder);
    		}else{
    			itRank.putExtra("flag_test", false);
    		}
    		startActivity(itRank); 
    	}
    }
    
    public static class ViewHolder{
    	TextView name;
    	TextView status;
    }
    
    private class TestingAdapter extends BaseAdapter{
    	ArrayList<Testing_Data>list;
    	Context ctx;
    	int itemLayout;
    	
		class ViewHolder{
			TextView name;
			TextView status;
		}
    	
    	TestingAdapter(Context ctx, int itemLayout, ArrayList<Testing_Data> list){
    		this.ctx = ctx;
    		this.itemLayout = itemLayout;
    		this.list = list;
    	}
    	
		
		public int getCount() {
			// TODO Auto-generated method stub
			return list.size();
		}

		
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return list.get(position);
		}

		
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
					
			ViewHolder vh = new ViewHolder();
			
			if(convertView == null){
				LayoutInflater inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(itemLayout, parent, false);
				
				vh.name = (TextView)convertView.findViewById(R.id.row_testing_name);
				vh.status = (TextView)convertView.findViewById(R.id.row_testing_status);
				
				convertView.setTag(vh);
			}else{
				vh = (ViewHolder) convertView.getTag();
			}
			
			vh.name.setText(list.get(position).getName());
			vh.status.setText(list.get(position).getStatus());
			
			return convertView;
		}    	
    }
    
    private void initBenchENV(){
    	// Read Benchmarking Environment from Shared Data    	
    	test_target = sp_Data.getInt("TargetDevice", 0);
        test_buffer_size_seq = sp_Data.getInt("BufferSize_SEQ", 0) * KBYTE;
        test_buffer_size_rnd = sp_Data.getInt("BufferSize_RND", 0) * KBYTE;
        test_file_size_read = sp_Data.getInt("FileSize_RD", 0) * MBYTE;
        test_file_size_write = sp_Data.getInt("FileSize_WR", 0) * MBYTE;
        test_num_sqlite = sp_Data.getInt("Num_Sqlite", 0);
    	
    	// Setting IO Type Option automatically!
		if(test_target == 0){
			typeFileSystem = MemoryStatus.getInternalFilesystem();
		}else if(test_target == 1){
			typeFileSystem = MemoryStatus.getExternalFilesystem();
		}else{
			typeFileSystem = MemoryStatus.getSDExternalFilesystem();
		}
		
		// Check YAFFS, YAFFS2, EXT2 for purge_cache routine
		if(typeFileSystem.equals("yaffs") || typeFileSystem.equals("yaffs2") || typeFileSystem.equals("ext2")){
			test_file_flag = 0;
		}else{
			test_file_flag = 1;
		}
    }
    
    // Create benchmarking thread
    private void doBenchmarking(){
    	mBenchmark = new Thread_Benchmark();
    	mBenchmark.start();
    }
    
    // Initializing benchmarks listview
    private void initResult(){
    	alTesting.clear();
    	alTesting.add(new Testing_Data("Sequential Read", "n/a"));
    	alTesting.add(new Testing_Data("Sequential Write", "n/a"));
    	alTesting.add(new Testing_Data("Random Read", "n/a"));
    	alTesting.add(new Testing_Data("Random Write", "n/a"));
       	alTesting.add(new Testing_Data("SQLite Insert", "n/a"));
       	alTesting.add(new Testing_Data("SQLite Update", "n/a"));
       	alTesting.add(new Testing_Data("SQLite Delete", "n/a"));
        alTesting.add(new Testing_Data("Browser", "n/a"));
        alTesting.add(new Testing_Data("Market", "n/a"));
        alTesting.add(new Testing_Data("Camera", "n/a"));
        alTesting.add(new Testing_Data("Camcorder", "n/a"));
    	lvTesting.setAdapter(taTesting);
    }
       
    // Printing result of benchmarks listview
    private void printResult(){
    	
    	alTesting.clear();
    	
    	if(flag_microbench){
    		alTesting.add(new Testing_Data("Sequential Read", avg_perf_mbps_sr + " MB/s"));
	    	alTesting.add(new Testing_Data("Sequential Write", avg_perf_mbps_sw + " MB/s"));
	    	alTesting.add(new Testing_Data("Random Read", avg_perf_mbps_rr + " MB/s, " + avg_perf_iops_rr + " IOPS(" + (test_buffer_size_rnd / 1024) + "K)"));
	    	alTesting.add(new Testing_Data("Random Write", avg_perf_mbps_rw + " MB/s, " + avg_perf_iops_rw + " IOPS(" + (test_buffer_size_rnd / 1024) + "K)"));
    	}else{
        	alTesting.add(new Testing_Data("Sequential Read", "n/a"));
        	alTesting.add(new Testing_Data("Sequential Write", "n/a"));
        	alTesting.add(new Testing_Data("Random Read", "n/a"));
        	alTesting.add(new Testing_Data("Random Write", "n/a"));
    	}
    	
    	if(flag_sqlitebench){
        	alTesting.add(new Testing_Data("SQLite Insert", perf_sqlite_insert + " TPS, " + sec_sqlite_insert + " sec"));
        	alTesting.add(new Testing_Data("SQLite Update", perf_sqlite_update + " TPS, " + sec_sqlite_update + " sec"));
        	alTesting.add(new Testing_Data("SQLite Delete", perf_sqlite_delete + " TPS, " + sec_sqlite_delete + " sec"));
    	}else{
           	alTesting.add(new Testing_Data("SQLite Insert", "n/a"));
           	alTesting.add(new Testing_Data("SQLite Update", "n/a"));
           	alTesting.add(new Testing_Data("SQLite Delete", "n/a"));
    	}
    	
    	if(flag_macro){
            alTesting.add(new Testing_Data("Browser", avg_macro_browser + " msec"));
            alTesting.add(new Testing_Data("Market", avg_macro_market + " msec"));
            alTesting.add(new Testing_Data("Camera", avg_macro_camera + " msec"));
            alTesting.add(new Testing_Data("Camcorder", avg_macro_camcorder + " msec"));
    	}else{
            alTesting.add(new Testing_Data("Browser", "n/a"));
            alTesting.add(new Testing_Data("Market", "n/a"));
            alTesting.add(new Testing_Data("Camera", "n/a"));
            alTesting.add(new Testing_Data("Camcorder", "n/a"));
    	}
    	lvTesting.setAdapter(taTesting);
    }
    
   
    public void startingDialog(){
    	dlg_bench_progress.setCancelable(false);
        dlg_bench_progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dlg_bench_progress.setMessage(" ");
        dlg_bench_progress.setProgress(0);
        dlg_bench_progress.setMax(100);
        dlg_bench_progress.show();
    }
       
    public void changeDialog(final int title, final int percent){
		runOnUiThread(new Runnable(){
			
			public void run(){	
				if(cntTesting==3){
					cntTesting--;
				}
				switch(title){
				case PROC_INIT:
					dlg_bench_progress.setMessage("Initializing for Read");
					dlg_bench_progress.setMax(test_file_size_read);
					dlg_bench_progress.setProgress(percent);
					break;
				case PROC_INIT_PURGE:
					dlg_bench_progress.setMessage("Initializing for Purge Cache");
					dlg_bench_progress.setMax((((MemoryStatus.getTotalMem()/KBYTE)/2) + 50)*MBYTE);
					dlg_bench_progress.setProgress(percent);
					break;
				case PROC_PURGE_CACHE:
					dlg_bench_progress.setMessage("Purge Buffer Cache");
					dlg_bench_progress.setMax((((MemoryStatus.getTotalMem()/KBYTE)/2) + 50)*MBYTE);
					dlg_bench_progress.setProgress(percent);
					break;
				case PROC_SEQ_READ:
					dlg_bench_progress.setMessage("Sequential Reading (" + (cntTesting+1) + "/" + number_of_testing + ")");
					dlg_bench_progress.setMax(test_file_size_read);
					dlg_bench_progress.setProgress(percent);
					break;
				case PROC_SEQ_WRITE:
					dlg_bench_progress.setMessage("Sequential Writing (" + (cntTesting+1) + "/" + number_of_testing + ")");
					dlg_bench_progress.setMax(test_file_size_write);
					dlg_bench_progress.setProgress(percent);
					break;
				case PROC_RND_READ:
					dlg_bench_progress.setMessage("Random Reading (" + (cntTesting+1) + "/" + number_of_testing + ")");
					dlg_bench_progress.setMax((test_file_size_read));
					dlg_bench_progress.setProgress(percent);
					break;
				case PROC_RND_WRITE:
					dlg_bench_progress.setMessage("Random Writing (" + (cntTesting+1) + "/" + number_of_testing + ")");
					dlg_bench_progress.setMax(test_file_size_write);
					dlg_bench_progress.setProgress(percent);
					break;
				case PROC_SQLITE_INSERT:
					dlg_bench_progress.setMessage("SQLite Insert");
					dlg_bench_progress.setMax(test_num_sqlite);
					dlg_bench_progress.setProgress(percent);
					break;
				case PROC_SQLITE_UPDATE:
					dlg_bench_progress.setMessage("SQLite Update");
					dlg_bench_progress.setMax(test_num_sqlite);
					dlg_bench_progress.setProgress(percent);
					break;
				case PROC_SQLITE_DELETE:
					dlg_bench_progress.setMessage("SQLite Delete");
					dlg_bench_progress.setMax(test_num_sqlite);
					dlg_bench_progress.setProgress(percent);
					break;
				case PROC_MACRO:
					dlg_bench_progress.setMessage("Macro: " + test_macro_name + " (" + (cntTesting+1) + "/" + number_of_testing + ")");
					dlg_bench_progress.setMax(test_macro_length);
					dlg_bench_progress.setProgress(percent);
					break;
				}
			}
		});
    }
    
    public void clearResult(){
		processing = false;
		sp_e_Flag.remove("StartingBench");
		sp_e_Flag.putBoolean("StartingBench", false);
		sp_e_Flag.commit();
    }
    
    // Copy readme.txt to created directory from ASSET
    public void copyReadme(AssetManager am){
    	File f = new File("/sdcard/.androbench/readme.txt");
    	FileOutputStream fos = null;
    	BufferedOutputStream bos = null;
    	try{
    		InputStream is = am.open("readme.txt");
    		BufferedInputStream bis = new BufferedInputStream(is);
    		if(f.exists()){
    			f.delete();
    			f.createNewFile();
    		}
    		fos = new FileOutputStream(f);
    		bos = new BufferedOutputStream(fos);
    		int read = -1;
    		byte[] buffer = new byte[1024];
    		while((read = bis.read(buffer, 0, 1024)) != -1){
    			bos.write(buffer, 0, read);
    		}
    		bos.flush();
    		fos.close();
    		bos.close();
    		is.close();
    		bis.close();    		
    	}catch(IOException e){
    		e.printStackTrace();
    	}
    }
    
    // Copy script to files directory from ASSET (for macro benchmarks)
    public int copyScript(AssetManager am, String scriptName){
    	File f = new File("/data/data/com.andromeda.androbench2/files/"+ scriptName);
    	FileOutputStream fos = null;
    	BufferedOutputStream bos = null;
    	try{
    		InputStream is = am.open(scriptName);
    		BufferedInputStream bis = new BufferedInputStream(is);
    		if(f.exists()){
    			f.delete();
    			f.createNewFile();
    		}
    		fos = new FileOutputStream(f);
    		bos = new BufferedOutputStream(fos);
    		int read = -1;
    		byte[] buffer = new byte[1024];
    		while((read = bis.read(buffer, 0, 1024)) != -1){
    			bos.write(buffer, 0, read);
    		}
    		bos.flush();
    		fos.close();
    		bos.close();
    		is.close();
    		bis.close();    		
    	}catch(IOException e){
    		e.printStackTrace();
    	}
    	
    	BufferedReader bufferedReader = null;
    	  
    	int count = 0;
    	try{
    		 bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
    		try{
				while (bufferedReader.readLine() != null) count++;
			}catch (IOException e) {
				e.printStackTrace();
				return 0;
			}
    		return count;
    	}catch(FileNotFoundException e){
    		e.printStackTrace();
    		return 0;
    	}
    }
    
    void deleteDir(int target, String path){
    	
    	if(target == 0){
    		path = "/data/data/com.andromeda.androbench2/files/script";
    	}else if(target == 1){
    		path = "/mnt/sdcard/.androbench2/script";
    	}
    	
    	File file = new File(path);
    	File[] childFileList = file.listFiles();
    	for(File childFile : childFileList){
    		childFile.delete();
    	}
    	file.delete();
    }
    
    // ERROR_CHECK for micro & macro benchmarks (JNI system call)
    void ERROR_CHECK(long ERROR_CODE){    	
    	if(ERROR_CODE == -1){
    		Log.d("AndroBench_Error", "ERROR_NOT_OPEN");
    		toast.setText("ERROR_NOT_OPEN");
    		toast.show();
    	}else if(ERROR_CODE == -2){
    		Log.d("AndroBench_Error", "ERROR_NOT_READ");
    		toast.setText("ERROR_NOT_READ");
    		toast.show();
    	}else if(ERROR_CODE == -3){
    		Log.d("AndroBench_Error", "ERROR_NOT_WRITE");
    		toast.setText("ERROR_NOT_WRITE");
    		toast.show();
    	}else if(ERROR_CODE == -4){
    		Log.d("AndroBench_Error", "ERROR_NOT_ALLOCATION");
    		toast.setText("ERROR_NOT_ALLOCATION");
    		toast.show();
    	}else if(ERROR_CODE == -5){
    		Log.d("AndroBench_Error", "ERROR_NOT_CLOSE");
    		toast.setText("ERROR_NOT_CLOSE");
    		toast.show();
    	}else if(ERROR_CODE == -6){
    		Log.d("AndroBench_Error", "ERROR_NOT_CREATE_RNDNUM");
    		toast.setText("ERROR_NOT_CREATE_RNDNUM");
    		toast.show();
    	}
    }
    
    
    // Benchmarks thread (Doing micro, SQLite and macro benchmarks)
	//실제 benchmark 실행하는 thread 생산
    public class Thread_Benchmark extends Thread{
    	
    	public void preExcute(){//준비 (sqlite table empty)
    		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    		
    		processing = true;
    		
    		total_perf_mbps = 0;
    		total_perf_iops = 0;
    	    avg_perf_mbps_sr = 0;
    	    avg_perf_mbps_sw = 0;
    	    avg_perf_mbps_rr = 0;
    	    avg_perf_mbps_rw = 0;
    	    avg_perf_iops_rr = 0;
    	    avg_perf_iops_rw = 0;
    	    sec_sqlite_insert = 0;
    	    sec_sqlite_update = 0;
    	    sec_sqlite_delete = 0;
    	    perf_sqlite_insert = 0;
    	    perf_sqlite_update = 0;
    	    perf_sqlite_delete = 0;
    	    avg_macro_browser = 0;
    	    avg_macro_market = 0;
    	    avg_macro_camera = 0;
    	    avg_macro_camcorder = 0;
    	    
            runOnUiThread(new Runnable(){
    			
    			public void run(){
    	    	    initResult();   				
    			}
            });

    		// Get access permission for "/data/data/package_name/files"
    		mContext.getFilesDir();
    		
    		// Initialize for SQLite Testing
    		testSQLite = new Testing_SQLite_DBHelper(mContext, 1);
    		dbTesting = testSQLite.getWritableDatabase();
            dbTesting.delete("testing", null, null);
            testSQLite.close();
            
    	}
    	
    	public void postExcute(){//after running 
    		String curDate;
    		String curTarget;
    		String useBuffer;
    		
            dlg_bench_progress.dismiss();
            
        	runOnUiThread(new Runnable(){
    			
    			public void run(){
		            dlg_snd_result = dlg_bld_snd_result.create();
		            dlg_snd_result.show();
    			}});
        	
        	runOnUiThread(new Runnable(){
    			
    			public void run(){
    				printResult();
    			}});
    		
    		GregorianCalendar cal = new GregorianCalendar();
    		
    		curDate = String.format("%04d-%02d-%02d %02d:%02d:%02d", cal.get(Calendar.YEAR), (cal.get(Calendar.MONTH) + 1), cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
    		
    		if(test_target == 0){
    			curTarget = "/data (" + MemoryStatus.getInternalFilesystem() + ")";
    		}else if(test_target == 1){
    			curTarget = "/sdcard (" + MemoryStatus.getExternalFilesystem() + ")";
    		}else if(test_target == 2){
    			curTarget = MemoryStatus.getSDExternalPath() + " (" + MemoryStatus.getSDExternalFilesystem() + ")";
    		}else{
    			curTarget = "Unknown";
    		}
    		
    		if(test_file_flag == 0){
    			useBuffer = "Yes(Purge)";
    		}else{
    			useBuffer = "No";
    		}
    		
    		SQLiteDatabase db = historyDBHelper.getWritableDatabase();
    		ContentValues row;
    		row = new ContentValues();
    		row.put("date", curDate);
    		row.put("target", curTarget);
    		row.put("filesize_read", (test_file_size_read / MBYTE));
    		row.put("filesize_write", (test_file_size_write / MBYTE));
    		row.put("buffersize_seq", (test_buffer_size_seq / KBYTE));
    		row.put("buffersize_rnd", (test_buffer_size_rnd / KBYTE));
    		row.put("use_buffer", useBuffer);
    		row.put("avg_mbps_sr", avg_perf_mbps_sr);
    		row.put("avg_mbps_sw", avg_perf_mbps_sw);
    		row.put("avg_mbps_rr", avg_perf_mbps_rr);
    		row.put("avg_iops_rr", avg_perf_iops_rr);
    		row.put("avg_mbps_rw", avg_perf_mbps_rw);
    		row.put("avg_iops_rw", avg_perf_iops_rw);
    		row.put("perf_sqlite_insert", perf_sqlite_insert);
    		row.put("perf_sqlite_update", perf_sqlite_update);
    		row.put("perf_sqlite_delete", perf_sqlite_delete);
    		row.put("macro_browser_time", avg_macro_browser);
    		row.put("macro_market_time", avg_macro_market);
    		row.put("macro_camera_time", avg_macro_camera);
    		row.put("macro_camcorder_time", avg_macro_camcorder);
    		db.insert("history", null, row);
            
    		processing = false;
    		
    		sp_e_Flag.remove("StartingBench");
			sp_e_Flag.putBoolean("StartingBench", false);
			sp_e_Flag.commit();
			
    		testSQLite = new Testing_SQLite_DBHelper(mContext, 1);
    		dbTesting = testSQLite.getWritableDatabase();
            dbTesting.delete("testing", null, null);
            testSQLite.close();
			
            FINAL(test_target, MemoryStatus.getSDExternalPath() + TARGET_PATH);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    	}
    	   	
    	public void run(){//본체
    		preExcute();//준비

			//1.microbench: seq r/w + rand r/w
    		if(flag_microbench){ 					
				changeDialog(PROC_INIT, 0);
				
	   			/*******************************************************************************************
	   			 * Micro benchmarks :: Initializing for sequential & random read benchmarks 
	   			 *******************************************************************************************/
				//테스트 파일 생성 -> read의 읽을 대상
	    		INIT_READ(test_target, MemoryStatus.getSDExternalPath() + TARGET_PATH, test_file_flag , MBYTE, test_file_size_read);
	    		
	   			/*******************************************************************************************
	   			 * Micro benchmarks :: Purge_cache for YAFFS, YAFFS2, Ext2 
	   			 *******************************************************************************************/
	   			if(test_file_flag == 0){
	   				INIT_PURGE_CACHE((((MemoryStatus.getTotalMem()/KBYTE)/2) + 50)*MBYTE);
	   				// Copy readme.txt file in temporary directory   				
	   				copyReadme(mContext.getAssets());
				}
	    		
	   			/*******************************************************************************************
	   			 * Micro benchmarks :: Sequential read benchmarks 
	   			 *******************************************************************************************/
				total_perf_mbps = 0;
	    		total_perf_iops = 0;
	    		    		
	    		for(cntTesting=0; cntTesting<number_of_testing; cntTesting++){   			
	    			if(test_file_flag == 0){
	    				PURGE_CACHE((((MemoryStatus.getTotalMem()/KBYTE)/2) + 50)*MBYTE);//캐시 비우기
	    			}
	    			
	    			seq_read_time[cntTesting] = SEQ_READ(test_target, MemoryStatus.getSDExter@@nalPath() + TARGET_PATH, test_file_flag, test_buffer_size_seq, test_file_size_read);
	            	
	            	if(seq_read_time[cntTesting] == -1){
	            		toast.setText("ERROR_NOT_OPEN");
	            		toast.show();
	            		break;
	            	}else if(seq_read_time[cntTesting] == -2){
	            		toast.setText("ERROR_NOT_READ");
	            		toast.show();
	            		break;
	            	}else if(seq_read_time[cntTesting] == -3){
	            		toast.setText("ERROR_NOT_WRITE");
	            		toast.show();
	            		break;
	            	}else if(seq_read_time[cntTesting] == -4){
	            		toast.setText("ERROR_NOT_ALLOCATION");
	            		toast.show();
	            		break;
	            	}else{
	           			perf_mbps = (double)(test_file_size_read / (1024*1024)) / (seq_read_time[cntTesting] / (double)1000000);
	           			total_perf_mbps += perf_mbps;
	           			avg_perf_mbps_sr = (int)(total_perf_mbps/(double)(cntTesting+1)*100)/(double)100;
	           			
	            	}
	            }   		
	    		
	   			/*******************************************************************************************
	   			 * Micro benchmarks :: Sequential write benchmarks 
	   			 *******************************************************************************************/	            
				total_perf_mbps = 0;
	    		total_perf_iops = 0;
	            
	            for(cntTesting=0; cntTesting<number_of_testing; cntTesting++){
	    			
	            	seq_write_time[cntTesting] = SEQ_WRITE(test_target, MemoryStatus.getSDExternalPath() + TARGET_PATH, test_file_flag, test_buffer_size_seq, test_file_size_write);
	            	
	            	if(seq_write_time[cntTesting] == -1){
	            		toast.setText("ERROR_NOT_OPEN");
	            		toast.show();
	            		break;
	            	}else if(seq_write_time[cntTesting] == -2){
	            		toast.setText("ERROR_NOT_READ");
	            		toast.show();
	            		break;
	            	}else if(seq_write_time[cntTesting] == -3){
	            		toast.setText("ERROR_NOT_WRITE");
	            		toast.show();
	            		break;
	            	}else if(seq_write_time[cntTesting] == -4){
	            		toast.setText("ERROR_NOT_ALLOCATION");
	            		toast.show();
	            		break;
	            	}else{
	        			perf_mbps = (double)(test_file_size_write / (1024*1024)) / (seq_write_time[cntTesting] / (double)1000000);
	        			total_perf_mbps += perf_mbps;
	        			avg_perf_mbps_sw = (int)(total_perf_mbps/(double)(cntTesting+1)*100)/(double)100;
	            	}
	            }
	            
	   			/*******************************************************************************************
	   			 * Micro benchmarks :: Random read benchmarks 
	   			 *******************************************************************************************/	           
				total_perf_mbps = 0;
	    		total_perf_iops = 0;
	    		
	    		// Create seed number for rand function(in JNI) 
	    		test_random_seed = (int)(10000.0 * Math.random());
	            
	            for(cntTesting=0; cntTesting<number_of_testing; cntTesting++){            	
	    			if(test_file_flag == 0){
	    				PURGE_CACHE((((MemoryStatus.getTotalMem()/KBYTE)/2) + 50)*MBYTE);
	    			}
	    			      	
	            	rnd_read_time[cntTesting] = RND_READ(test_target, MemoryStatus.getSDExternalPath() + TARGET_PATH, test_file_flag, test_buffer_size_rnd, test_file_size_read, test_random_seed);
	            	
	            	if(rnd_read_time[cntTesting] == -1){
	            		toast.setText("ERROR_NOT_OPEN");
	            		toast.show();
	            		break;
	            	}else if(rnd_read_time[cntTesting] == -2){
	            		toast.setText("ERROR_NOT_READ");
	            		toast.show();
	            		break;
	            	}else if(rnd_read_time[cntTesting] == -3){
	            		toast.setText("ERROR_NOT_WRITE");
	            		toast.show();
	            		break;
	            	}else if(rnd_read_time[cntTesting] == -4){
	            		toast.setText("ERROR_NOT_ALLOCATION");
	            		toast.show();
	            		break;
	            	}else{
	                	perf_mbps = (double)(test_file_size_read / (1024*1024)) / (rnd_read_time[cntTesting] / (double)1000000);
	    	    		total_perf_mbps += perf_mbps;
	    	    		avg_perf_mbps_rr = (int)(total_perf_mbps/(double)(cntTesting+1)*100)/(double)100;
	    	    		
	    	    		perf_iops = (double)(test_file_size_read / test_buffer_size_rnd) / (rnd_read_time[cntTesting] / (double)1000000);
	    	    		total_perf_iops += perf_iops;
	    	    		avg_perf_iops_rr = (int)(total_perf_iops/(double)(cntTesting+1)*100)/(double)100;
	            	}
	            }
	            
	   			/*******************************************************************************************
	   			 * Micro benchmarks :: Random write benchmarks 
	   			 *******************************************************************************************/	
				total_perf_mbps = 0;
	    		total_perf_iops = 0;
	    		
	    		// Create seed number for rand function(in JNI)
	    		test_random_seed = (int)(10000.0 * Math.random());
	            
	            for(cntTesting=0; cntTesting<number_of_testing; cntTesting++){    			         	
	            	rnd_write_time[cntTesting] = RND_WRITE(test_target, MemoryStatus.getSDExternalPath() + TARGET_PATH, test_file_flag, test_buffer_size_rnd, test_file_size_write, test_random_seed);
	            	
	            	if(rnd_write_time[cntTesting]  == -1){
	            		toast.setText("ERROR_NOT_OPEN");
	            		toast.show();
	            		break;
	            	}else if(rnd_write_time[cntTesting]  == -2){
	            		toast.setText("ERROR_NOT_READ");
	            		toast.show();
	            		break;
	            	}else if(rnd_write_time[cntTesting]  == -3){
	            		toast.setText("ERROR_NOT_WRITE");
	            		toast.show();
	            		break;
	            	}else if(rnd_write_time[cntTesting]  == -4){
	            		toast.setText("ERROR_NOT_ALLOCATION");
	            		toast.show();
	            		break;
	            	}else{
	            		perf_mbps = (double)(test_file_size_write / (1024*1024)) / (rnd_write_time[cntTesting] / (double)1000000);
		    			total_perf_mbps += perf_mbps;
		    			avg_perf_mbps_rw = (int)(total_perf_mbps/(double)(cntTesting+1)*100)/(double)100; 
		    			
		    			perf_iops = (double)(test_file_size_write / test_buffer_size_rnd) / (rnd_write_time[cntTesting] / (double)1000000);
		    			total_perf_iops += perf_iops;
		    			avg_perf_iops_rw = (int)(total_perf_iops/(double)(cntTesting+1)*100)/(double)100;
	            	}
	            }
    		}

    		
    		if(flag_sqlitebench){
    			changeDialog(PROC_SQLITE_INSERT, 0);
    			List<Object> shNum = new ArrayList<Object>();
    			for(int i = 0; i < test_num_sqlite; i++) shNum.add(i);
    			
	            SQLiteDatabase.releaseMemory();
	            sqlite_insert_time = 0;
	            Date startTime;
	            Date endTime;
	            
	   			/*******************************************************************************************
	   			 * SQLite benchmarks :: INSERT benchmarks 
	   			 *******************************************************************************************/	            
	    		testSQLite = new Testing_SQLite_DBHelper(mContext, 1);
	    		dbTesting = testSQLite.getWritableDatabase();
	            
	            for(int i = 0; i < test_num_sqlite; i++){
	            	startTime = new Date();
	            	dbTesting.execSQL("INSERT INTO testing VALUES (null, " + i + ", 1234, 'feelsogood', 0, 0, 1306118060583, 0, 1, 1, '2249i209ec9a88b17496a', 0, 0, 'com.google', 'androbench', 0, 'androbench@csl');");
	            	endTime = new Date();
	            	SQLiteDatabase.releaseMemory();
	            	sqlite_insert_time = sqlite_insert_time + (endTime.getTime() - startTime.getTime());
	            	changeDialog(PROC_SQLITE_INSERT,i);
	            }
	            
	            sec_sqlite_insert = (int)(((double)sqlite_insert_time / (double)1000) * 100) / (double)100;
	            perf_sqlite_insert = (int)(((double)test_num_sqlite / ((double)sqlite_insert_time / (double)1000)) * 100) / (double)100;
	            
	            testSQLite.close();
	                        
	   			/*******************************************************************************************
	   			 * SQLite benchmarks :: UPDATE benchmarks 
	   			 *******************************************************************************************/
	    		testSQLite = new Testing_SQLite_DBHelper(mContext, 1);
	    		dbTesting = testSQLite.getWritableDatabase();
	            
	            SQLiteDatabase.releaseMemory();
	            sqlite_update_time = 0;

    			Collections.shuffle(shNum);
	            for(int i = 0; i < test_num_sqlite; i++){
	            	startTime = new Date();
	            	dbTesting.execSQL("UPDATE testing SET photo_id = 456" + i + ", custom_ringtone = 'notbad', send_to_voicemail = 1, times_contacted  = 1, last_time_contacted = 1306118060000, starred = 1, in_visible_group = 0, has_phone_number = 0, lookup = '2249i209ec9a88b17496b', status_update_id = 1, single_is_restricted = 1, ext_account_Type = 'google.com', ext_photo_url = 'andromeda', vip = 1, display_name = 'andromeda@csl' WHERE name_raw_contact_id = " + shNum.get(i) + ";");
	            	endTime = new Date();
	            	SQLiteDatabase.releaseMemory();
	            	sqlite_update_time = sqlite_update_time + (endTime.getTime() - startTime.getTime());
	            	changeDialog(PROC_SQLITE_UPDATE,i);
	            }
	            
	            sec_sqlite_update = (int)(((double)sqlite_update_time / (double)1000) * 100) / (double)100;
	            perf_sqlite_update = (int)(((double)test_num_sqlite / ((double)sqlite_update_time / (double)1000)) * 100) / (double)100;
	            
	            testSQLite.close();
	            
	   			/*******************************************************************************************
	   			 * SQLite benchmarks :: DELETE benchmarks 
	   			 *******************************************************************************************/
	    		testSQLite = new Testing_SQLite_DBHelper(mContext, 1);
	    		dbTesting = testSQLite.getWritableDatabase();
	            
	            SQLiteDatabase.releaseMemory();
	            sqlite_delete_time = 0;

    			Collections.shuffle(shNum);
	            for(int i = 0; i < test_num_sqlite; i++){
	            	startTime = new Date();
	            	dbTesting.execSQL("DELETE FROM testing WHERE name_raw_contact_id = " + shNum.get(i) + ";");
	            	endTime = new Date();
	            	SQLiteDatabase.releaseMemory();
	            	sqlite_delete_time = sqlite_delete_time + (endTime.getTime() - startTime.getTime());
	            	changeDialog(PROC_SQLITE_DELETE,i);
	            }
	            
	            sec_sqlite_delete = (int)(((double)sqlite_delete_time / (double)1000) * 100) / (double)100;
	            perf_sqlite_delete = (int)(((double)test_num_sqlite / ((double)sqlite_delete_time / (double)1000)) * 100) / (double)100;
	            
	            testSQLite.close();
    		}
    		
    		if(flag_macro){
    			long temp_total_time = 0;
    			int scriptLine = 0;
    			  			
	   			/*******************************************************************************************
	   			 * Macro benchmarks :: Browser (Facebook, eBay, Amazon)
	   			 *******************************************************************************************/	
	    		test_macro_name = "Browser";
	    		test_macro_length = 6;

	    		for(cntTesting=0; cntTesting<number_of_testing; cntTesting++){
	    			scriptLine = copyScript(mContext.getAssets(), "browser_facebook.script");
	    			macro_browser_fb_time[cntTesting] = MACRO(test_target, MemoryStatus.getSDExternalPath() + TARGET_PATH, "browser_facebook.script", 1, scriptLine) / 1000;
	    			if(macro_browser_fb_time[cntTesting] < 0)
	    				ERROR_CHECK(macro_browser_fb_time[cntTesting]);
	    			deleteDir(test_target, MemoryStatus.getSDExternalPath() + TARGET_PATH);    			

	    			scriptLine = copyScript(mContext.getAssets(), "browser_ebay.script");
    				macro_browser_eb_time[cntTesting] = MACRO(test_target, MemoryStatus.getSDExternalPath() + TARGET_PATH, "browser_ebay.script", 2, scriptLine) / 1000;
	    			if(macro_browser_eb_time[cntTesting] < 0)
	    				ERROR_CHECK(macro_browser_eb_time[cntTesting]);
	    			deleteDir(test_target, MemoryStatus.getSDExternalPath() + TARGET_PATH);
	    			
	    			scriptLine = copyScript(mContext.getAssets(), "browser_amazon.script");
    				macro_browser_eb_time[cntTesting] = MACRO(test_target, MemoryStatus.getSDExternalPath() + TARGET_PATH, "browser_amazon.script", 3, scriptLine) / 1000;
	    			if(macro_browser_az_time[cntTesting] < 0)
	    				ERROR_CHECK(macro_browser_az_time[cntTesting]);
	    			deleteDir(test_target, MemoryStatus.getSDExternalPath() + TARGET_PATH);
    			}
	    		
	    		temp_total_time = 0;
	    		for(cntTesting=0; cntTesting<number_of_testing; cntTesting++){
	    			temp_total_time = temp_total_time + macro_browser_fb_time[cntTesting];
	    			temp_total_time = temp_total_time + macro_browser_eb_time[cntTesting];
	    			temp_total_time = temp_total_time + macro_browser_az_time[cntTesting];
	    		}
	    		
	    		avg_macro_browser = (int)(temp_total_time/(double)(cntTesting+1)*100)/(double)100;
   			
	   			/*******************************************************************************************
	   			 * Macro benchmarks :: Market
	   			 *******************************************************************************************/
    			test_macro_name = "Market";
    			test_macro_length = 2;
    			
    			scriptLine = copyScript(mContext.getAssets(), "market.script");
    			for(cntTesting=0; cntTesting<number_of_testing; cntTesting++){
    				macro_market_time[cntTesting] = MACRO(test_target, MemoryStatus.getSDExternalPath() + TARGET_PATH, "market.script", 1, scriptLine) / 1000;
	    			if(macro_market_time[cntTesting] < 0)
	    				ERROR_CHECK(macro_market_time[cntTesting]);
	    			deleteDir(test_target, MemoryStatus.getSDExternalPath() + TARGET_PATH);
    			}
    			
	    		temp_total_time = 0;
	    		for(cntTesting=0; cntTesting<number_of_testing; cntTesting++){
	    			temp_total_time = temp_total_time + macro_market_time[cntTesting];
	    		}
	    		avg_macro_market = (int)(temp_total_time/(double)(cntTesting+1)*100)/(double)100;
    			
	   			/*******************************************************************************************
	   			 * Macro benchmarks :: Camera
	   			 *******************************************************************************************/
    			test_macro_name = "Camera";
    			test_macro_length = 2;
    			
    			scriptLine = copyScript(mContext.getAssets(), "camera_photo.script");
    			for(cntTesting=0; cntTesting<number_of_testing; cntTesting++){
    				macro_camera_time[cntTesting] = MACRO(test_target, MemoryStatus.getSDExternalPath() + TARGET_PATH, "camera_photo.script", 1, scriptLine) / 1000;
	    			if(macro_camera_time[cntTesting] < 0)
	    				ERROR_CHECK(macro_camera_time[cntTesting]);
	    			deleteDir(test_target, MemoryStatus.getSDExternalPath() + TARGET_PATH);
    			}
    			
	    		temp_total_time = 0;
	    		for(cntTesting=0; cntTesting<number_of_testing; cntTesting++){
	    			temp_total_time = temp_total_time + macro_camera_time[cntTesting];
	    		}
	    		avg_macro_camera = (int)(temp_total_time/(double)(cntTesting+1)*100)/(double)100;
    			
	   			/*******************************************************************************************
	   			 * Macro benchmarks :: Camcorder
	   			 *******************************************************************************************/
    			test_macro_name = "Camcorder";
    			test_macro_length = 2;
    			
    			scriptLine = copyScript(mContext.getAssets(), "camera_movie.script");
    			for(cntTesting=0; cntTesting<number_of_testing; cntTesting++){
    				macro_camcorder_time[cntTesting] = MACRO(test_target, MemoryStatus.getSDExternalPath() + TARGET_PATH, "camera_movie.script", 1, scriptLine) / 1000;
	    			if(macro_camcorder_time[cntTesting] < 0)
	    				ERROR_CHECK(macro_camcorder_time[cntTesting]);
	    			deleteDir(test_target, MemoryStatus.getSDExternalPath() + TARGET_PATH);
    			}
    			
	    		temp_total_time = 0;
	    		for(cntTesting=0; cntTesting<number_of_testing; cntTesting++){
	    			temp_total_time = temp_total_time + macro_camcorder_time[cntTesting];
	    		}
	    		avg_macro_camcorder = (int)(temp_total_time/(double)(cntTesting+1)*100)/(double)100;
    		}

            postExcute();
    	}
    }
    
    public class Thread_SND_Result extends Thread{
    	public void run(){
    		int try_send;
    	    
    		if(flag_microbench){
	    	    try_send = 0;
	    		send_success_micro = false;
	    		while((try_send < 3) && (!send_success_micro)){
		        	try{
		            	String snd_info;
		            	String bench_target;
		            	String target_filesystem;
		            	
		            	if(test_target == 0){
		            		bench_target = "/data";
		            		target_filesystem = MemoryStatus.getInternalFilesystem();
		            	}else if(test_target == 1){
		            		bench_target = "/sdcard";
		            		target_filesystem = MemoryStatus.getExternalFilesystem();
		            	}else{
		            		bench_target = MemoryStatus.getSDExternalPath();
		            		target_filesystem = MemoryStatus.getSDExternalFilesystem();
		            	}
		            		            	
		            	snd_info = "http://www.androbench.org/db_androbench/recorder_micro.php?target=" + bench_target
		            	+ "&filesystem=" + target_filesystem
		    			+ "&filesize_read=" + (test_file_size_read/MBYTE) 
		    			+ "&filesize_write=" + (test_file_size_write/MBYTE) 
		    			+ "&buffersize_seq=" + (test_buffer_size_seq/KBYTE) 
		    			+ "&buffersize_rnd=" + (test_buffer_size_rnd/KBYTE) 
		    			+ "&use_buffer=" + test_file_flag
		    			+ "&avg_mbps_sr=" + avg_perf_mbps_sr
		    			+ "&avg_mbps_sw=" + avg_perf_mbps_sw
		    			+ "&avg_mbps_rr=" + avg_perf_mbps_rr
		    			+ "&avg_iops_rr=" + avg_perf_iops_rr
		    			+ "&avg_mbps_rw=" + avg_perf_mbps_rw
		    			+ "&avg_iops_rw=" + avg_perf_iops_rw
		    			+ "&device_model=" + Build.MODEL.toString().replaceAll(" ", "%20")
		    			+ "&androbench_ver=" + mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName
		    			+ "&android_ver=" + Build.VERSION.RELEASE
		    			+ "&kernel_ver=" + VersionCheck.getKernelVersion()
		    			+ "&build_ver=" + Build.VERSION.INCREMENTAL;
		            	URL url = new URL(snd_info);
		            	
		            	
		            	HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		            	if(conn != null){
		            		conn.setConnectTimeout(10000);
		            		conn.setUseCaches(false);
		            		if(conn.getResponseCode() == HttpURLConnection.HTTP_OK){
		            			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		            			// Success : "success", Fail : "fail"
		            			if(br.readLine().equals("success")){
		            				send_success_micro = true;
		            			}
		            		}
		            		conn.disconnect();
		            	}
		            }catch(Exception ex){;}
		            
		            try_send++;
	    		}
    		}
    		
    		if(flag_sqlitebench){
	    		try_send = 0;
	    		send_success_sqlite = false;
	    		while((try_send < 3) && (!send_success_sqlite)){
		        	try{
		            	String snd_info;
		            	String bench_target;
		            	String target_filesystem;
		            	
		            	if(test_target == 0){
		            		bench_target = "/data";
		            		target_filesystem = MemoryStatus.getInternalFilesystem();
		            	}else if(test_target == 1){
		            		bench_target = "/sdcard";
		            		target_filesystem = MemoryStatus.getExternalFilesystem();
		            	}else{
		            		bench_target = MemoryStatus.getSDExternalPath();
		            		target_filesystem = MemoryStatus.getSDExternalFilesystem();
		            	}
		            	
		            	snd_info = "http://www.androbench.org/db_androbench/recorder_sqlite.php?target=&device_model=" + Build.MODEL.toString().replaceAll(" ", "%20")
		    			+ "&perf_insert=" + perf_sqlite_insert
		    			+ "&perf_update=" + perf_sqlite_update
		    			+ "&perf_delete=" + perf_sqlite_delete
		            	+ "&num_sqlite=" + test_num_sqlite
		    			+ "&androbench_ver=" + mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName
		    			+ "&android_ver=" + Build.VERSION.RELEASE
		    			+ "&kernel_ver=" + VersionCheck.getKernelVersion()
		    			+ "&build_ver=" + Build.VERSION.INCREMENTAL;
		            	URL url = new URL(snd_info);
		            	
		            	HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		            	if(conn != null){
		            		conn.setConnectTimeout(10000);
		            		conn.setUseCaches(false);
		            		if(conn.getResponseCode() == HttpURLConnection.HTTP_OK){
		            			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		            			// Success : "success", Fail : "fail"
		            			if(br.readLine().equals("success")){
		            				send_success_sqlite = true;
		            			}
		            		}
		            		conn.disconnect();
		            	}
		            }catch(Exception ex){;}
		            
		            try_send++;
	    		}
    		}
    		
    		if(flag_macro){
	    	    try_send = 0;
	    	    send_success_macro = false;
	    	    
	    	    SQLiteDatabase macro_db;
	    	    Cursor macro_cursor;
	    	    int macro_device_rnd = 0;
	    	    
	    	    macro_db = deviceDBHelper.getReadableDatabase();
	    	    macro_cursor = macro_db.rawQuery("SELECT device_rnd FROM device", null);
	    		while(macro_cursor.moveToNext()){
	    			macro_device_rnd += macro_cursor.getInt(0); 
	    		}
	    		macro_cursor.close();
	    		macro_db.close();
	    	    	    	    
	    		while((try_send < 3) && (!send_success_macro)){
		        	try{
		            	String snd_info;
		            	String bench_target;
		            	String target_filesystem;
		            	
		            	if(test_target == 0){
		            		bench_target = "/data";
		            		target_filesystem = MemoryStatus.getInternalFilesystem();
		            	}else if(test_target == 1){
		            		bench_target = "/sdcard";
		            		target_filesystem = MemoryStatus.getExternalFilesystem();
		            	}else{
		            		bench_target = MemoryStatus.getSDExternalPath();
		            		target_filesystem = MemoryStatus.getSDExternalFilesystem();
		            	}
		            		            	
		            	snd_info = "http://www.androbench.org/db_androbench/recorder_macro.php?target=" + bench_target
		            	+ "&filesystem=" + target_filesystem
		    			+ "&avg_browser=" + avg_macro_browser
		    			+ "&avg_market=" + avg_macro_market
		    			+ "&avg_camera=" + avg_macro_camera
		    			+ "&avg_camcorder=" + avg_macro_camcorder
		    			+ "&device_model=" + Build.MODEL.toString().replaceAll(" ", "%20")
		    			+ "&androbench_ver=" + mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName
		    			+ "&android_ver=" + Build.VERSION.RELEASE
		    			+ "&kernel_ver=" + VersionCheck.getKernelVersion()
		    			+ "&build_ver=" + Build.VERSION.INCREMENTAL
		    			+ "&device_rnd=" + macro_device_rnd;
		            	URL url = new URL(snd_info);
		            	
		            	
		            	HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		            	if(conn != null){
		            		conn.setConnectTimeout(10000);
		            		conn.setUseCaches(false);
		            		if(conn.getResponseCode() == HttpURLConnection.HTTP_OK){
		            			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		            			// Success : "success", Fail : "fail"
		            			if(br.readLine().equals("success")){
		            				send_success_macro = true;
		            			}
		            		}
		            		conn.disconnect();
		            	}
		            }catch(Exception ex){;}
		            
		            try_send++;
	    		}
    		}
            
            runOnUiThread(new Runnable(){
    			
    			public void run(){
    				if(send_success_micro || send_success_sqlite || send_success_macro){
    					Toast.makeText(mContext, "Transmission complete", Toast.LENGTH_SHORT).show();
    				}else{
    					Toast.makeText(mContext, "Transmission failed", Toast.LENGTH_SHORT).show();
    				}
    			}
            });
    	}
    }
}

class Testing_Data{
	String Testing_Name;
	String Testing_Status;
	
	Testing_Data(String Testing_Name, String Testing_Status){
		this.Testing_Name = Testing_Name;
		this.Testing_Status = Testing_Status;
	}
	public String getName(){
		return Testing_Name;
	}
	public String getStatus(){
		return Testing_Status;
	}
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: _empty_/MemoryStatus#getSDExternalPath#