package org.helllabs.android.xmp.preferences;

import java.io.File;
import java.io.IOException;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.browser.Message;
import org.helllabs.android.xmp.service.PlayerService;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.KeyEvent;

public class Preferences extends PreferenceActivity {
	public static final File SD_DIR = Environment.getExternalStorageDirectory();
	public static final File DATA_DIR = new File(SD_DIR, "Xmp for Android");
	public static final File CACHE_DIR = new File(SD_DIR, "Android/data/org.helllabs.android.xmp/cache/");

	public static final String DEFAULT_MEDIA_PATH = SD_DIR.toString() + "/mod";
	public static final String MEDIA_PATH = "media_path";
	public static final String VOL_BOOST = "vol_boost";
	public static final String CHANGELOG_VERSION = "changelog_version";
	//public static final String STEREO = "stereo";
	public static final String PAN_SEPARATION = "pan_separation";
	public static final String DEFAULT_PAN = "default_pan";
	public static final String PLAYLIST_MODE = "playlist_mode";
	// Don't use PREF_INTERPOLATION -- was boolean in 2.x and string in 3.2.0
	public static final String INTERPOLATE = "interpolate";
	public static final String INTERP_TYPE = "interp_type";
	//public static final String FILTER = "filter";
	public static final String EXAMPLES = "examples";
	public static final String SAMPLING_RATE = "sampling_rate";
	//public static final String BUFFER_MS = "buffer_ms";
	public static final String BUFFER_MS = "buffer_ms_opensl";
	public static final String SHOW_TOAST = "show_toast";
	public static final String SHOW_INFO_LINE = "show_info_line";
	public static final String USE_FILENAME = "use_filename";
	//public static final String TITLES_IN_BROWSER = "titles_in_browser";
	public static final String ENABLE_DELETE = "enable_delete";
	public static final String KEEP_SCREEN_ON = "keep_screen_on";
	public static final String HEADSET_PAUSE = "headset_pause";
	public static final String ALL_SEQUENCES = "all_sequences";
	public static final String BACK_BUTTON_PARENTDIR = "back_button_parentdir";

	private SharedPreferences prefs;
	private String oldPath;

	@Override
	protected void onCreate(final Bundle icicle) {
		super.onCreate(icicle);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		oldPath = prefs.getString(MEDIA_PATH, DEFAULT_MEDIA_PATH);
		addPreferencesFromResource(R.xml.preferences);

		final PreferenceScreen soundScreen = (PreferenceScreen)findPreference("sound_screen");
		soundScreen.setEnabled(!PlayerService.isAlive);

		final Preference clearCache = (Preference)findPreference("clear_cache");
		clearCache.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(final Preference preference) {
				try {
					deleteCache(CACHE_DIR);
					Message.toast(getBaseContext(), getString(R.string.cache_clear));
				} catch (IOException e) {
					Message.toast(getBaseContext(), getString(R.string.cache_clear_error));
				}
				return true;
			}
		});
	}


	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) { 	
		if(event.getAction() == KeyEvent.ACTION_DOWN) {
			if (keyCode == KeyEvent.KEYCODE_BACK) { 
				final String newPath = prefs.getString(MEDIA_PATH, DEFAULT_MEDIA_PATH);
				setResult(newPath.equals(oldPath) ? RESULT_CANCELED : RESULT_OK);        				
				finish();   			
			}
		}

		return super.onKeyDown(keyCode, event);
	}

	public static void deleteCache(final File file) throws IOException {
		if (!file.exists()) {
			return;
		}

		if (file.isDirectory()) {
			for (final File cacheFile : file.listFiles()) {
				deleteCache(cacheFile);
			}
		}
		file.delete();
	}
}
