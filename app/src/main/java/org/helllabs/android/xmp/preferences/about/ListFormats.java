package org.helllabs.android.xmp.preferences.about;

import java.util.Arrays;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.Xmp;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;


public class ListFormats extends ListActivity {
	private final String[] formats = Xmp.getFormats();
	
	@Override
    public void onCreate(final Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.list_formats);
		Arrays.sort(formats);
		setListAdapter(new ArrayAdapter<>(this, R.layout.format_list_item, formats));
	}
}
