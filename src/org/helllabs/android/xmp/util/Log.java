package org.helllabs.android.xmp.util;


public final class Log {
	private static final String TAG = "Xmp";
	
	private Log() {
		
	}
	
	public static void i(final String tag, final String message) {
		android.util.Log.i(TAG, "[" + tag + "] " + message);
	}
	
	public static void w(final String tag, final String message) {
		android.util.Log.w(TAG, "[" + tag + "] " + message);
	}
	
	public static void e(final String tag, final String message) {
		android.util.Log.e(TAG, "[" + tag + "] " + message);
	}
}
