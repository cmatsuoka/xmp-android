package org.helllabs.android.xmp.modarchive.result;

import java.util.List;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.modarchive.model.Module;
import org.helllabs.android.xmp.modarchive.request.ModuleRequest;


public class RandomResult extends ModuleResult implements ModuleRequest.OnResponseListener<List<Module>> {

	@Override
	protected void makeRequest(final String query) {
		final String key = getString(R.string.modarchive_apikey);
		final ModuleRequest request = new ModuleRequest(key, "random");
		request.setOnResponseListener(this).send();
	}
	
	@Override
	public void onResponse(final List<Module> moduleList) {
		if (moduleList.size() > 0) {
			super.onResponse(moduleList);
		} else {
			// Handle error!
		}
	}
}
