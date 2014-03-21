package org.helllabs.android.xmp.service;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.player.PlayerActivity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;


public class Notifier {
	private final NotificationManager nm;	// NOPMD
    private final PendingIntent contentIntent;
    private static final int NOTIFY_ID = R.layout.player;
    public static final String ACTION_STOP = "stop";
    public static final String ACTION_PAUSE = "pause";
    public static final String ACTION_NEXT = "next";
	private QueueManager queue;
	private final Context context;

	public Notifier(final Context context) {
    	this.context = context;
		nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
    	contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, PlayerActivity.class), 0);
	}
	
	private String formatIndex(final int index) {
		return String.format("%d/%d", index + 1, queue.size());
	}
	
	public void cancel() {
		nm.cancel(NOTIFY_ID);
	}
	
	public void clean() {
		notification(null);
	}
	
	// Simple one-line notification for player messages
	public void notification(final String title) {			
		final Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon);
		
		final Notification notification = new NotificationCompat.Builder(context)
				.setContentTitle(context.getString(R.string.app_name))
				.setContentText(title)
				.setContentIntent(contentIntent)
				.setSmallIcon(R.drawable.icon)
				.setLargeIcon(icon)
				.setOngoing(true)
				.build();
		
		nm.notify(NOTIFY_ID, notification);
	}
	
	private PendingIntent makePendingIntent(final String action) {
		final Intent intent = new Intent(context, NotificationActionReceiver.class);
		intent.setAction(action);
		return PendingIntent.getBroadcast(context, 0, intent, 0);
	}
	
	// Notification with player buttons
	public void notification(final String title, final int index) {
		final Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon);
		
		final PendingIntent stopIntent = makePendingIntent(ACTION_STOP);
		final PendingIntent pauseIntent = makePendingIntent(ACTION_PAUSE);
		final PendingIntent nextIntent = makePendingIntent(ACTION_NEXT);
		
		final Notification notification = new NotificationCompat.Builder(context)
				.setContentTitle(context.getString(R.string.app_name))
				.setContentInfo(formatIndex(index))
				.setContentText(title)
				.setContentIntent(contentIntent)
				.setSmallIcon(R.drawable.icon)
				.setLargeIcon(icon)
				.setOngoing(true)
				//.setShowWhen(false)
				.addAction(R.drawable.ic_action_stop, "Stop", stopIntent)
				.addAction(R.drawable.ic_action_pause, "Pause", pauseIntent)
				.addAction(R.drawable.ic_action_next, "Next", nextIntent)
				.build();
		
		nm.notify(NOTIFY_ID, notification);
	}
	
	public void setQueue(final QueueManager queue) {
		this.queue = queue;
	}
}
