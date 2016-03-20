package org.helllabs.android.xmp.browser;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.util.Log;
import org.helllabs.android.xmp.util.Message;

import java.util.List;

/**
 * Created by claudio on 3/19/16.
 */
public class UseSdcardActivity extends Activity {

	private final static String TAG = "UseSdcardActivity";
	private TextView info;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.use_sdcard);

		final TextView apiVer = (TextView)findViewById(R.id.sd_apiver);
		info = (TextView)findViewById(R.id.sd_info);

		apiVer.setText(String.format("Android version: %s (API level %d)", Build.VERSION.RELEASE, Build.VERSION.SDK_INT));

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
			info.setText("Android versions prior to 19 don't require special permissions to access the SD card.");
		} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			info.setText("This android device can't access the SD card. Use the internal emulated SD card instead.");
		} else {
			showPermissions();
			Message.yesNoDialog(this, "Enable write access",
					"In the following screen select your external (physical) SD card to enable write access. Continue?",
					new Runnable() {
						@Override
						public void run() {
							final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
							startActivityForResult(intent, 42);
						}
					});
		}
	}

	@Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent resultData) {
		if (requestCode == 42 && resultCode == RESULT_OK) {
			final Uri treeUri = resultData.getData();

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				Log.w(TAG, "grant permissions to " + treeUri.getPath());

				getContentResolver().takePersistableUriPermission(treeUri,
						Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

				showPermissions();
			}
		}
	}

	private void showPermissions() {
		final ContentResolver resolver = getContentResolver();
		final List<UriPermission> list = resolver.getPersistedUriPermissions();

		final StringBuilder builder = new StringBuilder();
		builder.append("Write permission granted to:\n");

		if (list.size() > 0) {
			for (final UriPermission perm : list) {
				builder.append(perm.getUri().getPath());
				builder.append("\n");
			}
		} else {
			builder.append("nothing\n");
		}

		info.setText(builder.toString());
	}
}
