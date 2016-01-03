package org.helllabs.android.xmp.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.helllabs.android.xmp.service.receiver.MediaButtonsReceiver;
import org.helllabs.android.xmp.util.Log;

import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;

// for media buttons
// see http://android-developers.blogspot.com/2010/06/allowing-applications-to-play-nicer.html

class MediaButtons {
    private static final String TAG = "MediaButtons";
    private final AudioManager audioManager;
	private final ComponentName mediaButtonsResponder;
    private static Method registerMediaButtonEventReceiver;
    private static Method unregisterMediaButtonEventReceiver;
    
    static {
    	initializeRegistrationMethods();
    }
	
    public MediaButtons(final Context context) {
		audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
		mediaButtonsResponder = new ComponentName(context.getPackageName(), MediaButtonsReceiver.class.getName());
    }
    
	private static void initializeRegistrationMethods() {
		try {
			if (registerMediaButtonEventReceiver == null) {		// NOPMD
				registerMediaButtonEventReceiver = AudioManager.class
						.getMethod("registerMediaButtonEventReceiver", ComponentName.class);
			}
			if (unregisterMediaButtonEventReceiver == null) {	// NOPMD
				unregisterMediaButtonEventReceiver = AudioManager.class
						.getMethod("unregisterMediaButtonEventReceiver", ComponentName.class);
			}
			/* success, this device will take advantage of better remote */
			/* control event handling */
		} catch (NoSuchMethodException nsme) {
			/* failure, still using the legacy behavior, but this app */
			/* is future-proof! */
			Log.e(TAG, nsme.getMessage());
		}
	}

	public void register() {
		try {
			if (registerMediaButtonEventReceiver == null) {
				return;
			}
			registerMediaButtonEventReceiver.invoke(audioManager, mediaButtonsResponder);
		} catch (InvocationTargetException ite) {
			// unpack original exception when possible
			final Throwable cause = ite.getCause();
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			} else if (cause instanceof Error) {
				throw (Error) cause;
			} else {
				// unexpected checked exception; wrap and re-throw
				throw new RuntimeException(ite);	// NOPMD
			}
		} catch (IllegalAccessException ie) {
			Log.e(TAG, "Unexpected " + ie);
		}
	}

	public void unregister() {
		try {
			if (unregisterMediaButtonEventReceiver == null) {
				return;
			}
			unregisterMediaButtonEventReceiver.invoke(audioManager,	mediaButtonsResponder);
		} catch (InvocationTargetException ite) {
			// unpack original exception when possible
			final Throwable cause = ite.getCause();
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			} else if (cause instanceof Error) {
				throw (Error) cause;
			} else {
				// unexpected checked exception; wrap and re-throw
				throw new RuntimeException(ite);	// NOPMD
			}
		} catch (IllegalAccessException ie) {
			Log.e(TAG, "Unexpected " + ie);
		}
	}
}
