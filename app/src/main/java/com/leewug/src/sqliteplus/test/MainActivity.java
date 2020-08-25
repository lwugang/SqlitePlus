package com.leewug.src.sqliteplus.test;

import android.Manifest;
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
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        Database database = Database.openOrCreateDatabase(getApplicationContext(),
                new File(Environment.getExternalStorageDirectory(),"aa.db").getAbsolutePath());
        database.createTable("test","w","a");

        long start = System.currentTimeMillis();

//        Object[][] values = new Object[1000000][];
//        for (int i = 0; i < values.length; i++) {
//            values[i] = new Object[2];
//            values[i][0] = "314312";
//            values[i][1] = "fdsafdsa";
//        }
//        database.insertBatch("insert into test values(null,?,?);",values);

        Log.e("------", "onCreate: "+database.executeQuery("select * from test where a='aaaaaa';"));
//        database.testInsert(1000000);
        Statement statement = database.beginTransaction("insert into test values(null,?,?);");
        for (int i = 0; i < 1000000; i++) {
            statement.bindString(1,"wwwwww");
            statement.bindString(2,"aaaaaa");
            int ret = statement.execute();
            if(ret!=0){
                break;
            }
        }
        statement.commitTransaction();
        Log.e("--------------", "onCreate: "+(System.currentTimeMillis()-start) );
    }
}
