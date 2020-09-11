package com.leewug.src.sqliteplus.test;

import android.Manifest;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.leewug.src.sqliteplus.Database;
import com.leewug.src.sqliteplus.Statement;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);


        //-----------------原生插入-----------------------
        long start = System.currentTimeMillis();
        SQLiteDatabase sqLiteDatabase = openOrCreateDatabase("bb.db",MODE_PRIVATE, null);
        sqLiteDatabase.execSQL("create table IF NOT EXISTS bb(A TEXT,B TEXT);");
        sqLiteDatabase.execSQL("create index IF NOT EXISTS bb_query ON bb('A' ASC);");
        sqLiteDatabase.beginTransaction();
        SQLiteStatement sqLiteStatement = sqLiteDatabase.compileStatement("insert into bb values(?,?);");
        for (int i = 0; i < 100000; i++) {
            sqLiteStatement.clearBindings();
            sqLiteStatement.bindString(1,"aaaaaa");
            sqLiteStatement.bindString(2,"bbbbbb");
            sqLiteStatement.executeInsert();
        }
        sqLiteDatabase.setTransactionSuccessful();
        sqLiteDatabase.endTransaction();
        sqLiteStatement.close();
        Log.e("--------------", "原生插入耗时: "+(System.currentTimeMillis()-start) );



        //原生查询
        start = System.currentTimeMillis();
        Cursor cursor = sqLiteDatabase.rawQuery("SELECT * from bb where A = ? limit 1", new String[]{"aaaaaa1"});
        if(cursor.moveToNext()){
            Log.e("----------", "onCreate: "+cursor.getString(0) );
        }
        Log.e("--------------", "原生查询耗时: "+(System.currentTimeMillis()-start)+"----" );


        //-----------------自定义C增强插入-----------------------
        start = System.currentTimeMillis();
        final Database database = Database.openOrCreateDatabase(getApplicationContext(),getDatabasePath("aa.db").getAbsolutePath());
        database.createTable("test","w","a");

        database.createIndex("test","test_query","w");
        Statement statement = database.beginTransaction("insert into test values(null,?,?);");
        for (int i = 0; i < 100000; i++) {
            statement.bindString(1,"wwwwww");
            statement.bindString(2,"aaaaaa");
            int ret = statement.execute();
            if(ret!=0){
                break;
            }
        }
        statement.commitTransaction();
        Log.e("--------------", "自定义C增强插入耗时: "+(System.currentTimeMillis()-start) );


        //自定义查询
        start = System.currentTimeMillis();
        database.executeQuery("SELECT * from test where w = 'aaaaaa1' limit 1");
        Log.e("--------------", "自定义查询耗时: "+(System.currentTimeMillis()-start) );


    }
}
