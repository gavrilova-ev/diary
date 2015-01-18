package org.github.gavrilovaev.diary;

import org.github.gavrilovaev.diary.db.BackupUtil;
import org.github.gavrilovaev.diary.fragments.EntryListFragment;
import org.github.gavrilovaev.diary.services.NotificationService;

import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;


public class MainActivity extends ActionBarActivity {

	private ActionBarDrawerToggle drawerToggle;
	private ListView drawerList;
	private DrawerLayout drawerLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.i("diary", "Creating " + this.getClass().getSimpleName());

		setContentView(R.layout.activity_main);

		String[] navigationLevels = { "Days", "Weeks", "Months", "Years",
				"Card style" };
		drawerList = (ListView) findViewById(R.id.left_drawer);
		drawerList.setAdapter(new ArrayAdapter<String>(this,
				R.layout.drawer_row, R.id.item_text, navigationLevels));
		drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
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

		ComponentName componentName = startService(new Intent(this, NotificationService.class));
		Log.d("diary", "Tried to start service: " + componentName.toString());
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


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (drawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		switch (item.getItemId()) {
		case R.id.action_backup:
			BackupUtil.backupDataToSD(this);
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
		Fragment newEntryListFragment = EntryListFragment.newInstance(newMinFavoriteType);
		transaction.replace(R.id.content_fragment, newEntryListFragment);
		transaction.setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
//		transaction.addToBackStack(null);
//		transaction.detach(fragmentManager.findFragmentById(R.id.content_fragment));
		transaction.commit();
	}

	private class DrawerItemClickListener implements
			ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			if (position == 4) {
				Intent intent = new Intent(MainActivity.this,
						CardActivity.class);
				intent.putExtra(CardActivity.MIN_FAVORITE_TYPE, 0);
				startActivity(intent);
				finish();
			} else {
				changeMinFavoriteType(position);
			}
			
			drawerList.setItemChecked(position, true);
			// setTitle(...);
			drawerLayout.closeDrawer(drawerList);
		}
	}

}
