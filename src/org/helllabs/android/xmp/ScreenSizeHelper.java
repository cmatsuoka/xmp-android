package org.helllabs.android.xmp;

import android.content.Context;
import android.content.res.Configuration;

public class ScreenSizeHelper {	
	public int getScreenSize(Context context) {
		return context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
	}
}
