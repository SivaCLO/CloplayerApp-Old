package com.cloplayer.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MySQLiteHelper extends SQLiteOpenHelper {

	public static final String TABLE_STORIES = "stories";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_URL = "url";
	public static final String COLUMN_DOMAIN = "domain";
	public static final String COLUMN_HEADLINE = "headline";
	public static final String COLUMN_DETAIL = "detail";
	public static final String COLUMN_ITEM_COUNT = "itemcount";
	public static final String COLUMN_DOWNLOAD_PROGRESS = "dlprogress";
	public static final String COLUMN_PLAY_PROGRESS = "plprogress";
	public static final String COLUMN_STATE = "state";

	private static final String DATABASE_NAME = "cloplayer.db";
	private static final int DATABASE_VERSION = 1;

	// Database creation sql statement
	private static final String DATABASE_CREATE = "create table "
			+ TABLE_STORIES + "(" 
			+ COLUMN_ID + " integer primary key autoincrement, " 
			+ COLUMN_URL + " text not null, "
			+ COLUMN_DOMAIN + " text, "
			+ COLUMN_HEADLINE + " text, "
			+ COLUMN_DETAIL + " text, "
			+ COLUMN_ITEM_COUNT + " integer not null, "
			+ COLUMN_DOWNLOAD_PROGRESS + " integer not null, "
			+ COLUMN_PLAY_PROGRESS + " integer not null, "
			+ COLUMN_STATE + " integer not null"
			+ ");";

	public MySQLiteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(MySQLiteHelper.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_STORIES);
		onCreate(db);
	}

}
