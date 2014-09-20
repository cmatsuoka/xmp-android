package org.helllabs.android.xmp.modarchive;

import java.util.List;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.modarchive.model.Module;
import org.helllabs.android.xmp.modarchive.request.ModuleRequest;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class TitleResult extends Result implements ModuleRequest.OnResponseListener<List<Module>>, ListView.OnItemClickListener {
	private Context context;
	private ListView list;
	private List<Module> moduleList;

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
		final ArrayAdapter<Module> adapter = new ArrayAdapter<Module>(context, android.R.layout.simple_list_item_1, response);
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
