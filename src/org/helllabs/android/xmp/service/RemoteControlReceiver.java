package org.helllabs.android.xmp.service;

import org.helllabs.android.xmp.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;


public class RemoteControlReceiver extends BroadcastReceiver {
	private static final String TAG = "RemoteControlReceiver";
	public static final int NO_KEY = -1;
	private static int keyCode = NO_KEY;

	@Override
	public void onReceive(final Context context, final Intent intent) {
		final String action = intent.getAction();
		Log.i(TAG, "Action " + action);
		if (action.equals(Intent.ACTION_MEDIA_BUTTON)) {
			final KeyEvent event = (KeyEvent)intent.getExtras().get(Intent.EXTRA_KEY_EVENT);

			if (event.getAction() != KeyEvent.ACTION_DOWN) {
				return;
			}

			int code;
			switch (code = event.getKeyCode()) {
			case KeyEvent.KEYCODE_MEDIA_NEXT:
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
			case KeyEvent.KEYCODE_MEDIA_STOP:
			//case KeyEvent.KEYCODE_MEDIA_PAUSE:
			//case KeyEvent.KEYCODE_MEDIA_PLAY:
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				Log.i(TAG, "Key code " + code);
				keyCode = code;
				break;
			default:
				Log.i(TAG, "Unhandled key code " + code);
			}
			abortBroadcast();
		}
	}
	
	public static int getKeyCode() {
		return keyCode;
	}
	
	public static void setKeyCode(int keyCode) {
		RemoteControlReceiver.keyCode = keyCode;
	}
}