package org.helllabs.android.xmp.modarchive.result;

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

public class ModuleResult extends Result implements ModuleRequest.OnResponseListener<List<Module>>, View.OnClickListener {
	private static final String TAG = "ModuleResult";
	private static final String MODARCHIVE_DIRNAME = "ModArchive";
	private TextView title;
	private TextView filename;
	private TextView info;
	private TextView instruments;
	private TextView license;
	private Module module;
	private Downloader downloader;
	
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
		
		final Button downloadButton = (Button)findViewById(R.id.module_download);
		downloadButton.setOnClickListener(this);

		downloader = new Downloader(this);

		final long id = getIntent().getLongExtra(Search.MODULE_ID, -1);
		Log.d(TAG, "request module ID " + id);
		makeRequest(String.valueOf(id));
	}

	protected void makeRequest(final String query) {
		final String key = getString(R.string.modarchive_apikey);
		final ModuleRequest request = new ModuleRequest(key, "view_by_moduleid&query=" + query);
		request.setOnResponseListener(this).send();
	}

	@Override
	public void onResponse(final List<Module> moduleList) {
		final Module module = moduleList.get(0);
		Log.i(TAG, "Response: title=" + module.getSongTitle());
		title.setText(module.getSongTitle());
		filename.setText(module.getFilename());
		final float size = (float)module.getBytes() / 1024;
		info.setText(String.format("%s by %s (%.1f KB)", module.getFormat(), module.getArtist(), size));
		license.setText("License: " + module.getLicense());
		instruments.setText(module.getInstruments());

		this.module = module;

		crossfade();
	}
	
	@Override
	public void onError(final Throwable error) {
		handleError(error);
	}
	
	@Override
	public void onClick(final View view) {
		final StringBuffer sb = new StringBuffer();
		
		sb.append(mPrefs.getString(Preferences.MEDIA_PATH, Preferences.DEFAULT_MEDIA_PATH));
		
		if (mPrefs.getBoolean(Preferences.MODARCHIVE_FOLDER, true)) {
			sb.append('/');
			sb.append(MODARCHIVE_DIRNAME);
		}
		
		if (mPrefs.getBoolean(Preferences.ARTIST_FOLDER, true)) {
			sb.append('/');
			sb.append(module.getArtist());
		}
		
		final String modDir = sb.toString();
		final String url = module.getUrl();
		
		Log.i(TAG, "Download " + url + " to " + modDir);
		downloader.download(url, modDir);	
	}

}
