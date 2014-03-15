package org.helllabs.android.xmp.browser.about;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

public final class AppInfo {
	
	// All methods are staticW
	private AppInfo() {
		// Do nothing
	}
	
	public static String getVersion(final Context context) {
		try {
			final PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
						context.getPackageName(), 0);
			String version = packageInfo.versionName;
			final int end = version.indexOf(' ');
			if (end > 0) {
				version = version.substring(0, end);
			}
			
			return version;
		} catch (NameNotFoundException e) {
			return "";
		}
	}
}
