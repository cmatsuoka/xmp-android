package org.helllabs.android.xmp.service;

import org.helllabs.android.xmp.preferences.Preferences;
import org.helllabs.android.xmp.service.receiver.BluetoothConnectionReceiver;
import org.helllabs.android.xmp.service.receiver.HeadsetPlugReceiver;
import org.helllabs.android.xmp.service.receiver.MediaButtonsReceiver;
import org.helllabs.android.xmp.service.receiver.NotificationActionReceiver;
import org.helllabs.android.xmp.util.Log;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.KeyEvent;

public class ReceiverHelper {
	
	private static final String TAG = "ReceiverHelper";
	private final PlayerService player;
	private HeadsetPlugReceiver headsetPlugReceiver;
	private BluetoothConnectionReceiver bluetoothConnectionReceiver;
	private MediaButtons mediaButtons;
	private final SharedPreferences prefs;
	
	// Autopause
	private boolean autoPaused;			// paused on phone call
	private boolean headsetPaused;
	
	public ReceiverHelper(final PlayerService player) {
		this.player = player;
		prefs = PreferenceManager.getDefaultSharedPreferences(player);
	}
	
	public void registerReceivers() {
		if (prefs.getBoolean(Preferences.HEADSET_PAUSE, true)) {
			Log.i(TAG, "Register headset receiver");
			// For listening to headset changes, the broadcast receiver cannot be
			// declared in the manifest, it must be dynamically registered. 
			headsetPlugReceiver = new HeadsetPlugReceiver();
			player.registerReceiver(headsetPlugReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
		}

		if (prefs.getBoolean(Preferences.BLUETOOTH_PAUSE, true)) {
			Log.i(TAG, "Register bluetooth receiver");
			bluetoothConnectionReceiver = new BluetoothConnectionReceiver();
			final IntentFilter filter = new IntentFilter();
			filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
			filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
			filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
			if (Build.VERSION.SDK_INT >= 11) {
				filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
			}
			player.registerReceiver(bluetoothConnectionReceiver, filter);
		}

		mediaButtons = new MediaButtons(player);
		mediaButtons.register();
	}
	
	
	public void unregisterReceivers() {
		if (headsetPlugReceiver != null) {
			player.unregisterReceiver(headsetPlugReceiver);
		}
		if (bluetoothConnectionReceiver != null) {		// Z933 (glaucus) needs this test
			player.unregisterReceiver(bluetoothConnectionReceiver);
		}
		if (mediaButtons != null) {
			mediaButtons.unregister();
		}
	}
	
	public void checkReceivers() {
		checkMediaButtons();
		checkHeadsetState();
		checkBluetoothState();
		checkNotificationButtons();
	}
	
	private void checkMediaButtons() {
		final int key = MediaButtonsReceiver.getKeyCode();

		if (key != MediaButtonsReceiver.NO_KEY) {
			switch (key) {
			case KeyEvent.KEYCODE_MEDIA_NEXT:
				Log.i(TAG, "Handle KEYCODE_MEDIA_NEXT");
				player.actionNext();
				break;
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
				Log.i(TAG, "Handle KEYCODE_MEDIA_PREVIOUS");
				player.actionPrev();
				break;
			case KeyEvent.KEYCODE_MEDIA_STOP:
				Log.i(TAG, "Handle KEYCODE_MEDIA_STOP");
				player.actionStop();
				break;
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				Log.i(TAG, "Handle KEYCODE_MEDIA_PLAY_PAUSE");
				player.actionPlayPause();
				headsetPaused = false;
				break;
			case KeyEvent.KEYCODE_MEDIA_PLAY:
				Log.i(TAG, "Handle KEYCODE_MEDIA_PLAY");
				if (player.isPaused()) {
					player.actionPlayPause();
				}
				break;
			case KeyEvent.KEYCODE_MEDIA_PAUSE:
				Log.i(TAG, "Handle KEYCODE_MEDIA_PAUSE");
				if (!player.isPaused()) {
					player.actionPlayPause();
					headsetPaused = false;
				}
				break;
			}

			MediaButtonsReceiver.setKeyCode(MediaButtonsReceiver.NO_KEY);
		}
	}

	private void checkNotificationButtons() {
		final int key = NotificationActionReceiver.getKeyCode();

		if (key != NotificationActionReceiver.NO_KEY) {
			switch (key) {
			case NotificationActionReceiver.STOP:
				Log.i(TAG, "Handle notification stop");
				player.actionStop();
				break;
			case NotificationActionReceiver.PAUSE:
				Log.i(TAG, "Handle notification pause");
				player.actionPlayPause();
				headsetPaused = false;
				break;
			case NotificationActionReceiver.NEXT:
				Log.i(TAG, "Handle notification next");
				player.actionNext();
				break;
			case NotificationActionReceiver.PREV:
				Log.i(TAG, "Handle notification prev");
				player.actionPrev();
				break;
			}

			NotificationActionReceiver.setKeyCode(NotificationActionReceiver.NO_KEY);
		}
	}

	private void checkHeadsetState() {
		final int state = HeadsetPlugReceiver.getState();

		if (state != HeadsetPlugReceiver.NO_STATE) {
			switch (state) {
			case HeadsetPlugReceiver.HEADSET_UNPLUGGED:
				Log.i(TAG, "Handle headset unplugged");

				// If not already paused
				if (!player.isPaused() && !autoPaused) {
					headsetPaused = true;
					player.actionPlayPause();
				} else {
					Log.i(TAG, "Already paused");
				}
				break;
			case HeadsetPlugReceiver.HEADSET_PLUGGED:
				Log.i(TAG, "Handle headset plugged");

				// If paused by headset unplug
				if (headsetPaused) {
					// Don't unpause if we're paused due to phone call
					if (!autoPaused) {
						player.actionPlayPause();
					} else {
						Log.i(TAG, "Paused by phone state, don't unpause");
					}
					headsetPaused = false;
				} else {
					Log.i(TAG, "Manual pause, don't unpause");
				}
				break;
			}

			HeadsetPlugReceiver.setState(HeadsetPlugReceiver.NO_STATE);
		}
	}

	private void checkBluetoothState() {
		final int state = BluetoothConnectionReceiver.getState();

		if (state != BluetoothConnectionReceiver.NO_STATE) {
			switch (state) {
			case BluetoothConnectionReceiver.DISCONNECTED:
				Log.i(TAG, "Handle bluetooth disconnection");

				// If not already paused
				if (!player.isPaused() && !autoPaused) {
					headsetPaused = true;
					player.actionPlayPause();
				} else {
					Log.i(TAG, "Already paused");
				}
				break;
			case BluetoothConnectionReceiver.CONNECTED:
				Log.i(TAG, "Handle bluetooth connection");

				// If paused by headset unplug
				if (headsetPaused) {
					// Don't unpause if we're paused due to phone call
					if (!autoPaused) {
						player.actionPlayPause();
					} else {
						Log.i(TAG, "Paused by phone state, don't unpause");
					}
					headsetPaused = false;
				} else {
					Log.i(TAG, "Manual pause, don't unpause");
				}
				break;
			}

			BluetoothConnectionReceiver.setState(BluetoothConnectionReceiver.NO_STATE);
		}
	}
	
	public boolean isAutoPaused() {
		return autoPaused;
	}
	
	public void setAutoPaused(final boolean autoPaused) {
		this.autoPaused = autoPaused;
	}
	
	public boolean isHeadsetPaused() {
		return headsetPaused;
	}
	
	public void setHeadsetPaused(final boolean headsetPaused) {
		this.headsetPaused = headsetPaused;
	}
}
