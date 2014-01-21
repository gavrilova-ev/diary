package org.github.gavrilovaev.diary.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import android.content.Context;
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
			Toast.makeText(context, "Backup failed:\n"+e.getMessage(), Toast.LENGTH_SHORT).show();
			return;
		}
		Toast.makeText(context, "Backup successful.", Toast.LENGTH_SHORT).show();
	}

	private static void copy(File db, File backupDB) throws IOException {
		FileChannel src = null, dst = null;
		try {
			if (db.exists()) {
				src = new FileInputStream(db).getChannel();
				dst = new FileOutputStream(backupDB).getChannel();
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
