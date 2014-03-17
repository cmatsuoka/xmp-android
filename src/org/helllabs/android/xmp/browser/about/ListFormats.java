package org.helllabs.android.xmp.browser.about;


import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.Xmp;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;

public class ListFormats extends ListActivity {
	private final Xmp xmp = new Xmp();	
	private final String[] formats = xmp.getFormats();
	
	@Override
    public void onCreate(final Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.list_formats);
		setListAdapter(new ArrayAdapter<String>(this, 
				R.layout.format_list_item, formats));
	}
}
