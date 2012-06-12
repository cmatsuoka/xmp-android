package org.helllabs.android.xmp;

import java.io.File;
import java.io.IOException;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.KeyEvent;

public class Settings extends android.preference.PreferenceActivity {
	// Workaround for NOOKcolor reporting bogus sdcard directory
	static final File sdDir = new File("/media/B&N Downloads").isDirectory() ?
			new File("/media") : Environment.getExternalStorageDirectory();
	static final File dataDir = new File(sdDir, "Xmp for Android");
	static final File oldCacheDir = new File(dataDir, "cache");
	static final File cacheDir = new File(sdDir, "Android/data/org.helllabs.android.xmp/cache/");
	public static final String DEFAULT_MEDIA_PATH = sdDir.toString() + "/mod";
	public static final String PREF_MEDIA_PATH = "media_path";
	public static final String PREF_VOL_BOOST = "vol_boost";
	public static final String PREF_CHANGELOG_VERSION = "changelog_version";
	public static final String PREF_STEREO = "stereo";
	public static final String PREF_PAN_SEPARATION = "pan_separation";
	public static final String PREF_EXAMPLES = "examples";
	public static final String PREF_SAMPLING_RATE = "sampling_rate";
	public static final String PREF_BUFFER_MS = "buffer_ms";
	public static final String PREF_SHOW_TOAST = "show_toast";
	public static final String PREF_SHOW_INFO_LINE = "show_info_line";
	public static final String PREF_USE_FILENAME = "use_filename";
	public static final String PREF_TITLES_IN_BROWSER = "titles_in_browser";
	public static final String PREF_ENABLE_DELETE = "enable_delete";
	public static final String PREF_KEEP_SCREEN_ON = "keep_screen_on";
	public static final String PREF_DARK_THEME = "dark_theme";
	private SharedPreferences prefs;
	private String oldPath;
	private boolean oldDarkTheme;
	
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        oldPath = prefs.getString(PREF_MEDIA_PATH, DEFAULT_MEDIA_PATH);
        oldDarkTheme = prefs.getBoolean(PREF_DARK_THEME, false);
        addPreferencesFromResource(R.xml.preferences);
        
        PreferenceScreen soundScreen = (PreferenceScreen)findPreference("sound_screen");
        soundScreen.setEnabled(!ModService.isAlive);
        
        Preference clearCache = (Preference)findPreference("clear_cache");
        clearCache.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        	public boolean onPreferenceClick(Preference preference) {
        		try {
					deleteCache(cacheDir);
					Message.toast(getBaseContext(), getString(R.string.cache_clear));
				} catch (IOException e) {
					Message.toast(getBaseContext(), getString(R.string.cache_clear_error));
				}
        		return true;
        	}
        });
    }

        
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) { 	
    	if(event.getAction() == KeyEvent.ACTION_DOWN) {
    		switch(keyCode) {
    		case KeyEvent.KEYCODE_BACK:
    			boolean newDarkTheme = prefs.getBoolean(PREF_DARK_THEME, false);
    			if (newDarkTheme != oldDarkTheme) {
    				// Restart application
    				setResult(RESULT_FIRST_USER);
    			} else {
    	   			String newPath = prefs.getString(PREF_MEDIA_PATH, DEFAULT_MEDIA_PATH);
        			setResult(newPath.equals(oldPath) ? RESULT_CANCELED : RESULT_OK);        				
    			}
    			finish();   			
    		}
    	}

    	return super.onKeyDown(keyCode, event);
    }
    
	public static void deleteCache(File f) throws IOException {
		if (!f.exists())
			return;
		
		if (f.isDirectory()) {
			for (File c : f.listFiles())
				deleteCache(c);
		}
		f.delete();
	}
}
