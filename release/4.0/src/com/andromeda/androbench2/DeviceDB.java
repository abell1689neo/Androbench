package com.andromeda.androbench2;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

public class DeviceDB  extends SQLiteOpenHelper
{
	public DeviceDB(Context context, String name, CursorFactory factory)
	{
		super(context, "device.db", null, 2);
	}
	
	
	@Override
	public void onCreate(SQLiteDatabase db)
	{
		int device_rnd = (int)(100000000.0 * Math.random());
		ContentValues row;
		
		db.execSQL("create table device (_id INTEGER PRIMARY KEY AUTOINCREMENT, device_rnd INTEGER);");
		row = new ContentValues();
		row.put("device_rnd", device_rnd);
		db.insert("device", null, row);
	}

	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
	{
		db.execSQL("drop table if exists device");
		onCreate(db);
	}
}