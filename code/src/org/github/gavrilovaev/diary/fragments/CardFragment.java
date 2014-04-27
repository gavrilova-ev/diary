package org.github.gavrilovaev.diary.fragments;

import java.util.Calendar;

import org.github.gavrilovaev.diary.NewEntryActivity;
import org.github.gavrilovaev.diary.R;
import org.github.gavrilovaev.diary.db.DiarySQLiteOpenHelper;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class CardFragment extends ListFragment {

	public static final String MIN_FAVORITE_TYPE = "minFavoriteType";
	public static final int[] FAVORITE_ICON_IDS = { R.drawable.star_none_2,
			R.drawable.star_bronze_2, R.drawable.star_silver_3,
			R.drawable.star_gold_2 };
	private SQLiteDatabase db;
	private int minFavoriteType = 0;
	private View rootView = null;

	public static CardFragment newInstance(int minFavoriteType) {
		CardFragment newInstance = new CardFragment();

		Bundle args = new Bundle();
		args.putInt(MIN_FAVORITE_TYPE, minFavoriteType);
		newInstance.setArguments(args);
		
		return newInstance;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = getArguments();
		if (args != null && args.containsKey(MIN_FAVORITE_TYPE)) {
			minFavoriteType = args.getInt(MIN_FAVORITE_TYPE);
		}

		setListAdapter(new DiaryCursorAdapter(getActivity(), getFreshCursor()));
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		this.rootView = inflater.inflate(R.layout.activity_card, container, false);
		return this.rootView;
	}

	@Override
	public void onDestroyView() {
		this.rootView = null;
		super.onDestroyView();
	}

	private Cursor getFreshCursor() {
		ensureDatabaseOpen();
		Cursor cursor = db.query(false, "events", new String[] { "date",
				"group_concat(description, '#~#~#') AS descriptions",
				"min(_id) AS _id" }, "fav_type >= ?",
				new String[] { String.valueOf(minFavoriteType) }, "date", null,
				"date desc", null);
		return cursor;
	}

	private void ensureDatabaseOpen() {
		if (db == null || !db.isOpen()) {
			DiarySQLiteOpenHelper helper = new DiarySQLiteOpenHelper(getActivity());
			db = helper.getWritableDatabase();
		}
	}

	private void ensureDatabaseClosed() {
		if (db != null && db.isOpen()) {
			db.close();
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.entry_list_fragment_actions, menu);
	}

	@Override
	public void onStart() {
		super.onStart();
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
	public void onStop() {
		super.onStop();
		ensureDatabaseClosed();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_add:
			Intent intent = new Intent(getActivity(), NewEntryActivity.class);
			startActivity(intent);
			return true;

		default:
			return false;
		}
	}

//	private boolean removeAll() {
//		DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
//
//			@Override
//			public void onClick(DialogInterface dialog, int which) {
//				if (which == DialogInterface.BUTTON_POSITIVE) {
//					CardFragment.this.removeAllEntries();
//				}
//
//			}
//		};
//		new AlertDialog.Builder(this).setMessage("Are you sure?")
//				.setPositiveButton("Yes", onClickListener)
//				.setNegativeButton("No", onClickListener).show();
//		return true;
//	}
//
//
//	protected void removeAllEntries() {
//		ensureDatabaseOpen();
//		db.execSQL("DELETE FROM events");
//		refreshCursor();
//		getListView().invalidate();
//	}

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
			Toast.makeText(getActivity(), "This favorite type is too great to modify.",
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
			View view = inflater.inflate(R.layout.activity_card_card, null);
			bindView(view, context, cursor);
			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
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

			TextView dateText = (TextView) view.findViewById(R.id.date_text);
			dateText.setText(String.format("%s, %04d-%02d-%02d",
					daysOfWeek[dayOfWeek - 1], year, month + 1, day));
			
			TextView descriptionsText = (TextView) view
					.findViewById(R.id.content_text);
			String descriptionsAggregate = cursor.getString(1);
			String[] descriptions = descriptionsAggregate.split("#~#~#");
			String multiLineDescription = "";
			for (String description : descriptions) {
				multiLineDescription += "- "+description+"\n";
			}
			multiLineDescription = multiLineDescription.substring(0, multiLineDescription.length() - 1);
			descriptionsText.setText(multiLineDescription);

			// ImageView favoriteImage = (ImageView) view
			// .findViewById(R.id.favorite_image);
			// int favType = cursor.getInt(2);
			// setFavImage(favoriteImage, favType);
		}

	}

}
