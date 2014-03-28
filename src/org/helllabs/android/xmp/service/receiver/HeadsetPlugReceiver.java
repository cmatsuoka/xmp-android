package org.helllabs.android.xmp.service.receiver;

import org.helllabs.android.xmp.util.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class HeadsetPlugReceiver extends BroadcastReceiver {
	private static final String TAG = "HeadsetPluginReceiver";
	public static final int HEADSET_UNPLUGGED = 0;
	public static final int HEADSET_PLUGGED = 1;
	public static final int NO_STATE = -1;
	private static int state = NO_STATE;

	@Override
	public void onReceive(final Context context, final Intent intent) {
		final String action = intent.getAction();
		Log.i(TAG, "Action " + action);
		
		if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
			final int headsetState = intent.getIntExtra("state", -1);
			if (headsetState == 0) {
				Log.i(TAG, "Headset unplugged");
				state = HEADSET_UNPLUGGED;
			} else if (headsetState == 1) {
				Log.i(TAG, "Headset plugged");
				state = HEADSET_PLUGGED;
			}
		}
	}
	
	public static int getState() {
		return state;
	}
	
	public static void setState(final int state) {
		HeadsetPlugReceiver.state = state;
	}

}
