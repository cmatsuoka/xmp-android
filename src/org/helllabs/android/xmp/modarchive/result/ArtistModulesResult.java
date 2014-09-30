package org.helllabs.android.xmp.modarchive.result;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.modarchive.Search;
import org.helllabs.android.xmp.modarchive.adapter.ModuleArrayAdapter;
import org.helllabs.android.xmp.modarchive.model.Module;
import org.helllabs.android.xmp.modarchive.request.ModuleRequest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class ArtistModulesResult extends Result implements ModuleRequest.OnResponseListener<List<Module>>, ListView.OnItemClickListener {

	private ListView list;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.result_list);
		setupCrossfade();

		setTitle(R.string.search_artist_modules_title);

		list = (ListView)findViewById(R.id.result_list);
		list.setOnItemClickListener(this);

		final long artistId = getIntent().getLongExtra(Search.ARTIST_ID, -1);
		final String key = getString(R.string.modarchive_apikey);

		try {
			final ModuleRequest request = new ModuleRequest(key, ModuleRequest.ARTIST_MODULES, artistId);
			request.setOnResponseListener(this).send();
		} catch (UnsupportedEncodingException e) {
			handleQueryError();
		}
	}

	@Override
	public void onResponse(final List<Module> response) {
		final ModuleArrayAdapter adapter = new ModuleArrayAdapter(this, R.layout.search_list_item, response);
		list.setAdapter(adapter);
		
		if (response.isEmpty()) {
			list.setVisibility(View.GONE);
		}
		
		crossfade();
	}

	@Override
	public void onError(final Throwable error) {
		handleError(error);
	}

	@Override
	public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
		final ModuleArrayAdapter adapter = (ModuleArrayAdapter)parent.getAdapter();
		final Intent intent = new Intent(this, ModuleResult.class);
		intent.putExtra(Search.MODULE_ID, adapter.getItem(position).getId());
		startActivity(intent);
	}
}
