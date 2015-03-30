package com.kes;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by gadza on 2015.03.12..
 */
class KesDB extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DB_NAME = "kesdb";
    private static final String TABLE_PENDING = "pending";
    private static final String KEY_ID = "id";
    private static final String KEY_QUESTION = "question";
    private static final String KEY_PICTURE = "picture";
    private static final String KEY_FAILED = "failed";

    private Context mContext;

    public KesDB(Context context)
    {
        super(context, DB_NAME, null, DATABASE_VERSION);
    }

    public void addPendingQuestion(String question, String picture)
    {
        ContentValues values = new ContentValues();
        values.put(KEY_QUESTION, question);
        values.put(KEY_PICTURE, picture);
        getWritableDatabase().insertOrThrow(TABLE_PENDING, null, values);
    }

    public void close()
    {
        getWritableDatabase().close();
    }

    private static final String CREATE_TABLE_PENDING = "CREATE TABLE "
            + TABLE_PENDING + "(" + KEY_ID + " INTEGER PRIMARY KEY," + KEY_QUESTION
            + " TEXT," + KEY_PICTURE + " STRING," + KEY_FAILED
            + " INTEGER" + ")";

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_PENDING);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public long addPending(String question, String picture)
    {
      ContentValues values = new ContentValues();
      values.put(KEY_QUESTION, question);
      values.put(KEY_PICTURE, picture);
      values.put(KEY_FAILED, 0);
      return getWritableDatabase().insert(TABLE_PENDING,null,values);
    }

    public void updatePending(long id, boolean success)
    {
        if (success)
            getWritableDatabase().delete(TABLE_PENDING, KEY_ID + " = ?",
                    new String[] { Long.toString(id) });
        else
        {
            ContentValues values = new ContentValues();
            values.put(KEY_FAILED, 1);
            getWritableDatabase().update(TABLE_PENDING, values, KEY_ID + " = ?",
                    new String[] { Long.toString(id) });
        }
    }

    /*
    public void getAllPending(List<PendingQuestion> dest) {
        String selectQuery = "SELECT * FROM " + TABLE_PENDING;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        int col_id = c.getColumnIndex(KEY_ID);
        int col_question = c.getColumnIndex(KEY_QUESTION);
        int col_picture = c.getColumnIndex(KEY_PICTURE);
        int col_failed = c.getColumnIndex(KEY_FAILED);

        if (c.moveToFirst()) {
            do {
                PendingQuestion pq = new PendingQuestion();
                pq.id = c.getLong(col_id);
                pq.question = c.getString(col_question);
                pq.picturePath = c.getString(col_picture);
                pq.failed = c.getInt(col_failed) != 0;
                dest.add(pq);
            } while (c.moveToNext());
        }
        c.close();
    }
    */

}
