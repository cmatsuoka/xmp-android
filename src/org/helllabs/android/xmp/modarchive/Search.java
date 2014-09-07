package org.helllabs.android.xmp.modarchive;

import org.helllabs.android.xmp.R;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;

public class Search extends ActionBarActivity {
	
	private RadioGroup searchType;
	
	private final View.OnClickListener searchClick = new View.OnClickListener() {
		@Override
		public void onClick(final View view) {
			final int selectedId = searchType.getCheckedRadioButtonId();
			
			switch (selectedId) {
			case R.id.title_radio:
				break;
			case R.id.artist_radio:
				break;
			default:
				break;
			}
		}
	};
	
	private final View.OnClickListener randomClick = new View.OnClickListener() {
		@Override
		public void onClick(final View view) {
			
		}
	};
	
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search);
		
		setTitle("Module search");
		
		final Button searchButton = (Button)findViewById(R.id.search_button);
		final Button randomButton = (Button)findViewById(R.id.random_button);
		
		searchType = (RadioGroup)findViewById(R.id.search_type);
		searchType.check(R.id.title_radio);
		
		searchButton.setOnClickListener(searchClick);
		randomButton.setOnClickListener(randomClick);
	}

}
