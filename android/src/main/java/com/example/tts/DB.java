package com.example.tts;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;

public class DB {

    SQLiteDatabase db;

    public DB(Context context) {
        String path = context.getApplicationInfo().dataDir + "/databases/authors.db";
        File file = new File(path);
        if (file.exists()) {
            db = SQLiteDatabase.openDatabase(
                    path,
                    null,
                    0
            );
        }
    }

    public void updateProgressOfBook(int id, String progress) {
        if (db != null) {
            db.execSQL("UPDATE books SET progress = " + progress + " WHERE id = " + id);
        }
    }
}
