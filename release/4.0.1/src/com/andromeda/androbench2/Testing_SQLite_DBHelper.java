package com.andromeda.androbench2;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class Testing_SQLite_DBHelper extends SQLiteOpenHelper
{
	public Testing_SQLite_DBHelper(Context context, int version)
	{
		super(context, "testing_sqlite.db", null, version);
	}
	
	
	@Override
	public void onCreate(SQLiteDatabase db)
	{
		db.execSQL("create table testing (_id INTEGER PRIMARY KEY AUTOINCREMENT, name_raw_contact_id INTEGER, photo_id INTEGER, custom_ringtone TEXT, send_to_voicemail INTEGER, times_contacted INTEGER, last_time_contacted INTEGER, starred INTEGER, in_visible_group INTEGER, has_phone_number INTEGER, lookup TEXT, status_update_id INTEGER, single_is_restricted INTEGER, ext_account_Type TEXT, ext_photo_url TEXT, vip INTEGER, display_name TEXT);");
	}

	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
	{
		db.execSQL("drop table if exists testing");
		onCreate(db);
	}
}