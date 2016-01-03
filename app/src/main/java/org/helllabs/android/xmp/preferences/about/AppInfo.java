package org.helllabs.android.xmp.preferences.about;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

final class AppInfo {
	
	private AppInfo() {
		
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
