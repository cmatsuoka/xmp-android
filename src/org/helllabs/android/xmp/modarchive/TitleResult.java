package org.helllabs.android.xmp.modarchive;

import java.util.List;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.modarchive.model.Module;
import org.helllabs.android.xmp.modarchive.request.ModuleRequest;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class TitleResult extends Result implements ModuleRequest.OnResponseListener<List<Module>>, ListView.OnItemClickListener {
	private Context context;
	private ListView list;
	private List<Module> moduleList;

	private static class TitleResultAdapter extends ArrayAdapter<Module> {

		private Context context;
		private List<Module> items;

		public TitleResultAdapter(final Context context, final int resource, final int textViewResourceId, final List<Module> items) {
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
	    		size.setText((module.getBytes() / 1024) + " Kb");
	    		line1.setText(module.getSongTitle());
	    		line2.setText("by " + module.getArtist());
	    	}
	    	
	    	return view;
		}

	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.result_list);
		setupCrossfade();

		setTitle("Title search");

		context = this;
		list = (ListView)findViewById(R.id.result_list);

		final String searchText = getIntent().getStringExtra(Search.SEARCH_TEXT);

		final String key = getString(R.string.modarchive_apikey);
		final ModuleRequest request = new ModuleRequest(key, "search&type=filename_and_songtitle&query=" + searchText);
		request.setOnResponseListener(this).send();

		list.setOnItemClickListener(this);
	}

	@Override
	public void onResponse(final List<Module> response) {
		moduleList = response;
		final TitleResultAdapter adapter = new TitleResultAdapter(context, R.layout.search_list_item, R.id.search_list_line1, response);
		list.setAdapter(adapter);
		crossfade();
	}

	@Override
	public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
		final Intent intent = new Intent(this, ModuleResult.class);
		intent.putExtra(Search.MODULE_ID, moduleList.get(position).getId());
		startActivity(intent);
	}


}
