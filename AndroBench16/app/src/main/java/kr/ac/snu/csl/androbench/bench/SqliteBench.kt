package kr.ac.snu.csl.androbench.bench

import android.content.Context

//actual measurement occurs here


//benchmark 1회 시행 결과
data class SqliteResult(
    //members:
    val insertElapsedNs: Long,
    val updateElapsedNs: Long,
    val deleteElapsedNs: Long,
    val numOperations: Int,
){
    //method: 
    fun insertTps(): Double=numOperations/(insertElapsedNs/1e9)
    fun updateTps(): Double=numOperations/(updateElapsedNs/1e9)
    fun deleteTps(): Double=numOperations/(deleteElapsedNs/1e9)
}

/*
* SQLite Insert, update, delete benchmark
*/

class SqliteBench(private val context: Context){
    fun run(numOperations: Int=1024):SqliteResult{
        //input numoperations, return sqliteresult struct

        val helper=BenchDbHelper(context) //from BenchDbHelper.kt
        val db=helper.writableDatabase  //oncreate on first call

        db.delete(BenchDbHelper.TABLE_NAME, null, null) //remove remaining dbs

        //-------INSERT Phase---------//
        // append style sequential write -> journal -> fsync -> erase journal
        val insertStart=System.nanoTime()
        for(i in 0 until numOperations){
            db.execSQL(
                "INSERT INTO testing VALUES"+
                "(null, $i, 1234, 'feelsogood', 0, 0, 1306118060583, 0, 1, 1, " +
                "'2249i209ec9a88b17496a', 0, 0, 'com.google', 'androbench', 0, " +
                "'androbench@csl')"
            )
        }
        val insertElapsedNs=System.nanoTime()-insertStart

        //-------UPDATE Phase---------//
        //random read/write + journal/fsync
        //no page cache effect(shuffled)
        val updateOrder=(0 until numOperations).shuffled() //shuffle key to update in random order (real disk access pattern)
        val updateStart=System.nanoTime()
        for (i in 0 until numOperations){
            val key=updateOrder[i]
            db.execSQL(
                "UPDATE testing SET " +
                "photo_id = ${456 + key}, custom_ringtone = 'notbad', " +
                "send_to_voicemail = 1, times_contacted = 1, " +
                "last_time_contacted = 1306118060000, starred = 1, " +
                "in_visible_group = 0, has_phone_number = 0, " +
                "lookup = '2249i209ec9a88b17496b', status_update_id = 1, " +
                "single_is_restricted = 1, ext_account_Type = 'google.com', " +
                "ext_photo_url = 'andromeda', vip = 1, " +
                "display_name = 'andromeda@csl' " +
                "WHERE name_raw_contact_id = $key"
            )
        }
        val updateElapsedNs=System.nanoTime()-updateStart

        //-------DELETE Phase---------//
        //random read/write + fsync
        val deleteOrder=(0 until numOperations).shuffled()
        val deleteStart=System.nanoTime()
        for (i in 0 until numOperations){
            val key=deleteOrder[i]
            db.execSQL("DELETE FROM testing WHERE name_raw_contact_id=$key")
        }
        val deleteElapsedNs=System.nanoTime()-deleteStart

        db.close()
        helper.close()

        return SqliteResult(
            insertElapsedNs=insertElapsedNs,
            updateElapsedNs=updateElapsedNs, 
            deleteElapsedNs=deleteElapsedNs, 
            numOperations=numOperations,)
    }
}

//row 마다 2번씩 fsync: journal fsync(변경 전 페이지) + 수정 db fsync