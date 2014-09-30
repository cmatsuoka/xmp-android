package org.helllabs.android.xmp.modarchive.result;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.modarchive.Search;
import org.helllabs.android.xmp.modarchive.SearchError;
import org.helllabs.android.xmp.util.Crossfader;

import android.content.Intent;
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

	protected void handleError(final Throwable error) {
		final Intent intent = new Intent(this, SearchError.class);
		intent.putExtra(Search.ERROR, error);
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		startActivity(intent);
	}
	
	protected void handleQueryError() {
		handleError(new Throwable("Bad search string. "));	// NOPMD
	}
}
