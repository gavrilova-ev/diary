package org.github.gavrilovaev.diary;

import org.github.gavrilovaev.diary.db.BackupUtil;
import org.github.gavrilovaev.diary.db.DiarySQLiteOpenHelper;
import org.github.gavrilovaev.diary.fragments.CardFragment;

import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class CardActivity extends ActionBarActivity {

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
		
		Log.i("diary", "Creating " + this.getClass().getSimpleName());

		Bundle extras = getIntent().getExtras();
		if (extras != null && extras.containsKey(MIN_FAVORITE_TYPE)) {
			minFavoriteType = extras.getInt(MIN_FAVORITE_TYPE);
		}

		setContentView(R.layout.card_activity);
		initializeNavigationDrawer();

		// Initialize drop-down navigation
//		OnNavigationListener navigationListener = new NavigationSpinnerListener();
//		ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this,
//				R.layout.card_activity_dropdown_item, 
//				R.id.card_activity_dropdown_item_text,
//				Arrays.asList("Days", "Weeks", "Months", "Years"));
//		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//		ActionBar actionBar = getSupportActionBar();
//		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
//		actionBar
//				.setListNavigationCallbacks(spinnerAdapter, navigationListener);
	}

	private void initializeNavigationDrawer() {
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
				"group_concat(description, '#~#~#') AS descriptions",
				"min(_id) AS _id" }, "fav_type >= ?",
				new String[] { String.valueOf(minFavoriteType) }, "date", null,
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
		case R.id.action_restore:
			Toast.makeText(this, "Restore dem backup.", Toast.LENGTH_SHORT).show();
			BackupUtil.loadBackup(this);
			return true;
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


	private void changeMinFavoriteType(int newMinFavoriteType) {
		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction transaction = fragmentManager.beginTransaction();
		Fragment newCardFragment = CardFragment.newInstance(newMinFavoriteType);
//		transaction.detach(fragmentManager.findFragmentById(R.id.content_fragment));
//		transaction.attach(newEntryListFragment);
		int oldMinFavoriteType = 0;
		Fragment oldFragment = fragmentManager.findFragmentById(R.id.content_fragment);
		if (oldFragment != null && oldFragment instanceof CardFragment) {
			CardFragment oldCardFragment = (CardFragment) oldFragment;
			oldMinFavoriteType = oldCardFragment.getMinFavoriteType();
		}
		if (oldMinFavoriteType < newMinFavoriteType) {
			transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
		} else if (oldMinFavoriteType > newMinFavoriteType) {
			transaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
		}
		transaction.replace(R.id.content_fragment, newCardFragment);
//		transaction.addToBackStack(null);
//		transaction.detach(fragmentManager.findFragmentById(R.id.content_fragment));
		transaction.commit();
	}


	private class DrawerItemClickListener implements
			ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			changeMinFavoriteType(position);
		}
	}
	
	private class NavigationSpinnerListener implements ActionBar.OnNavigationListener {

		@Override
		public boolean onNavigationItemSelected(int itemPosition, long itemId) {
			if (itemPosition != minFavoriteType) {
				changeMinFavoriteType(itemPosition);
			}
			return true;
		}
		
	}

}
