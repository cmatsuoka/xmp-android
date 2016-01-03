package org.helllabs.android.xmp.service.receiver;

import org.helllabs.android.xmp.service.notifier.Notifier;
import org.helllabs.android.xmp.util.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class NotificationActionReceiver extends BroadcastReceiver {
	private static final String TAG = "NotificationActionReceiver";
	public static final int NO_KEY = -1;
	public static final int STOP = 1;
	public static final int PAUSE = 2;
	public static final int NEXT = 3;
	public static final int PREV = 4;
	private static int keyCode = NO_KEY;

	@Override
	public void onReceive(final Context context, final Intent intent) {
		final String action = intent.getAction();
		Log.i(TAG, "Action " + action);

		switch (action) {
			case Notifier.ACTION_STOP:
				keyCode = STOP;
				break;
			case Notifier.ACTION_PAUSE:
				keyCode = PAUSE;
				break;
			case Notifier.ACTION_NEXT:
				keyCode = NEXT;
				break;
			case Notifier.ACTION_PREV:
				keyCode = PREV;
				break;
		}
	}
	
	public static int getKeyCode() {
		return keyCode;
	}
	
	public static void setKeyCode(final int keyCode) {
		NotificationActionReceiver.keyCode = keyCode;
	}

}
