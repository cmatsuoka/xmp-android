package org.helllabs.android.xmp.modarchive.adapter;

import java.util.List;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.modarchive.model.Module;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ModuleArrayAdapter extends ArrayAdapter<Module> {
	private final Context context;
	private final List<Module> items;

	public ModuleArrayAdapter(final Context context, final int resource, final int textViewResourceId, final List<Module> items) {
		super(context, resource, textViewResourceId, items);
		this.context = context;
		this.items = items;
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {
		View view = convertView;
    	if (view == null) {
    		final LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    		view = inflater.inflate(R.layout.search_list_item, null);
    	}

    	final Module module = items.get(position);
    	if (module != null) {
    		final TextView fmt = (TextView)view.findViewById(R.id.search_list_fmt);
    		final TextView line1 = (TextView)view.findViewById(R.id.search_list_line1);
    		final TextView line2 = (TextView)view.findViewById(R.id.search_list_line2);
    		final TextView size = (TextView)view.findViewById(R.id.search_list_size);
    		
    		fmt.setText(module.getFormat());
    		line1.setText(module.getSongTitle());
    		line2.setText("by " + module.getArtist());
    		size.setText((module.getBytes() / 1024) + " Kb");
    	}
    	
    	return view;
	}


}
