package org.helllabs.android.xmp.service.utils;

import org.helllabs.android.xmp.service.receiver.MediaButtonsReceiver;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.RemoteControlClient;
import android.os.Build;

public class RemoteControl {

	private static final String TAG = "RemoteControl";
	private final ComponentName remoteControlReceiver;
	private RemoteControlClientCompat remoteControlClient;
	private final AudioManager audioManager;

	public RemoteControl(final Context context, final AudioManager audioManager) {
		this.audioManager = audioManager;

		remoteControlReceiver = new ComponentName(context.getPackageName(), MediaButtonsReceiver.class.getName());

		if (remoteControlClient == null) {
			final Intent remoteControlIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
			remoteControlIntent.setComponent(remoteControlReceiver);

			remoteControlClient = new RemoteControlClientCompat(PendingIntent.getBroadcast(context, 0, remoteControlIntent, 0));

			if (Build.VERSION.SDK_INT >= 14) {
				remoteControlClient.setTransportControlFlags(
						RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
						RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
						RemoteControlClient.FLAG_KEY_MEDIA_REWIND |
						RemoteControlClient.FLAG_KEY_MEDIA_FAST_FORWARD |
						RemoteControlClient.FLAG_KEY_MEDIA_STOP);
			}

			RemoteControlHelper.registerRemoteControlClient(audioManager, remoteControlClient);
			audioManager.registerMediaButtonEventReceiver(remoteControlReceiver);
		}
	}

	public void unregisterReceiver() {
		audioManager.unregisterMediaButtonEventReceiver(remoteControlReceiver);
	}
}
