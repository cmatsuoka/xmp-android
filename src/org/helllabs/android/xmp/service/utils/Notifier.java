package org.helllabs.android.xmp.service.utils;

import java.util.Locale;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.player.PlayerActivity;
import org.helllabs.android.xmp.service.receiver.NotificationActionReceiver;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;


public class Notifier {
    private final PendingIntent contentIntent;
    private static final int NOTIFY_ID = R.layout.player;
    public static final String ACTION_STOP = "org.helllabs.android.xmp.STOP";
    public static final String ACTION_PAUSE = "org.helllabs.android.xmp.PAUSE";
    public static final String ACTION_NEXT = "org.helllabs.android.xmp.NEXT";
	private QueueManager queue;
	private final Service service;
	private final long when;

	public Notifier(final Service service) {
    	this.service = service;
    	final Intent intent = new Intent(service, PlayerActivity.class);
    	contentIntent = PendingIntent.getActivity(service, 0, intent, 0);
    	when = System.currentTimeMillis();
	}
	
	private String formatIndex(final int index) {
		return String.format(Locale.US, "%d/%d", index + 1, queue.size());
	}
	
	public void cancel() {
		service.stopForeground(true);
	}
	
	private PendingIntent makePendingIntent(final String action) {
		final Intent intent = new Intent(service, NotificationActionReceiver.class);
		intent.setAction(action);
		return PendingIntent.getBroadcast(service, 0, intent, 0);
	}
	
	public void tickerNotification(final String title, final int index) {
		notification(title, index, true, false);
	}
	
	public void pauseNotification(final String title, final int index) {
		notification(title, index, false, true);
	}
	
	public void unpauseNotification(final String title, final int index) {
		notification(title, index, false, false);
	}
		
	// Notification with player buttons
	private void notification(String title, final int index, final boolean ticker, final boolean paused) {
		final Bitmap icon = BitmapFactory.decodeResource(service.getResources(), R.drawable.icon);
		
		if (title != null && title.trim().isEmpty()) {
			title = "<untitled>";
		}
		
		final PendingIntent stopIntent = makePendingIntent(ACTION_STOP);
		final PendingIntent pauseIntent = makePendingIntent(ACTION_PAUSE);
		final PendingIntent nextIntent = makePendingIntent(ACTION_NEXT);
		final String indexText = formatIndex(index);
		
		final NotificationCompat.Builder builder = new NotificationCompat.Builder(service)
				.setContentTitle(service.getString(R.string.app_name))
				.setContentInfo(indexText)
				.setContentIntent(contentIntent)
				.setSmallIcon(R.drawable.notification_icon)
				.setLargeIcon(icon)
				.setOngoing(true)
				.setWhen(when)
				//.setShowWhen(false)
				.addAction(R.drawable.ic_action_stop, "Stop", stopIntent);
		
		if (ticker) {
			if (queue.size() > 1) {
				builder.setTicker(title + " (" + indexText + ")");
			} else  {
				builder.setTicker(title);
			}
		}
		
		if (paused) {
			builder.addAction(R.drawable.ic_action_play, "Play", pauseIntent);
			builder.setContentText(title + " (paused)");
		} else {
			builder.addAction(R.drawable.ic_action_pause, "Pause", pauseIntent);
			if (android.os.Build.VERSION.SDK_INT < 11) {
				builder.setContentText(title + " (" + indexText + ")");
			} else {
				builder.setContentText(title);
			}
		}
		
		builder.addAction(R.drawable.ic_action_next, "Next", nextIntent);
				
		service.startForeground(NOTIFY_ID, builder.build());
	}
	
	public void setQueue(final QueueManager queue) {
		this.queue = queue;
	}
}
