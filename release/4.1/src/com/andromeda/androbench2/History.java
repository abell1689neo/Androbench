package com.andromeda.androbench2;

import java.util.ArrayList;
import java.util.Collections;

import com.andromeda.androbench2.HistoryData;
import com.andromeda.androbench2.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class History extends Activity{
	
	// Create class for list of history
	private ArrayList<HistoryData> al_History;
	private ListView listview;
	private GroupAdapter adapter;
	
	// Create ListView item message
	TextView tv_history_message;
	TextView tv_recent_benchmarking;
	
	// Create SQL helper for list of history
	SQLiteDatabase db;
	private HistoryDB historyDBHelper;
	
	// Create clear history button
    Button btnClearHistory;
	
    
    @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history);
                
        // Connect ListView for list of history
        listview = (ListView)findViewById(R.id.HistoryListView);
        al_History=new ArrayList<HistoryData>();

        // Connect TextView for notifying of not have list
        tv_history_message = (TextView)findViewById(R.id.HistoryMessage);
        
        // Connect TextView for notifying of recent benchmarking
        tv_recent_benchmarking = (TextView)findViewById(R.id.RecentBenchmarking);
        
        // Click item of history
        listview.setOnItemClickListener(new OnItemClickListener(){
        	
        	public void onItemClick(AdapterView<?> parent, View view, int position, long id){
        		ItemClick(parent, view, position, id);
        	}
        });
        
        // Create Database helper(with Android)
        historyDBHelper = new HistoryDB(this, null, null);
    	db = historyDBHelper.getReadableDatabase();
    	
    	// Clear History Button Process
        btnClearHistory = (Button)findViewById(R.id.btnClearHistory);
        btnClearHistory.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
				// Clear all of history
				db.delete("history", null, null);
				db.execSQL("DELETE FROM history;");
				
				// Clear ListView items of histories
				al_History.clear();
				listview.setAdapter(adapter);
				
				// Clear Recent benchmarking date
				tv_recent_benchmarking.setText("Recent benchmarking\n(No history)");
				
				tv_history_message.setText("You have no benchmark history");
			}
		});
    }
    
    
    @Override
	public void onDestroy(){
    	super.onDestroy();
    	
    	// Close Database helper
    	historyDBHelper.close();
    }
    
    // Click list item, provide information of history in the past
    void ItemClick(AdapterView<?> parent, View view, int position, long id){
    	
    	// Create AlertDialog Builder for AlertDialog
    	AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
    	alt_bld.setMessage(al_History.get(position).getResult()).setCancelable(false).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			
			
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				
			}});
    	
    	// Create AlertDialog for provide information of history in the past. Setting title and icon
    	AlertDialog alert = alt_bld.create();
    	alert.setTitle("History");
    	alert.setIcon(R.drawable.icon_history);
    	alert.show();
    }
    
    
    @Override
	public void onResume(){
    	super.onResume();

    	// Create String for recent benchmarking date
    	String RecentDate = null;
    	
    	// Create Database cursor for read database
    	Cursor cursor;
    	cursor = db.query("history", new String[]{"date", "target", "filesize_read", "filesize_write", "buffersize_seq", "buffersize_rnd", "use_buffer", "avg_mbps_sr", "avg_mbps_sw", "avg_mbps_rr", "avg_iops_rr", "avg_mbps_rw", "avg_iops_rw", "perf_sqlite_insert", "perf_sqlite_update", "perf_sqlite_delete", "macro_browser_time", "macro_market_time", "macro_camera_time", "macro_camcorder_time", "one_filesize", "num_thread"}, null, null, null, null, null);
    	
    	// Clear ArrayList for re-read database
    	al_History.clear();
    	
    	// Re-read from database
    	while(cursor.moveToNext()){
    		RecentDate = cursor.getString(0);
    		al_History.add(new HistoryData(cursor.getString(0), cursor.getString(1), cursor.getInt(2), cursor.getInt(3), cursor.getInt(4), cursor.getInt(5), cursor.getString(6), cursor.getDouble(7), cursor.getDouble(8), cursor.getDouble(9), cursor.getDouble(10), cursor.getDouble(11), cursor.getDouble(12), cursor.getDouble(13), cursor.getDouble(14), cursor.getDouble(15), cursor.getDouble(16), cursor.getDouble(17), cursor.getDouble(18), cursor.getDouble(19),cursor.getInt(20),cursor.getInt(21)));
    	}
    	
    	// Sorting ArrayList items of histories
    	Collections.reverse(al_History);
    	
    	// Read recent benchmarking date
    	if(cursor.getCount() != 0){
    		tv_recent_benchmarking.setText("Recent benchmarking\n" + RecentDate);
    	}
    	
    	// Close Database cursor
    	cursor.close();
    	
    	// Connect ArrayList items and ListView
        if(al_History.size()>0){
        	tv_history_message.setText("");
        	adapter = new GroupAdapter(this, R.layout.row_history, al_History);
        	listview.setAdapter(adapter);
        }else{
        	// If no items in ArrayList, view empty message 
        	tv_history_message.setText("You have no benchmark history");
        }
    }
    
    
    // Guide class for connect ArrayList and ListView
    public class GroupAdapter extends ArrayAdapter<Object>{
    	private ArrayList<HistoryData> item;
    	private HistoryData temp;
    	
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
    			v = vi.inflate(R.layout.row_history, null);
    		}
    		
    		if(temp != null){
    			TextView tv_history_date = (TextView)v.findViewById(R.id.row_history_date);
    			tv_history_date.setText(temp.getDate());
    		}
    		return v;
    	}
    }
}

