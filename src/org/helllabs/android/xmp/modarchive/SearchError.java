package org.helllabs.android.xmp.modarchive;

import org.helllabs.android.xmp.R;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.widget.TextView;

public class SearchError extends ActionBarActivity implements Runnable {
	
	private static final int PERIOD = 1337;
	
	private TextView msg;
	private boolean frameBlink;
	
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_error);
		
		setTitle("Search error");
		
		final Throwable error = (Throwable)getIntent().getSerializableExtra(Search.ERROR);
		msg = (TextView)findViewById(R.id.error_message);
		msg.setText(error.getMessage());
		
		msg.postDelayed(this, PERIOD);
	}
	
	
	@Override
	public void onDestroy() {
		msg.removeCallbacks(this);
		super.onDestroy();
	}
	
	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		
		// Back key returns to search
	    if (keyCode == KeyEvent.KEYCODE_BACK) {
	        final Intent intent = new Intent(this, Search.class);
	        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	        startActivity(intent);
	        overridePendingTransition(0, 0);
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}


	@SuppressWarnings("deprecation")
	@Override
	public void run() {
		// Guru frame blink
		msg.setBackgroundDrawable(getResources().getDrawable(frameBlink ? R.drawable.guru_frame : R.drawable.guru_frame_2));
		frameBlink ^= true;
		msg.postDelayed(this, PERIOD);		
	}
}
