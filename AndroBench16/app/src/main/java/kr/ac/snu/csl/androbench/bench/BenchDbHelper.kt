package kr.ac.snu.csl.androbench.bench

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/*
SQLite helper for benchmark database
*/

class BenchDbHelper(context: Context)//db 파일 저장에 필요
    : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION){ //부모 class(extended)
        //which app, db file name, cursorfactory, schema version

        override fun onCreate(db:SQLiteDatabase){ //initial: no current DB
            db.execSQL(CREATE_TABLE_SQL);
        }

        //db version upgraded
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int){
            db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
            onCreate(db);
        }

        companion object{ //static 
            const val DB_NAME="testing_sqlite.db"
            const val DB_VERSION=1
            const val TABLE_NAME="testing"

            //SQL 명령어 덩어리
            private const val CREATE_TABLE_SQL=""" 
                CREATE TABLE testing(
                    _id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name_raw_contact_id INTEGER,
                    photo_id INTEGER,
                    custom_ringtone TEXT,
                    send_to_voicemail INTEGER,
                    times_contacted INTEGER,
                    last_time_contacted INTEGER,
                    starred INTEGER,
                    in_visible_group INTEGER,
                    has_phone_number INTEGER,
                    lookup TEXT,
                    status_update_id INTEGER,
                    single_is_restricted INTEGER,
                    ext_account_Type TEXT,
                    ext_photo_url TEXT,
                    vip INTEGER,
                    display_name TEXT
                )
            """
        }
    }