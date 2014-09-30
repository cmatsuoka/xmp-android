package org.helllabs.android.xmp.modarchive.result;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.modarchive.Search;
import org.helllabs.android.xmp.modarchive.adapter.ArtistArrayAdapter;
import org.helllabs.android.xmp.modarchive.model.Artist;
import org.helllabs.android.xmp.modarchive.request.ArtistRequest;
import org.helllabs.android.xmp.modarchive.request.ModArchiveRequest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class ArtistResult extends Result implements ArtistRequest.OnResponseListener<List<Artist>>, ListView.OnItemClickListener {

	private ListView list;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.result_list);
		setupCrossfade();

		setTitle(R.string.search_artist_title);

		list = (ListView)findViewById(R.id.result_list);
		list.setOnItemClickListener(this);

		final String searchText = getIntent().getStringExtra(Search.SEARCH_TEXT);
		final String key = getString(R.string.modarchive_apikey);

		try {
			final ArtistRequest request = new ArtistRequest(key, ModArchiveRequest.ARTIST, searchText);
			request.setOnResponseListener(this).send();
		} catch (UnsupportedEncodingException e) {
			handleQueryError();
		}
	}

	@Override
	public void onResponse(final List<Artist> response) {
		final ArtistArrayAdapter adapter = new ArtistArrayAdapter(this, android.R.layout.simple_list_item_1, response);
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
		final ArtistArrayAdapter adapter = (ArtistArrayAdapter)parent.getAdapter();
		final Intent intent = new Intent(this, ArtistModulesResult.class);
		intent.putExtra(Search.ARTIST_ID, adapter.getItem(position).getId());
		startActivity(intent);
	}
}
