package org.helllabs.android.xmp.preferences.about;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.Xmp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class About extends Activity {

	@Override
	public void onCreate(final Bundle icicle) {	
		super.onCreate(icicle);

		setContentView(R.layout.about);

		((TextView)findViewById(R.id.version_name))
			.setText(getString(R.string.about_version, AppInfo.getVersion(this)));

		((TextView)findViewById(R.id.xmp_version))
			.setText(getString(R.string.about_xmp, Xmp.getVersion()));
	}
}
