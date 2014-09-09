package org.helllabs.android.xmp.modarchive;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.util.Log;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;


public class RandomResult extends Result implements ModArchiveRequest.OnResponseListener {
	
	private static final String TAG = "RandomResult";
	private TextView title;
	private TextView filename;
	private TextView info;
	private TextView instruments;
	private TextView license;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.result_random);	
		setupCrossfade();
		
		title = (TextView)findViewById(R.id.module_title);
		filename = (TextView)findViewById(R.id.module_filename);
		info = (TextView)findViewById(R.id.module_info);
		instruments = (TextView)findViewById(R.id.module_instruments);
		license = (TextView)findViewById(R.id.module_license);
		
		final String key = getString(R.string.modarchive_apikey);
		final ModArchiveRequest request = new ModArchiveRequest(key, "random");
		request.setOnResponseListener(this).send();
	}

	@Override
	public void onResponse(final Module module) {
		Log.i(TAG, "Response: " + module.getSongTitle());
		title.setText(module.getSongTitle());
		filename.setText(module.getFilename());
		final float size = (float)module.getBytes() / 1024;
		info.setText(String.format("%s by %s (%.1f KB)", module.getFormat(), module.getArtist(), size));
		license.setText("License: " + module.getLicense());
		instruments.setText(module.getInstruments());
		crossfade();
		
	}
	
	public void onDownloadClick(final View view) {
		
	}
}
