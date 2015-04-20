package org.helllabs.android.xmp.service.notifier;

import org.helllabs.android.xmp.R;

import android.app.Service;
import android.support.v4.app.NotificationCompat;


public class LegacyNotifier extends Notifier {
	
	private final long when;
	
	public LegacyNotifier(final Service service) {
		super(service);
		when = System.currentTimeMillis();
	}

	@Override
	public void notify(String title, final String info, final int index, final int type) {

		if (title != null && title.trim().isEmpty()) {
			title = "<untitled>";
		}
		
		final String indexText = formatIndex(index);
			
		final NotificationCompat.Builder builder = new NotificationCompat.Builder(service)
			.setContentTitle(title)
			.setContentText(info)
			.setContentInfo(indexText)
			.setContentIntent(contentIntent)
			.setSmallIcon(R.drawable.notification_icon)
			.setLargeIcon(icon)
			.setOngoing(true)
			.setWhen(when)
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
