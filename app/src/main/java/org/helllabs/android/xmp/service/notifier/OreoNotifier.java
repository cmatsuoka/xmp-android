package org.helllabs.android.xmp.service.notifier;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.os.Build;

import org.helllabs.android.xmp.R;

public class OreoNotifier extends Notifier {

	static private final String CHANNEL_ID = "xmp";

	public OreoNotifier(final Service service /*, final MediaSession.Token token*/) {
		super(service);
		createNotificationChannel(service);
	}

	@TargetApi(26)
	private void createNotificationChannel(final Service service) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			final NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
					service.getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_LOW);
			channel.setDescription(service.getString(R.string.notif_channel_desc));
			final NotificationManager notificationManager = service.getSystemService(NotificationManager.class);
			notificationManager.createNotificationChannel(channel);
		}
	}

	@TargetApi(26)
	public void notify(String title, String info, final int index, final int type) {

		if (title != null && title.trim().isEmpty()) {
			title = "<untitled>";
		}

		final String indexText = formatIndex(index);

		if (type == TYPE_PAUSE) {
			info = "(paused)";
		}

		final Notification.Builder builder = new Notification.Builder(service)
				.setContentTitle(title)
				.setContentText(info)
				.setContentInfo(indexText)
				.setContentIntent(contentIntent)
				.setSmallIcon(R.drawable.notification_icon)
				.setLargeIcon(icon)
				.setOngoing(true)
				.setWhen(0)
				.setChannelId(CHANNEL_ID)
				.setStyle(new Notification.MediaStyle().setShowActionsInCompactView(2))
				.setVisibility(Notification.VISIBILITY_PUBLIC)
				.addAction(R.drawable.ic_action_previous, "Prev", prevIntent)
				.addAction(R.drawable.ic_action_stop, "Stop", stopIntent);

		if (type == TYPE_PAUSE) {
			builder.addAction(R.drawable.ic_action_play, "Play", pauseIntent);
			builder.setContentText("(paused)");
		} else {
			builder.addAction(R.drawable.ic_action_pause, "Pause", pauseIntent);
		}

		builder.addAction(R.drawable.ic_action_next, "Next", nextIntent);

		if (type == TYPE_TICKER) {
			if (queue.size() > 1) {
				builder.setTicker(title + " (" + indexText + ")");
			} else  {
				builder.setTicker(title);
			}
		}

		service.startForeground(NOTIFY_ID, builder.build());
	}


}
