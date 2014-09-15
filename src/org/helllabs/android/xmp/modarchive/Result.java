package org.helllabs.android.xmp.modarchive;

import org.helllabs.android.xmp.R;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;

public abstract class Result extends ActionBarActivity  {
	

	// Cross-fade
	private View contentView;
	private View progressView;
	private int animationDuration;
	
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(R.string.search_result_title);
	}

	protected void setupCrossfade() {
		// Set up crossfade
		contentView = findViewById(R.id.result_content);
		progressView = findViewById(R.id.result_spinner);
		contentView.setVisibility(View.GONE);
		animationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
	}

	@TargetApi(12)
	protected void crossfade() {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1) {
			contentView.setAlpha(0f);
			contentView.setVisibility(View.VISIBLE);
			contentView.animate().alpha(1f).setDuration(animationDuration).setListener(null);
			progressView.animate().alpha(0f).setDuration(animationDuration).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(final Animator animation) {
					progressView.setVisibility(View.GONE);
				}
			});
		} else {
			progressView.setVisibility(View.GONE);
			contentView.setVisibility(View.VISIBLE);
		}
	}
}
