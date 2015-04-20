package org.helllabs.android.xmp.service.notifier;

import java.util.Locale;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.player.PlayerActivity;
import org.helllabs.android.xmp.service.receiver.NotificationActionReceiver;
import org.helllabs.android.xmp.service.utils.QueueManager;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;


public abstract class Notifier {

	protected static final int NOTIFY_ID = R.layout.player;
	
	public static final int TYPE_TICKER = 1;
	public static final int TYPE_PAUSE = 2;
	
	public static final String ACTION_STOP = "org.helllabs.android.xmp.STOP";
	public static final String ACTION_PAUSE = "org.helllabs.android.xmp.PAUSE";
	public static final String ACTION_PREV = "org.helllabs.android.xmp.PREV";
	public static final String ACTION_NEXT = "org.helllabs.android.xmp.NEXT";
	
	protected QueueManager queue;
	protected final PendingIntent contentIntent;
	protected final Service service;
	
	protected final Bitmap icon;
	protected final PendingIntent prevIntent;
	protected final PendingIntent stopIntent;
	protected final PendingIntent pauseIntent;
	protected final PendingIntent nextIntent;
	

	public Notifier(final Service service) {
		this.service = service;
		final Intent intent = new Intent(service, PlayerActivity.class);
		contentIntent = PendingIntent.getActivity(service, 0, intent, 0);
		
		icon = BitmapFactory.decodeResource(service.getResources(), R.drawable.icon);
		prevIntent = makePendingIntent(ACTION_PREV);
		stopIntent = makePendingIntent(ACTION_STOP);
		pauseIntent = makePendingIntent(ACTION_PAUSE);
		nextIntent = makePendingIntent(ACTION_NEXT);
	}

	protected String formatIndex(final int index) {
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

	public abstract void notify(String title, final String info, final int index, final int type);

	public void setQueue(final QueueManager queue) {
		this.queue = queue;
	}
}
