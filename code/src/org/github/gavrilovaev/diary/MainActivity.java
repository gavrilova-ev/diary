package org.github.gavrilovaev.diary;

import java.util.Calendar;

import org.github.gavrilovaev.diary.db.BackupUtil;
import org.github.gavrilovaev.diary.db.DiarySQLiteOpenHelper;
import org.github.gavrilovaev.diary.util.ActionBarListActivity;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ActionBarListActivity {

	public static final String MIN_FAVORITE_TYPE = "minFavoriteType";
	public static final int[] FAVORITE_ICON_IDS = { R.drawable.star_none_2,
			R.drawable.star_bronze_2, R.drawable.star_silver_3,
			R.drawable.star_gold_2 };
	private SQLiteDatabase db;
	private int minFavoriteType = 0;
	private ActionBarDrawerToggle drawerToggle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle extras = getIntent().getExtras();
		if (extras != null && extras.containsKey(MIN_FAVORITE_TYPE)) {
			minFavoriteType = extras.getInt(MIN_FAVORITE_TYPE);
		}

		setContentView(R.layout.activity_main);

		String[] navigationLevels = { "Days", "Weeks", "Months", "Years" };
		ListView drawerList = (ListView) findViewById(R.id.left_drawer);
		drawerList.setAdapter(new ArrayAdapter<String>(this,
				R.layout.drawer_row, R.id.item_text, navigationLevels));
		DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		final CharSequence title = getTitle();
		drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
				R.drawable.ic_drawer, 0, 0) {

			/** Called when a drawer has settled in a completely closed state. */
			public void onDrawerClosed(View view) {
				super.onDrawerClosed(view);
				getSupportActionBar().setTitle(title);
			}

			/** Called when a drawer has settled in a completely open state. */
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				getSupportActionBar().setTitle("Navigation");
			}
		};

		// Set the drawer toggle as the DrawerListener
		drawerLayout.setDrawerListener(drawerToggle);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		// Set the list's click listener
		drawerList.setOnItemClickListener(new DrawerItemClickListener());
		setListAdapter(new DiaryCursorAdapter(this, getFreshCursor()));
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		drawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		drawerToggle.onConfigurationChanged(newConfig);
	}

	private Cursor getFreshCursor() {
		ensureDatabaseOpen();
		Cursor cursor = db.query(false, "events", new String[] { "date",
				"description", "fav_type", "_id" }, "fav_type >= ?",
				new String[] { String.valueOf(minFavoriteType) }, null, null,
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
		getMenuInflater().inflate(R.menu.activity_main_actions, menu);
		return super.onCreateOptionsMenu(menu);
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
	public boolean onOptionsItemSelected(MenuItem item) {
		if (drawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		switch (item.getItemId()) {
		case R.id.action_add:
			Intent intent = new Intent(this, NewEntryActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_backup:
			BackupUtil.backupDataToSD(this);
			return true;
		case R.id.action_remove_all:
			return removeAll();
		case R.id.action_week_view:
			changeMinFavoriteType(0);
			return true;
		case R.id.action_month_view:
			changeMinFavoriteType(1);
			return true;
		case R.id.action_year_view:
			changeMinFavoriteType(2);
			return true;
		case R.id.action_all_times_view:
			changeMinFavoriteType(3);
			return true;
		}
		return false;

	}

	private boolean removeAll() {
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
		return true;
	}

	private void changeMinFavoriteType(int newMinFavoriteType) {
		Intent intent = new Intent(this, getClass());
		intent.putExtra(MIN_FAVORITE_TYPE, newMinFavoriteType);
		startActivity(intent);
		finish();
	}

	protected void removeAllEntries() {
		ensureDatabaseOpen();
		db.execSQL("DELETE FROM events");
		refreshCursor();
		getListView().invalidate();
	}

	public void onClickFavorite(View view) {
		if (minFavoriteType > 2) {
			return;
		}

		int position = getListView().getPositionForView(view);
		long id = getListAdapter().getItemId(position);
		int curFavType = (Integer) view.getTag();
		int newFavType;
		if (curFavType == minFavoriteType) {
			newFavType = minFavoriteType + 1;
		} else if (curFavType == minFavoriteType + 1) {
			newFavType = minFavoriteType;
		} else {
			Toast.makeText(this, "This favorite type is too great to modify.",
					Toast.LENGTH_SHORT).show();
			return;
		}
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
		favoriteImage.setImageResource(FAVORITE_ICON_IDS[favType]);
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
					daysOfWeek[dayOfWeek - 1], year, month + 1, day));
			TextView text2 = (TextView) view
					.findViewById(R.id.description_text);
			text2.setText(cursor.getString(1));

			ImageView favoriteImage = (ImageView) view
					.findViewById(R.id.favorite_image);
			int favType = cursor.getInt(2);
			setFavImage(favoriteImage, favType);
		}

	}

	private class DrawerItemClickListener implements
			ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView parent, View view, int position,
				long id) {
			changeMinFavoriteType(position);
		}
	}

}
