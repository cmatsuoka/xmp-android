package org.helllabs.android.xmp.player;

import org.helllabs.android.xmp.R;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class CommentActivity extends Activity {
	TextView commentText;
	
	@Override
	public void onCreate(final Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.comment);
		
		commentText = (TextView)findViewById(R.id.comment_text);
		
		final Bundle bundle = getIntent().getExtras();
		final String comment = bundle.getString("comment");
		commentText.setText(comment);
	}
}
