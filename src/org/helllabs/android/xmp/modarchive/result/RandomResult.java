package org.helllabs.android.xmp.modarchive.result;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.modarchive.request.ModuleRequest;
import org.helllabs.android.xmp.modarchive.response.ModArchiveResponse;
import org.helllabs.android.xmp.modarchive.response.ModuleResponse;

import android.os.Bundle;

public class RandomResult extends ModuleResult implements ModuleRequest.OnResponseListener {
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(R.string.search_random_title);
	}
	
	@Override
	protected void makeRequest(final String query) {
		final String key = getString(R.string.modarchive_apikey);
		final ModuleRequest request = new ModuleRequest(key, ModuleRequest.RANDOM);
		request.setOnResponseListener(this).send();
	}
	
	@Override
	public void onResponse(final ModArchiveResponse response) {
		final ModuleResponse moduleList = (ModuleResponse)response;
		if (!moduleList.isEmpty()) {
			super.onResponse(moduleList);
		} else {
			// Handle error!
		}
	}
}
