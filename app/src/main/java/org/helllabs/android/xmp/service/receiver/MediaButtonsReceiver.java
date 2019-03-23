package org.helllabs.android.xmp.service.receiver;

import org.helllabs.android.xmp.util.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class MediaButtonsReceiver extends BroadcastReceiver {
	private static final String TAG = "MediaButtonsReceiver";
	public static final int NO_KEY = -1;
	private static int keyCode = NO_KEY;
	private static long smartKeyPressMaxDelayMs = 500;
	protected boolean ordered = true;
	protected static final Semaphore smartButtonSem = new Semaphore(0);

	protected static Thread smartKeyThread = new Thread(new Runnable() {
		public void run() {
			Log.i(TAG, "Started smart button thread");
			try {
				while (true) {
					smartButtonSem.acquire();
					int pressCount = 1;
					Log.i(TAG, "First smart button press, waiting next presses...");
					while (smartButtonSem.tryAcquire(smartKeyPressMaxDelayMs, TimeUnit.MILLISECONDS)) {
						pressCount++;
						Log.i(TAG, "Smart button press #" + pressCount + ", waiting next presses...");
					}
					Log.i(TAG, "Total smart button key presses: " + pressCount);
					switch (pressCount) {
						case 1: //play/pause
							keyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
							break;
						case 2: // next
							keyCode = KeyEvent.KEYCODE_MEDIA_NEXT;
							break;
						default: // previous
							keyCode = KeyEvent.KEYCODE_MEDIA_PREVIOUS;
							break;
					}
				}
			} catch (InterruptedException ex) {
				Log.e(TAG, ex.getMessage());
			}
		}
	});

	public MediaButtonsReceiver() {
		if (!smartKeyThread.isAlive()) {
			smartKeyThread.start();
		}
	}

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
			switch (code = event.getKeyCode()) {	// NOPMD
			case KeyEvent.KEYCODE_MEDIA_NEXT:
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
			case KeyEvent.KEYCODE_MEDIA_STOP:
			case KeyEvent.KEYCODE_MEDIA_PAUSE:
			case KeyEvent.KEYCODE_MEDIA_PLAY:
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				Log.i(TAG, "Key code " + code);
				keyCode = code;
				break;
			case KeyEvent.KEYCODE_HEADSETHOOK:
				Log.i(TAG, "Smart key pressed");
				smartButtonSem.release();
				break;
			default:
				Log.i(TAG, "Unhandled key code " + code);
			}
			
			if (ordered) {
				abortBroadcast();
			}
		}
	}
	
	public static int getKeyCode() {
		return keyCode;
	}
	
	public static void setKeyCode(final int keyCode) {
		MediaButtonsReceiver.keyCode = keyCode;
	}
}