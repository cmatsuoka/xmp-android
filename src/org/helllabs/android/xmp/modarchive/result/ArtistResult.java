package org.helllabs.android.xmp.modarchive.result;

import java.util.List;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.modarchive.Search;
import org.helllabs.android.xmp.modarchive.model.Artist;
import org.helllabs.android.xmp.modarchive.request.ArtistRequest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ArtistResult extends Result implements ArtistRequest.OnResponseListener<List<Artist>>, ListView.OnItemClickListener {
	
	private ListView list;
	private List<Artist> artistList;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.result_list);
		setupCrossfade();
		
		list = (ListView)findViewById(R.id.result_list);
		
		final String searchText = getIntent().getStringExtra(Search.SEARCH_TEXT);
		
		final String key = getString(R.string.modarchive_apikey);
		final ArtistRequest request = new ArtistRequest(key, "search_artist&query=" + searchText);
		request.setOnResponseListener(this).send();
		
		list.setOnItemClickListener(this);
	}
	
	@Override
	public void onResponse(final List<Artist> response) {
		artistList = response;
		final ArrayAdapter<Artist> adapter = new ArrayAdapter<Artist>(this, android.R.layout.simple_list_item_1, response);
		list.setAdapter(adapter);
		crossfade();
	}
	
	@Override
	public void onError(final Throwable error) {
		handleError(error);
	}

	@Override
	public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
		final Intent intent = new Intent(this, ArtistModulesResult.class);
		intent.putExtra(Search.ARTIST_ID, artistList.get(position).getId());
		startActivity(intent);
	}

}
