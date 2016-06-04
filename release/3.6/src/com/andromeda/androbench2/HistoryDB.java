package com.andromeda.androbench2;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

// Guide Class for history database
public class HistoryDB extends SQLiteOpenHelper
{
	public HistoryDB(Context context, String name, CursorFactory factory)
	{
		super(context, "history.db", null, 6);
	}
	
	
	public void onCreate(SQLiteDatabase db)
	{
		db.execSQL("create table history (_id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, target TEXT, filesize_read INTEGER, filesize_write INTEGER, buffersize_seq INTEGER, buffersize_rnd INTEGER, use_buffer TEXT, avg_mbps_sr DOUBLE, avg_mbps_sw DOUBLE, avg_mbps_rr DOUBLE, avg_iops_rr DOUBLE, avg_mbps_rw DOUBLE, avg_iops_rw DOUBLE, perf_sqlite_insert DOUBLE, perf_sqlite_update DOUBLE, perf_sqlite_delete DOUBLE, macro_browser_time DOUBLE, macro_market_time DOUBLE, macro_camera_time DOUBLE, macro_camcorder_time DOUBLE);");
	}

	
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
	{
		db.execSQL("drop table if exists history");
		onCreate(db);
	}
}

