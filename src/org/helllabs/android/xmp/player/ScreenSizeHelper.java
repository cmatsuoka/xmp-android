package org.helllabs.android.xmp.player;

import android.content.Context;
import android.content.res.Configuration;

public class ScreenSizeHelper {	
	public int getScreenSize(final Context context) {
		return context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
	}
}
