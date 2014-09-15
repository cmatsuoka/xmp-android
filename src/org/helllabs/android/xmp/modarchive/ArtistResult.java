package org.helllabs.android.xmp.modarchive;

import java.util.List;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.modarchive.model.Artist;
import org.helllabs.android.xmp.modarchive.request.ArtistRequest;

import android.content.Context;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ArtistResult extends Result implements ArtistRequest.OnResponseListener<List<Artist>> {
	
	private Context context;
	private ListView list;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.result_artist);
		
		context = this;
		list = (ListView)findViewById(R.id.artist_list);
		
		final String search = getIntent().getStringExtra(Search.SEARCH);
		
		final String key = getString(R.string.modarchive_apikey);
		final ArtistRequest request = new ArtistRequest(key, "search_artist&query=" + search);
		request.setOnResponseListener(this).send();
	}
	
	@Override
	public void onResponse(final List<Artist> response) {	
		final ArrayAdapter<Artist> adapter = new ArrayAdapter<Artist>(context, android.R.layout.simple_list_item_1, response);
		list.setAdapter(adapter);
	}

}
