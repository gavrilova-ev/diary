package org.github.gavrilovaev.diary;

import org.github.gavrilovaev.diary.db.DiarySQLiteOpenHelper;

import android.app.Activity;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;

public class NewEntryActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new_entry);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.new_entry, menu);
		return true;
	}

	public void onClickSave(View view) {
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
}
