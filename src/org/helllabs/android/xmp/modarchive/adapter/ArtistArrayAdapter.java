package org.helllabs.android.xmp.modarchive.adapter;

import java.util.List;

import org.helllabs.android.xmp.modarchive.model.Artist;

import android.content.Context;
import android.widget.ArrayAdapter;

public class ArtistArrayAdapter extends ArrayAdapter<Artist> {
	
	public ArtistArrayAdapter(final Context context, final int resource, final List<Artist> items) {
		super(context, resource, items);
		//this.context = context;
	}
}
