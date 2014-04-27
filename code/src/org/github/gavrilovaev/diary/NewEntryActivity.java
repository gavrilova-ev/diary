package org.github.gavrilovaev.diary;

import org.github.gavrilovaev.diary.R;
import org.github.gavrilovaev.diary.db.DiarySQLiteOpenHelper;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.DatePicker;
import android.widget.EditText;

public class NewEntryActivity extends ActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new_entry);
		
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public Intent getSupportParentActivityIntent() {
		Intent parentActivityIntent = new Intent(this, MainActivity.class);
		return parentActivityIntent;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_new_entry_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	private void save() {
		DatePicker datePicker = (DatePicker) getWindow().findViewById(
				R.id.eventDatePicker);
		int dayOfMonth = datePicker.getDayOfMonth();
		int month = datePicker.getMonth();
		int year = datePicker.getYear();
		EditText editText = (EditText) getWindow().findViewById(
				R.id.eventEditText);
		String description = editText.getText().toString();
		
		DiarySQLiteOpenHelper helper = new DiarySQLiteOpenHelper(this);
		SQLiteDatabase db = helper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("date", Integer.valueOf(String.format("%04d%02d%02d", year, month, dayOfMonth)));
		values.put("description", description);
		values.put("fav_type", 0);
		db.insert("events", null, values);
		
		finish();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_done:
			save();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
