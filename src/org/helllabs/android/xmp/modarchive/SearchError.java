package org.helllabs.android.xmp.modarchive;

import org.helllabs.android.xmp.R;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
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
	
	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		
		// Back key returns to search
	    if (keyCode == KeyEvent.KEYCODE_BACK) {
	        final Intent intent = new Intent(this, Search.class);
	        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	        startActivity(intent);
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}
}
