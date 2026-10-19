package com.example.tts;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

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

    public void close() {
        if (db != null) db.close();
    }

    public void updateProgressOfBook(int id, String progress) {
        if (db != null) {
            ContentValues cv = new ContentValues();
            cv.put("progress", progress);
            db.update("books", cv, "id = ?", new String[]{String.valueOf(id)});
        }
    }
}
