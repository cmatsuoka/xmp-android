package org.helllabs.android.xmp.service;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.player.PlayerActivity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class Notifier {
	private final NotificationManager nm;	// NOPMD
    private final PendingIntent contentIntent;
    private static final int NOTIFY_ID = R.layout.player;
	private String title;
	private int index;
	private QueueManager queue;
	private final Context context;

	public Notifier(final Context context) {
    	this.context = context;
		nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
    	contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, PlayerActivity.class), 0);
	}
	
	private String message() {
		return queue.size() > 1 ?
			String.format("%s (%d/%d)", title, index, queue.size()) :
			title;
	}
	
	public void cancel() {
		nm.cancel(NOTIFY_ID);
	}
	
	public void notification() {
		notification(null, null);
	}
	
	public void notification(final String title, final int index) {
		this.title = title;
		this.index = index + 1;			
		notification(message(), message());
	}
	
	public void notification(final String ticker) {
		notification(ticker, message());
	}
	
	public void notification(final String ticker, final String latest) {
        Notification notification = new Notification(
        		R.drawable.notification, ticker, System.currentTimeMillis());
        notification.setLatestEventInfo(context, context.getText(R.string.app_name),
        		latest, contentIntent);
        notification.flags |= Notification.FLAG_ONGOING_EVENT;	        
        nm.notify(NOTIFY_ID, notification);				
	}
	
	public void setQueue(QueueManager queue) {
		this.queue = queue;
	}
}
