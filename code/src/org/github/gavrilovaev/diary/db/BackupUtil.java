package org.github.gavrilovaev.diary.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class BackupUtil {

	public static void backupDataToSD(Context context) {
		String sdState = Environment.getExternalStorageState();
		if (!Environment.MEDIA_MOUNTED.equals(sdState)) {
			Toast.makeText(context, "SD card not accessible.",
					Toast.LENGTH_SHORT).show();
			return;
		}

		File sd = Environment.getExternalStorageDirectory();
		File db = context.getDatabasePath(DiarySQLiteOpenHelper.DB_NAME);
		String backupDBPath = "diary-backup-" + System.currentTimeMillis();
		File backupDB = new File(sd, backupDBPath);

		try {
			copy(db, backupDB);
		} catch (IOException e) {
			Toast.makeText(context, "Backup failed:\n" + e.getMessage(),
					Toast.LENGTH_SHORT).show();
			return;
		}
		Toast.makeText(context, "Backup successful.", Toast.LENGTH_SHORT)
				.show();
	}

	/**
	 * Identifies the latest backup file on the SD card an loads it into the app
	 * directory.
	 */
	public static void loadBackup(final Context context) {
		String sdState = Environment.getExternalStorageState();
		if (!Environment.MEDIA_MOUNTED.equals(sdState)) {
			Toast.makeText(context, "SD card not accessible.",
					Toast.LENGTH_SHORT).show();
			return;
		}

		File sd = Environment.getExternalStorageDirectory();
		Pattern backupFilePattern = Pattern.compile("diary-backup-(\\d+)");
		long latestBackupTimestamp = 0l;
		File latestBackupFile = null;
		for (File potentialBackupFile : sd.listFiles()) {
			Matcher matcher = backupFilePattern.matcher(potentialBackupFile
					.getName());
			if (matcher.matches()) {
				long timestamp = Long.parseLong(matcher.group(1));
				if (latestBackupTimestamp < timestamp) {
					latestBackupTimestamp = timestamp;
					latestBackupFile = potentialBackupFile;
				}
			}
		}

		if (latestBackupFile == null) {
			Toast.makeText(context, "Could not find a backup file.",
					Toast.LENGTH_SHORT);
			return;
		}

		final File finalBackupFile = latestBackupFile;

		DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					try {
						copy(finalBackupFile,
								context.getDatabasePath(DiarySQLiteOpenHelper.DB_NAME));
					} catch (IOException e) {
						Toast.makeText(context,
								"Backup failed:\n" + e.getMessage(),
								Toast.LENGTH_SHORT).show();
						return;
					}
					Toast.makeText(context, "Backup successful.",
							Toast.LENGTH_SHORT).show();
				}

			}
		};
		String dialogMsg = String.format("Latest backup from %s, use that?",
				new Date(latestBackupTimestamp));
		new AlertDialog.Builder(context).setMessage(dialogMsg)
				.setPositiveButton("Yes", onClickListener)
				.setNegativeButton("No", onClickListener).show();

	}

	private static void copy(File srcFile, File targetFile) throws IOException {
		FileChannel src = null, dst = null;
		try {
			if (srcFile.exists()
					&& (!targetFile.exists() || targetFile.delete())) {
				src = new FileInputStream(srcFile).getChannel();
				dst = new FileOutputStream(targetFile).getChannel();
				dst.transferFrom(src, 0, src.size());
				src.close();
				dst.close();
			}
		} finally {
			try {
				if (src != null)
					src.close();
			} catch (IOException e) {
				Log.e("DIARY", "Failed to close resource.", e);
			}
			try {
				if (dst != null)
					src.close();
			} catch (IOException e) {
				Log.e("DIARY", "Failed to close resource.", e);
			}
		}
	}

}
