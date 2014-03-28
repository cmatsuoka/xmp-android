package org.helllabs.android.xmp.service.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class HeadsetPlugReceiver extends BroadcastReceiver {
	private static final String TAG = "HeadsetPluginReceiver";

	@Override
	public void onReceive(final Context context, final Intent intent) {
		final String action = intent.getAction();
		Log.i(TAG, "Action " + action);
		
		if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
			final int state = intent.getIntExtra("state", -1);
			if (state == 0) {
				Log.i(TAG, "Headset unplugged");
			} else if (state == 1) {
				Log.i(TAG, "Headset plugged");
			}
		}
		
	}

}
