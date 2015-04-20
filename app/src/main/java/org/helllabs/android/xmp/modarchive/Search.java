package org.helllabs.android.xmp.modarchive;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.modarchive.result.ArtistResult;
import org.helllabs.android.xmp.modarchive.result.RandomResult;
import org.helllabs.android.xmp.modarchive.result.TitleResult;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

public class Search extends ActionBarActivity implements TextView.OnEditorActionListener {

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
			performSearch();
		}
	};

	private final View.OnClickListener randomClick = new View.OnClickListener() {
		@Override
		public void onClick(final View view) {
			startActivity(new Intent(context, RandomResult.class));
			overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
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
		searchEdit.setOnEditorActionListener(this);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		// Show soft keyboard
		searchEdit.requestFocus();
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
	}
	
	@Override
    public boolean onEditorAction(final TextView view, final int actionId, final KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            performSearch();
            return true;
        }
        return false;
    }
	
	private void performSearch() {
		final int selectedId = searchType.getCheckedRadioButtonId();
		final String searchText = searchEdit.getText().toString().trim();

		Intent intent;

		switch (selectedId) {
		case R.id.title_radio:
			intent = new Intent(context, TitleResult.class);
			intent.putExtra(SEARCH_TEXT, searchText);
			startActivity(intent);
			overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
			break;
		case R.id.artist_radio:
			intent = new Intent(context, ArtistResult.class);
			intent.putExtra(SEARCH_TEXT, searchText);
			startActivity(intent);
			overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
			break;
		default:
			break;
		}
	}

}
