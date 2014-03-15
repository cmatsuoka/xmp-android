package org.helllabs.android.xmp.browser.about;


import org.helllabs.android.xmp.R;
import org.helllabs.libxmp.Player;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;

public class ListFormats extends ListActivity {
		
	private final String[] formats = Player.formatList();
	
	@Override
    public void onCreate(final Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.list_formats);
		setListAdapter(new ArrayAdapter<String>(this, 
				R.layout.format_list_item, formats));
	}
}
