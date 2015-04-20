package org.helllabs.android.xmp.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.view.View;

public class Crossfader {
	
	private final Activity activity;
	private final int animationDuration;
	private View contentView;
	private View progressView;
	
	
	public Crossfader(final Activity activity) {
		this.activity = activity;
		this.animationDuration = activity.getResources().getInteger(android.R.integer.config_shortAnimTime);
	}
	
	public void setup(final int contentRes, final int spinnerRes) {	
		contentView = activity.findViewById(contentRes);
		progressView = activity.findViewById(spinnerRes);
		contentView.setVisibility(View.GONE); 
	}
	
	@TargetApi(12)
	public void crossfade() {

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1) {

			// Set the content view to 0% opacity but visible, so that it is visible
			// (but fully transparent) during the animation.
			contentView.setAlpha(0f);
			contentView.setVisibility(View.VISIBLE);

			// Animate the content view to 100% opacity, and clear any animation
			// listener set on the view.
			contentView.animate()
				.alpha(1f)
				.setDuration(animationDuration)
				.setListener(null);

			// Animate the loading view to 0% opacity. After the animation ends,
			// set its visibility to GONE as an optimization step (it won't
			// participate in layout passes, etc.)
			progressView.animate()
				.alpha(0f)
				.setDuration(animationDuration)
				.setListener(new AnimatorListenerAdapter() {
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
