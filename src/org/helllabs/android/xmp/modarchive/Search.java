package org.helllabs.android.xmp.modarchive;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.modarchive.result.ArtistResult;
import org.helllabs.android.xmp.modarchive.result.RandomResult;
import org.helllabs.android.xmp.modarchive.result.TitleResult;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

public class Search extends ActionBarActivity {

	public static final String SEARCH_TEXT = "search_text";
	public static final String MODULE_ID = "module_id";
	public static final String ARTIST_ID = "artist_id";
	public static final String ERROR = "error";
	private RadioGroup searchType;
	private EditText searchEdit;
	private Context context;

	private final View.OnClickListener searchClick = new View.OnClickListener() {
		@Override
		public void onClick(final View view) {
			final int selectedId = searchType.getCheckedRadioButtonId();
			final String searchText = searchEdit.getText().toString();

			Intent intent;

			switch (selectedId) {
			case R.id.title_radio:
				intent = new Intent(context, TitleResult.class);
				intent.putExtra(SEARCH_TEXT, searchText);
				startActivity(intent);
				break;
			case R.id.artist_radio:
				intent = new Intent(context, ArtistResult.class);
				intent.putExtra(SEARCH_TEXT, searchText);
				startActivity(intent);
				break;
			default:
				break;
			}
		}
	};

	private final View.OnClickListener randomClick = new View.OnClickListener() {
		@Override
		public void onClick(final View view) {
			startActivity(new Intent(context, RandomResult.class));
		}
	};

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search);

		setTitle(R.string.search_title);
		context = this;

		final Button searchButton = (Button)findViewById(R.id.search_button);
		final Button randomButton = (Button)findViewById(R.id.random_button);

		searchType = (RadioGroup)findViewById(R.id.search_type);
		searchType.check(R.id.title_radio);

		searchButton.setOnClickListener(searchClick);
		randomButton.setOnClickListener(randomClick);

		searchEdit = (EditText)findViewById(R.id.search_text);
	}

}
