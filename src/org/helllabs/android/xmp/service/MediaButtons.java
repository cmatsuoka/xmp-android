package org.helllabs.android.xmp.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.helllabs.android.xmp.service.receiver.RemoteControlReceiver;
import org.helllabs.android.xmp.util.Log;

import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;

// for media buttons
// see http://android-developers.blogspot.com/2010/06/allowing-applications-to-play-nicer.html

public class MediaButtons {
    private static final String TAG = "MediaButtons";
    private final AudioManager audioManager;
	private final ComponentName remoteControlResponder;
    private static Method registerMediaButtonEventReceiver;
    private static Method unregisterMediaButtonEventReceiver;
    
    static {
    	initializeRemoteControlRegistrationMethods();
    }
	
    public MediaButtons(final Context context) {
		audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
		remoteControlResponder = new ComponentName(context.getPackageName(), RemoteControlReceiver.class.getName());
    }
    
	private static void initializeRemoteControlRegistrationMethods() {
		try {
			if (registerMediaButtonEventReceiver == null) {		// NOPMD
				registerMediaButtonEventReceiver = AudioManager.class
						.getMethod("registerMediaButtonEventReceiver",
								new Class[] { ComponentName.class });
			}
			if (unregisterMediaButtonEventReceiver == null) {	// NOPMD
				unregisterMediaButtonEventReceiver = AudioManager.class
						.getMethod("unregisterMediaButtonEventReceiver",
								new Class[] { ComponentName.class });
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
			registerMediaButtonEventReceiver.invoke(audioManager, remoteControlResponder);
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
			unregisterMediaButtonEventReceiver.invoke(audioManager,	remoteControlResponder);
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
