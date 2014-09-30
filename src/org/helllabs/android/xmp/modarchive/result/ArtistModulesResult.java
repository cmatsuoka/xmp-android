package org.helllabs.android.xmp.modarchive.result;

import java.io.UnsupportedEncodingException;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.modarchive.Search;
import org.helllabs.android.xmp.modarchive.adapter.ModuleArrayAdapter;
import org.helllabs.android.xmp.modarchive.request.ModuleRequest;
import org.helllabs.android.xmp.modarchive.response.HardErrorResponse;
import org.helllabs.android.xmp.modarchive.response.ModArchiveResponse;
import org.helllabs.android.xmp.modarchive.response.ModuleResponse;
import org.helllabs.android.xmp.modarchive.response.SoftErrorResponse;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

public class ArtistModulesResult extends Result implements ModuleRequest.OnResponseListener, ListView.OnItemClickListener {

	private ListView list;
	private TextView errorMessage;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.result_list);
		setupCrossfade();

		setTitle(R.string.search_artist_modules_title);

		list = (ListView)findViewById(R.id.result_list);
		list.setOnItemClickListener(this);
		
		errorMessage = (TextView)findViewById(R.id.error_message);

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
	public void onResponse(final ModArchiveResponse response) {
		final ModuleResponse moduleList = (ModuleResponse)response;
		final ModuleArrayAdapter adapter = new ModuleArrayAdapter(this, R.layout.search_list_item, moduleList.getList());
		list.setAdapter(adapter);
		
		if (moduleList.isEmpty()) {
			errorMessage.setText(R.string.search_artist_no_mods);
			list.setVisibility(View.GONE);
		}
		
		crossfade();
	}

	@Override
	public void onSoftError(final SoftErrorResponse response) {
		final TextView errorMessage = (TextView)findViewById(R.id.error_message);
		errorMessage.setText(response.getMessage());
		list.setVisibility(View.GONE);
		crossfade();
	}

	@Override
	public void onHardError(final HardErrorResponse response) {
		handleError(response.getError());
	}

	@Override
	public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
		final ModuleArrayAdapter adapter = (ModuleArrayAdapter)parent.getAdapter();
		final Intent intent = new Intent(this, ModuleResult.class);
		intent.putExtra(Search.MODULE_ID, adapter.getItem(position).getId());
		startActivity(intent);
	}
}
