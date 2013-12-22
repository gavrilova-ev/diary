package org.github.gavrilovaev.diary;

import java.util.Calendar;

import org.github.gavrilovaev.diary.db.DiarySQLiteOpenHelper;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends ListActivity {

	private SQLiteDatabase db;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		setListAdapter(new DiaryCursorAdapter(this, getFreshCursor()));
	}

	private Cursor getFreshCursor() {
		ensureDatabaseOpen();
		Cursor cursor = db.query(false, "events", new String[] { "date",
				"description", "fav_type", "_id" }, null, null, null, null,
				"date desc", null);
		return cursor;
	}

	private void ensureDatabaseOpen() {
		if (db == null || !db.isOpen()) {
			DiarySQLiteOpenHelper helper = new DiarySQLiteOpenHelper(this);
			db = helper.getWritableDatabase();
		}
	}

	private void ensureDatabaseClosed() {
		if (db != null && db.isOpen()) {
			db.close();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		refreshCursor();
		getListView().invalidate();
	}

	private void refreshCursor() {
		DiaryCursorAdapter adapter = (DiaryCursorAdapter) getListAdapter();
		Cursor oldCursor = adapter.getCursor();
		adapter.changeCursor(getFreshCursor());
		oldCursor.close();
	}

	@Override
	protected void onStop() {
		super.onStop();
		ensureDatabaseClosed();
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_add:
			Intent intent = new Intent(this, NewEntryActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_remove_all:
			DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (which == DialogInterface.BUTTON_POSITIVE) {
						MainActivity.this.removeAllEntries();
					}

				}
			};
			new AlertDialog.Builder(this).setMessage("Are you sure?")
					.setPositiveButton("Yes", onClickListener)
					.setNegativeButton("No", onClickListener).show();
		}
		return false;

	}

	protected void removeAllEntries() {
		ensureDatabaseOpen();
		db.execSQL("DELETE FROM events");
		refreshCursor();
		getListView().invalidate();
	}

	public void onClickFavorite(View view) {
		int position = getListView().getPositionForView(view);
		long id = getListAdapter().getItemId(position);
		int curFavType = (Integer) view.getTag();
		int newFavType = curFavType == 0 ? 1 : 0;
		ensureDatabaseOpen();
		ContentValues values = new ContentValues(1);
		values.put("fav_type", newFavType);
		int affectedRows = db.update("events", values, "_id = " + id, null);
		if (affectedRows >= 1) {
			setFavImage((ImageView) view, newFavType);
			refreshCursor();
		}

	}

	private void setFavImage(ImageView favoriteImage, int favType) {
		favoriteImage.setImageResource(favType > 0 ? R.drawable.star_on
				: R.drawable.star_off);
		favoriteImage.setTag(Integer.valueOf(favType));
	}

	private class DiaryCursorAdapter extends CursorAdapter {

		public DiaryCursorAdapter(Context context, Cursor c) {
			super(context, c, true);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			LayoutInflater inflater = LayoutInflater.from(context);
			View view = inflater.inflate(R.layout.activity_main_row, null);
			bindView(view, context, cursor);
			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			TextView text1 = (TextView) view.findViewById(R.id.date_text);
			int date = cursor.getInt(0);
			int day = date % 100;
			date /= 100;
			int month = date % 100;
			date /= 100;
			int year = date;

			Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.DATE, day);
			calendar.set(Calendar.MONTH, month);
			calendar.set(Calendar.YEAR, year);
			int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
			String[] daysOfWeek = getResources().getStringArray(
					R.array.days_of_week);
			text1.setText(String.format("%s, %04d-%02d-%02d",
					daysOfWeek[dayOfWeek - 1], year, month, day));
			TextView text2 = (TextView) view
					.findViewById(R.id.description_text);
			text2.setText(cursor.getString(1));

			ImageView favoriteImage = (ImageView) view
					.findViewById(R.id.favorite_image);
			int favType = cursor.getInt(2);
			setFavImage(favoriteImage, favType);
		}

	}

}
