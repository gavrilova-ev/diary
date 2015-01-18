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
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class EntryListFragment extends ListFragment {

	public static final String MIN_FAVORITE_TYPE = "minFavoriteType";
	public static final int[] FAVORITE_ICON_IDS = { R.drawable.star_none_2,
			R.drawable.star_bronze_2, R.drawable.star_silver_3,
			R.drawable.star_gold_2 };
	private SQLiteDatabase db;
	private int minFavoriteType = 0;
	private View rootView = null;
	
    public static EntryListFragment newInstance(int minFavoriteType) {
    	EntryListFragment fragment = new EntryListFragment();
        
    	Bundle args = new Bundle();
        args.putInt(MIN_FAVORITE_TYPE, minFavoriteType);
        fragment.setArguments(args);
        
        return fragment;
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.i("diary", "Creating " + this.getClass().getSimpleName());
		
		Bundle arguments = getArguments();
		if (arguments != null && arguments.containsKey(MIN_FAVORITE_TYPE)) {
			minFavoriteType = arguments.getInt(MIN_FAVORITE_TYPE);
		}
		
		setHasOptionsMenu(true);
	}
	
//	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//		if (this.rootView == null) {
//			this.rootView = inflater.inflate(R.layout.entry_list_fragment, container);
//		}
//		return this.rootView;
//	}
	
	public void onViewCreated(View view, Bundle savedInstanceState) {
		setListAdapter(new DiaryCursorAdapter(getActivity(), getFreshCursor()));
		registerForContextMenu(getListView());
		
	}

	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.entry_list_fragment_actions, menu);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		if (v.getId() == android.R.id.list) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			// menu.setHeaderTitle(info.position);
			String[] menuItems = { "Delete" };
			for (int i = 0; i < menuItems.length; i++) {
				menu.add(Menu.NONE, i, i, menuItems[i]);
			}
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		refreshCursor();
		getListView().invalidate();
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

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getMenuInfo() instanceof AdapterView.AdapterContextMenuInfo) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
					.getMenuInfo();
			int menuItemIndex = item.getItemId();
			if (menuItemIndex == 0) {
				int numAffectedRows = db.delete("events", "_id = ?",
						new String[] { String.valueOf(info.id) });
				if (numAffectedRows == 1) {
					Toast.makeText(getActivity(), "Entry deleted.",
							Toast.LENGTH_SHORT).show();
					refreshCursor();
				} else {
					Toast.makeText(getActivity(), "Could not delete entry.",
							Toast.LENGTH_SHORT).show();
				}
				return true;
			}
		}
		return super.onContextItemSelected(item);
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
	
//	@Override
//	public void onDestroyView() {
//		super.onDestroyView();
//
//		ViewGroup parentViewGroup = (ViewGroup) this.rootView.getParent();
//		if (parentViewGroup != null) {
//			parentViewGroup.removeView(this.rootView);
//		}
//	}
	
	
	@Override
	public void onDestroyOptionsMenu() {
		super.onDestroyOptionsMenu();
		
	}
	
	@Override
	public void onStop() {
		super.onStop();
		ensureDatabaseClosed();
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
			DiarySQLiteOpenHelper helper = new DiarySQLiteOpenHelper(
					getActivity());
			db = helper.getWritableDatabase();
		}
	}

	private void ensureDatabaseClosed() {
		if (db != null && db.isOpen()) {
			db.close();
		}
	}

	private void refreshCursor() {
		DiaryCursorAdapter adapter = (DiaryCursorAdapter) getListAdapter();
		Cursor oldCursor = adapter.getCursor();
		adapter.changeCursor(getFreshCursor());
		oldCursor.close();
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

}
