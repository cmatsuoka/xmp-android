package org.helllabs.android.xmp.modarchive;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.util.Crossfader;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public abstract class Result extends ActionBarActivity  {
	
	private Crossfader crossfader;

	
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(R.string.search_result_title);
		crossfader = new Crossfader(this);
	}

	protected void setupCrossfade() {
		crossfader.setup(R.id.result_content, R.id.result_spinner);
	}

	protected void crossfade() {
		crossfader.crossfade();
	}
}
