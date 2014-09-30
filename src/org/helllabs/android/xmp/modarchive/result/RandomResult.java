package org.helllabs.android.xmp.modarchive.result;

import java.util.List;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.modarchive.model.Module;
import org.helllabs.android.xmp.modarchive.request.ModuleRequest;

import android.os.Bundle;

public class RandomResult extends ModuleResult implements ModuleRequest.OnResponseListener<List<Module>> {
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
	public void onResponse(final List<Module> moduleList) {
		if (!moduleList.isEmpty()) {
			super.onResponse(moduleList);
		} else {
			// Handle error!
		}
	}
}
