package org.helllabs.android.xmp.modarchive;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.util.Log;
import org.json.JSONObject;

import android.os.Bundle;


public class RandomResult extends Result implements ModArchiveRequest.OnResponseListener {
	
	private static final String TAG = "RandomResult";

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.result_random);	
		setupCrossfade();
		
		final String key = getString(R.string.modarchive_apikey);
		final ModArchiveRequest request = new ModArchiveRequest(key, "random");
		request.setOnResponseListener(this).send();
	}

	@Override
	public void onResponse(JSONObject json) {
		Log.e(TAG, "JSON=" + json);
		
	}
}
