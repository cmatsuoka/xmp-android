package org.helllabs.android.xmp.service.notifier;

import org.helllabs.android.xmp.R;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.Service;
import android.media.session.MediaSession;

public class LollipopNotifier extends Notifier {
	
	private final MediaSession.Token token;
	
	public LollipopNotifier(final Service service, final MediaSession.Token token) {
		super(service);
		this.token = token;
	}

	@TargetApi(21)
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
			.setStyle(new Notification.MediaStyle().setMediaSession(token).setShowActionsInCompactView(2))
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
