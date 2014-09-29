package org.helllabs.android.xmp.modarchive;

import org.helllabs.android.xmp.R;

import android.content.Intent;
import android.graphics.Typeface;
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
		
//		// Hide the status bar
//        if (Build.VERSION.SDK_INT < 16) {
//            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        } else {
//        	final View decorView = getWindow().getDecorView();
//        	decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
//        }
        
		setContentView(R.layout.search_error);
		
		setTitle("Search error");
		
		final Throwable error = (Throwable)getIntent().getSerializableExtra(Search.ERROR);
		msg = (TextView)findViewById(R.id.error_message);
		
		String message = error.getMessage();
		
		final int idx = message.indexOf("Exception: ");
		if (idx >= 0) {
			message = message.substring(idx + 11);
		}
		
		if (message.trim().isEmpty()) {
			message = "Software Failure.   Press back button to continue.\n\nGuru Meditation #35068035.48454C50";
		} else {
			message += ".  Press back button to continue.";  // NOPMD
		}
		
		msg.setText(message);
		
		final Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/TopazPlus_a500_v1.0.ttf");
		msg.setTypeface(typeface);
		
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
