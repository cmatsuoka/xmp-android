package org.helllabs.android.xmp.modarchive;

import org.helllabs.android.xmp.R;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.TextView;

public class SearchError extends ActionBarActivity {
	
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_error);
		
		setTitle("Search error");
		
		final Throwable error = (Throwable)getIntent().getSerializableExtra(Search.ERROR);
		final TextView msg = (TextView)findViewById(R.id.error_message);
		msg.setText(error.getMessage());
	}
}
