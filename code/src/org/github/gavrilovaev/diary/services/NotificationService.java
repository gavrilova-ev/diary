package org.github.gavrilovaev.diary.services;

import java.lang.Thread.State;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.github.gavrilovaev.diary.MainActivity;
import org.github.gavrilovaev.diary.R;
import org.github.gavrilovaev.diary.db.DiarySQLiteOpenHelper;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

/**
 * This service regularly checks the database if it is up-to-date and informs
 * the user about insights via notifications.
 * 
 * @author Sebastian
 * 
 */
public class NotificationService extends Service implements Runnable {

	private static final int CHECK_INTERVAL = 60 * 60 * 1000;
	/**
	 * Thread that regularly checks the database for its current state.
	 */
	private Thread notificationThread;

	@Override
	public void onCreate() {
		createNotificationThread();
	}

	private void createNotificationThread() {
		this.notificationThread = new Thread(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Ensure that we have a notification thread.
		if (this.notificationThread == null
				|| this.notificationThread.getState() == State.TERMINATED) {
			createNotificationThread();
			Log.w("diary", "Needed to create new notification thread.");
		}

		// Start the notification thread if it is not already running.
		if (!this.notificationThread.isAlive()) {
			this.notificationThread.start();
			Log.w("diary", "Notification thread started.");
		}

		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		// Interrupt the thread so that it can
		if (this.notificationThread.isAlive()) {
			Log.d("diary", "Interrupting notification thread.");
			this.notificationThread.interrupt();
		}
	}

	@Override
	public void run() {
		while (true) {
			try {
				checkDatabaseRecency();
				Thread.sleep(CHECK_INTERVAL);
			} catch (InterruptedException e) {
				Log.d("diary", "Notification service has been interrupted.", e);
				return;
			}
		}

	}

	/**
	 * Checks if the database has up-to-date entries. If not, it notifies the
	 * user.
	 */
	private void checkDatabaseRecency() {
		int numDaysSinceLatestEntry = determineDaysSinceLatestEntry();
		if (numDaysSinceLatestEntry < 1) {
			return;
		}

		buildNoNewEntriesNotification(numDaysSinceLatestEntry);
	}

	/**
	 * Build a notification telling that the diary has no recent entries.
	 * 
	 * @param numDaysSinceLatestEntry
	 *            is the number of days since the latest entry
	 */
	private void buildNoNewEntriesNotification(int numDaysSinceLatestEntry) {
		String notificationText = String.format(Locale.ENGLISH,
				"%d days since the last entry", numDaysSinceLatestEntry);
		Builder builder = new NotificationCompat.Builder(this);
		builder.setContentTitle("Diary not up-to-date")
				.setContentText(notificationText)
				.setSmallIcon(R.drawable.ic_notification).setAutoCancel(true)
				.setOnlyAlertOnce(true);

		Intent startDiaryIntent = new Intent(this, MainActivity.class);
		TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(this);
		taskStackBuilder.addParentStack(MainActivity.class);
		taskStackBuilder.addNextIntent(startDiaryIntent);
		PendingIntent pendingIntent = taskStackBuilder.getPendingIntent(0,
				PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pendingIntent);
		Notification notification = builder.build();

		NotificationManager notificationService = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationService.notify(0, notification);
	}

	/**
	 * Loads the latest entry date from the database.
	 * 
	 * @return the latest DB-encoded entry date or -1 if none could be loaded
	 */
	private int determineDaysSinceLatestEntry() {
		int date = loadLatestEntryDate();

		if (date == -1) {
			return -1;
		}

		int day = date % 100;
		date /= 100;
		int month = date % 100;
		date /= 100;
		int year = date;

		// TODO: Make this more efficient.
		// Determine timespan since last entry by counting days down.
		Calendar latestEntryCalendar = new GregorianCalendar(year, month, day);
		Calendar nowCalendar = new GregorianCalendar();

		if (nowCalendar.getTimeInMillis() < latestEntryCalendar
				.getTimeInMillis()) {
			return -1;
		}

		int numDays = 0;
		while (nowCalendar.get(Calendar.DATE) != latestEntryCalendar
				.get(Calendar.DATE)
				|| nowCalendar.get(Calendar.MONTH) != latestEntryCalendar
						.get(Calendar.MONTH)
				|| nowCalendar.get(Calendar.YEAR) != latestEntryCalendar
						.get(Calendar.YEAR)) {
			nowCalendar.add(Calendar.DATE, -1);
			numDays++;
		}
		return numDays;
	}

	/**
	 * Loads the latest entry's date from the DB.
	 * 
	 * @return the latest entry's date or -1 if none could be loaded
	 */
	private int loadLatestEntryDate() {
		DiarySQLiteOpenHelper openHelper = new DiarySQLiteOpenHelper(this);
		SQLiteDatabase database = openHelper.getReadableDatabase();
		Cursor cursor = database.query("events", new String[] { "max(date)" },
				null, null, null, null, null);

		int date = -1;
		if (cursor.moveToFirst()) {
			date = cursor.getInt(0);
		}
		database.close();

		return date;
	}

}
