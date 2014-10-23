package org.helllabs.android.xmp.service.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.session.MediaSession;
import android.os.Build;

@TargetApi(21)
public class MediaSessionWrapper {
	
	private MediaSession session;
	
	public MediaSessionWrapper(final Context context, final String tag) {
		if (Build.VERSION.SDK_INT >= 21) {
			session = new MediaSession(context, tag);
		}
	}
	
	public void setActive(final boolean active) {
		if (session != null) {
			session.setActive(active);
		}
	}
	
	public MediaSession.Token getSessionToken() {
		if (session != null) {
			return session.getSessionToken();
		} else {
			return null;
		}
	}

}
