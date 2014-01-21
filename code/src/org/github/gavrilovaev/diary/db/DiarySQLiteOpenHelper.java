package org.github.gavrilovaev.diary.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DiarySQLiteOpenHelper extends SQLiteOpenHelper {

	public static final String DB_NAME = "diarydb";
	private static final int DB_VERSION = 1;

	public DiarySQLiteOpenHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE events (_id INTEGER PRIMARY KEY AUTOINCREMENT, date INTEGER, description TEXT, fav_type INTEGER)");

		db.execSQL("INSERT INTO events (date, description, fav_type) VALUES (100, 'text1', 0)");
		db.execSQL("INSERT INTO events (date, description, fav_type) VALUES (200, 'text2', 0)");
		db.execSQL("INSERT INTO events (date, description, fav_type) VALUES (200, 'text3', 0)");
		
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion != 0 || newVersion != DB_VERSION)
			throw new UnsupportedOperationException(
					"Cannot upgrade database from version " + oldVersion + ".");
	}
}
