package org.github.gavrilovaev.diary.services;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This class gets a notification when the Android system is booted. We use this hook, 
 * to start our background services.
 * 
 * @author Sebastian
 *
 */
public class DiaryServiceStarter extends BroadcastReceiver {

	@Override
	public void onReceive(Context ctx, Intent intent) {
		Log.i("diary", String.format("Received broadcast: %s", intent));
		if (Intent.ACTION_BOOT_COMPLETED.equalsIgnoreCase(intent.getAction())) {
			startNotificationService(ctx);
		}
		
	}

	public static void startNotificationService(Context ctx) {
		Intent intent = new Intent(ctx, NotificationService.class);
		ComponentName componentName = ctx.startService(intent);
		Log.d("diary", "Tried to start service: " + componentName.toString());
	}

}
