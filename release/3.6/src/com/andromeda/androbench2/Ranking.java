package com.andromeda.androbench2;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import com.andromeda.androbench2.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class Ranking extends Activity{
	
	Context mContext;
	Intent itRank;
	
	private ArrayList<RankingData> alRanking;
	private ListView lvRanking;
	private GroupAdapter gaRanking;
	
	double maxRankValue;
	
	TextView tvRankingTitle;
	
	ProgressDialog progDialog;
	
	String rank_info = "http://www.androbench.org/db_androbench/rank.php?type=";
	String strUnit;
	int try_send;
	boolean send_success;
	
	// DATABASE Interface
	private HistoryDB historyDBHelper;
	SQLiteDatabase db;
	
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ranking);
        
        // DB Helper for saving history
        historyDBHelper = new HistoryDB(this, null, null);
        
        mContext = getApplicationContext();
        
        tvRankingTitle = (TextView)findViewById(R.id.tv_ranking_title);
        lvRanking = (ListView)findViewById(R.id.RankingListView);
        alRanking = new ArrayList<RankingData>();
                
		itRank = getIntent();
		
		if(itRank.getExtras().getString("type").equals("v_avg_sr")
				|| itRank.getExtras().getString("type").equals("v_avg_sw")){
			strUnit = " MB/s";			
		}else if(itRank.getExtras().getString("type").equals("v_avg_rr")
				|| itRank.getExtras().getString("type").equals("v_avg_rw")){
			strUnit = " IOPS(4K)";
		}else if(itRank.getExtras().getString("type").equals("v_perf_insert")
				|| itRank.getExtras().getString("type").equals("v_perf_update")
				|| itRank.getExtras().getString("type").equals("v_perf_delete")){
			strUnit = " TPS";
		}else if(itRank.getExtras().getString("type").equals("v_avg_browser")
				|| itRank.getExtras().getString("type").equals("v_avg_market")
				|| itRank.getExtras().getString("type").equals("v_avg_camera")
				|| itRank.getExtras().getString("type").equals("v_avg_camcorder")){
			strUnit = " msec";
		}
		
		String[] strRankingTitle = itRank.getExtras().getString("type").split("_");
		
		tvRankingTitle.setText("Ranking : " + strRankingTitle[2].toUpperCase());
		

		rank_info += itRank.getExtras().getString("type");

    	progDialog = new ProgressDialog(this);
        progDialog.setMessage("Loading...");
        progDialog.setIndeterminate(true);
        progDialog.show();
        new AddRankingList().execute();
    }
    
    
    public void onDestroy(){
    	super.onDestroy();
    	historyDBHelper.close();
    }
    
    class historyAVG{
    	private int cntHistory(){
    		int cnt;
    		String colType;
    		Cursor cursor;
    		colType = chkType(itRank.getExtras().getString("type"));
    		db = historyDBHelper.getReadableDatabase();
    		cursor = db.rawQuery("SELECT * FROM history WHERE " + colType + ">0;", null);
    		cnt = cursor.getCount();
    		cursor.close();
    		db.close();
    		return cnt;
    	}
    	
    	private String avgHistory(){
    		String colType;
    		Cursor cursor;
    		double avgHis;
    		colType = chkType(itRank.getExtras().getString("type"));
    		db = historyDBHelper.getReadableDatabase();
    		cursor = db.rawQuery("SELECT " + colType + " FROM history WHERE " + colType + ">0;", null);
    		
    		avgHis = 0;
    		while(cursor.moveToNext()){
    			avgHis += cursor.getDouble(0); 
    		}
    		avgHis = (int)(avgHis/(double)(cursor.getCount())*100)/(double)100;
    		
    		cursor.close();
    		db.close();

    		return "" + avgHis;
    	}
    	
    	
    	private String chkType(String strSrc){
    		String strRtn;
    		strRtn = "unknown";
    		
    		if(strSrc.equals("v_avg_sr")){
    			strRtn = "avg_mbps_sr";
    		}else if(strSrc.equals("v_avg_sw")){
    			strRtn = "avg_mbps_sw";
    		}else if(strSrc.equals("v_avg_rr")){
    			strRtn = "avg_iops_rr";
    		}else if(strSrc.equals("v_avg_rw")){
    			strRtn = "avg_iops_rw";
    		}else if(strSrc.equals("v_perf_insert")){
    			strRtn = "perf_sqlite_insert";
    		}else if(strSrc.equals("v_perf_update")){
    			strRtn = "perf_sqlite_update";
    		}else if(strSrc.equals("v_perf_delete")){
    			strRtn = "perf_sqlite_delete";
    		}else if(strSrc.equals("v_avg_browser")){
    			strRtn = "macro_browser_time";
    		}else if(strSrc.equals("v_avg_market")){
    			strRtn = "macro_market_time";
    		}else if(strSrc.equals("v_avg_camera")){
    			strRtn = "macro_camera_time";
    		}else if(strSrc.equals("v_avg_camcorder")){
    			strRtn = "macro_camcorder_time";
    		}
    		
    		return strRtn;
    	}
    }
    
    class AddRankingList extends AsyncTask<Void, Void, Void> {
    	
		
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub
			
			try_send = 0;
			send_success = false;
			maxRankValue = 0;
			//maxRankValue = itRank.getExtras().getDouble("result");
			while((try_send < 3) && (!send_success)){
	        	try{
	            	URL url = new URL(rank_info);
	            	
	            	HttpURLConnection conn = (HttpURLConnection)url.openConnection();
	            	if(conn != null){
	            		conn.setConnectTimeout(10000);
	            		conn.setUseCaches(false);
	            		if(conn.getResponseCode() == HttpURLConnection.HTTP_OK){
	            			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	            			// Success : "success"
	            			if(br.readLine().equals("success")){
	            				send_success = true;
	            				String strResult = br.readLine();
	            				String spilitResult[];
	            				
	            				spilitResult = strResult.split("##");
	            				
	            				for(int i=0; i<spilitResult.length; i++){
	            					String partResult[];
	            					partResult = spilitResult[i].split("~~");
	            					
	            					if(i==0){

	            						if(itRank.getExtras().getDouble("result") >= Double.parseDouble(partResult[1])){
	            							maxRankValue = itRank.getExtras().getDouble("result");
	            						}else{
	            							maxRankValue = Double.parseDouble(partResult[1]);
	            						}
	            						String msgValue;
	            						historyAVG hAVG = new historyAVG();
	            						
	            						if(itRank.getExtras().getBoolean("flag_test")){
	            							if(hAVG.cntHistory() > 0){
	            								alRanking.add(new RankingData("-", "Your Device", itRank.getExtras().getDouble("result") + " (Avg: " + hAVG.avgHistory() + ")" + strUnit, itRank.getExtras().getDouble("result"), maxRankValue));
	            							}else{
	            								alRanking.add(new RankingData("-", "Your Device", itRank.getExtras().getDouble("result") + strUnit, itRank.getExtras().getDouble("result"), maxRankValue));
	            							}
	            						}else{
	            							if(hAVG.cntHistory() > 0){
	            								alRanking.add(new RankingData("-", "Your Device", "Avg: " + hAVG.avgHistory() + strUnit, Double.parseDouble(hAVG.avgHistory()), maxRankValue));
	            							}
	            						}
	            					}
	            					
	            					if(maxRankValue < Double.parseDouble(partResult[1])){
	            						maxRankValue = Double.parseDouble(partResult[1]);
	            					}
	            					
	            					
	            					alRanking.add(new RankingData("" + (i+1), partResult[0], partResult[1] + strUnit, Double.parseDouble(partResult[1]), maxRankValue));
	            					
	            				}
	            			}
	            		}
	            		conn.disconnect();
	            	}
	            }catch(Exception ex){;}
	            
	            try_send++;
			}

			return null;
		}
		
        
        protected void onPostExecute(Void unused){
        	progDialog.dismiss();
        	
	        if(alRanking.size()>0){
	        	gaRanking = new GroupAdapter(mContext, R.layout.row_rank, alRanking);
	        	lvRanking.setAdapter(gaRanking);
	        }
	        
	        if(!send_success){
	        	Toast.makeText(mContext, "Connection failed : The ranking service needs to connect internet. Check your network connection", Toast.LENGTH_LONG).show();
	        	if(alRanking.size()==0){
	        		finish();
	            }
	        }
        }
    	
    }
    
    private class GroupAdapter extends ArrayAdapter<Object>{
    	private ArrayList<RankingData> item;
    	private RankingData temp;
    	
    	public GroupAdapter(Context ctx, int resourceID, ArrayList item){
    		super(ctx, resourceID, item);
    		this.item = item;
    	}
    	
    	public View getView(int position, View convertView, ViewGroup parent){
    		View v = convertView;
    		
    		temp = item.get(position);
    		
    		if(v == null){
    			LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    			v = vi.inflate(R.layout.row_rank, null);
    		}
    		
    		if(temp != null){
    			TextView tv_title = (TextView)v.findViewById(R.id.row_rank_title);
    			tv_title.setText(temp.getTitle());
    			TextView tv_value = (TextView)v.findViewById(R.id.row_rank_value);
    			tv_value.setText(temp.getValue());
    			TextView tv_explain = (TextView)v.findViewById(R.id.row_rank_rank);
    			tv_explain.setText(temp.getRank());
    			ProgressBar pb_value = (ProgressBar)v.findViewById(R.id.row_rank_progress);
    			pb_value.setMax((int)(maxRankValue*(double)1000));
    			pb_value.setProgress(temp.getDValue());
    		}
    		return v;
    	}
    }
}

class RankingData{
	String title;
	String value;
	String rank;
	
	double dValue;
	double maxValue; 
	
	RankingData(String rank, String title, String value, double dValue, double maxValue){
		this.rank = rank;
		this.title = title;
		this.value = value;
		this.dValue = dValue;
		this.maxValue = maxValue;
	}
	public String getTitle(){
		return title;
	}
	public String getValue(){
		return value;
	}
	public String getRank(){
		return rank;
	}
	public int getDValue(){
		return (int)(dValue*(double)1000);
	}
	public int getMaxValue(){
		return (int)(maxValue*(double)1000);
	}
}
