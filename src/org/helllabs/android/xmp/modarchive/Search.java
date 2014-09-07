package org.helllabs.android.xmp.modarchive;

import org.helllabs.android.xmp.R;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class Search extends ActionBarActivity {
	
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search);
		
		setTitle("Module search");
	}

}
