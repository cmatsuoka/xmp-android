package org.helllabs.android.xmp.modarchive.result;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.modarchive.Downloader;
import org.helllabs.android.xmp.modarchive.Search;
import org.helllabs.android.xmp.modarchive.model.Module;
import org.helllabs.android.xmp.modarchive.request.ModuleRequest;
import org.helllabs.android.xmp.preferences.Preferences;
import org.helllabs.android.xmp.util.Log;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ModuleResult extends Result implements ModuleRequest.OnResponseListener<List<Module>>, Downloader.DownloaderListener {
	private static final String TAG = "ModuleResult";
	private static final String MODARCHIVE_DIRNAME = "TheModArchive";
	private TextView title;
	private TextView filename;
	private TextView info;
	private TextView instruments;
	private TextView license;
	private Module module;
	private Downloader downloader;
	private Button deleteButton;
	private Button playButton;

	private SharedPreferences mPrefs;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.result_module);	
		setupCrossfade();

		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		title = (TextView)findViewById(R.id.module_title);
		filename = (TextView)findViewById(R.id.module_filename);
		info = (TextView)findViewById(R.id.module_info);
		instruments = (TextView)findViewById(R.id.module_instruments);
		license = (TextView)findViewById(R.id.module_license);

		deleteButton = (Button)findViewById(R.id.module_delete);
		deleteButton.setEnabled(false);

		playButton = (Button)findViewById(R.id.module_play);
		playButton.setEnabled(false);

		downloader = new Downloader(this);
		downloader.setDownloaderListener(this);

		final long id = getIntent().getLongExtra(Search.MODULE_ID, -1);
		Log.d(TAG, "request module ID " + id);
		makeRequest(String.valueOf(id));
	}

	protected void makeRequest(final String query) {
		final String key = getString(R.string.modarchive_apikey);
		try {
			final ModuleRequest request = new ModuleRequest(key, ModuleRequest.MODULE, query);
			request.setOnResponseListener(this).send();
		} catch (UnsupportedEncodingException e) {
			handleQueryError();
		}
	}

	@Override
	public void onResponse(final List<Module> moduleList) {
		if (moduleList.size() > 0) {
			final Module module = moduleList.get(0);
			Log.i(TAG, "Response: title=" + module.getSongTitle());
			title.setText(module.getSongTitle());
			filename.setText(module.getFilename());
			final int size = module.getBytes() / 1024;
			info.setText(String.format("%s by %s (%d KB)", module.getFormat(), module.getArtist(), size));
			license.setText("License: " + module.getLicense());
			instruments.setText(module.getInstruments());
			this.module = module;

			updateButtons(module);

		}

		crossfade();
	}

	// ModuleRequest callbacks
	
	@Override
	public void onError(final Throwable error) {
		handleError(error);
	}
	
	// DownloaderListener callbacks

	@Override
	public void onSuccess() {
		updateButtons(module);
	}

	@Override
	public void onFailure() {
		// do nothing
	}

	// Button click handlers
	
	public void downloadClick(final View view) {

		final String modDir = getDownloadPath(module);
		final String url = module.getUrl();

		Log.i(TAG, "Download " + url + " to " + modDir);
		downloader.download(url, modDir, module.getBytes());	
	}

	public void deleteClick(final View view) {

	}

	public void playClick(final View view) {

	}

	private String getDownloadPath(final Module module) {
		final StringBuffer sb = new StringBuffer();

		sb.append(mPrefs.getString(Preferences.MEDIA_PATH, Preferences.DEFAULT_MEDIA_PATH));

		if (mPrefs.getBoolean(Preferences.MODARCHIVE_FOLDER, true)) {
			sb.append(File.separatorChar);
			sb.append(MODARCHIVE_DIRNAME);
		}

		if (mPrefs.getBoolean(Preferences.ARTIST_FOLDER, true)) {
			sb.append(File.separatorChar);
			sb.append(module.getArtist());
		}

		return sb.toString();
	}
	
	private void updateButtons(final Module module) {
		final String modDir = getDownloadPath(module);
		final String url = module.getUrl();

		if (Downloader.moduleExists(url, modDir)) {
			deleteButton.setEnabled(true);
			playButton.setEnabled(true);
		}
	}
}
