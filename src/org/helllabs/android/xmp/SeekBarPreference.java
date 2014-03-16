package org.helllabs.android.xmp;

/* The following code was written by Matthew Wiggins 
 * and is released under the APACHE 2.0 license 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;


public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener
{
	private SeekBar mSeekBar;
	private TextView mValueText;
	private final Context mContext;

	private final String mDialogMessage, mSuffix;
	private final int mDefault;
	private int mMax, mValue;

	public SeekBarPreference(final Context context, final AttributeSet attrs) { 
		super(context,attrs); 
		mContext = context;

		final TypedArray styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.SeekBarPreference);
		mDialogMessage = styledAttrs.getString(R.styleable.SeekBarPreference_android_dialogMessage);
		mSuffix = styledAttrs.getString(R.styleable.SeekBarPreference_android_text);
		mDefault = styledAttrs.getInt(R.styleable.SeekBarPreference_android_defaultValue, 0);
		mMax = styledAttrs.getInt(R.styleable.SeekBarPreference_android_max, 100);
		styledAttrs.recycle();
	}
	
	@Override 
	protected View onCreateDialogView() {
		LinearLayout.LayoutParams params;
		final LinearLayout layout = new LinearLayout(mContext);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setPadding(6,6,6,6);

		final TextView splashText = new TextView(mContext);
		if (mDialogMessage != null) {
			splashText.setText(mDialogMessage);
		}
		layout.addView(splashText);

		mValueText = new TextView(mContext);
		mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
		mValueText.setTextSize(32);
		params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT, 
				LinearLayout.LayoutParams.WRAP_CONTENT);
		layout.addView(mValueText, params);

		mSeekBar = new SeekBar(mContext);
		mSeekBar.setOnSeekBarChangeListener(this);
		layout.addView(mSeekBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

		if (shouldPersist()) {
			mValue = getPersistedInt(mDefault);
		}

		mSeekBar.setMax(mMax);
		mSeekBar.setProgress(mValue);
		
		return layout;
	}
	
	@Override 
	protected void onBindDialogView(final View view) {
		super.onBindDialogView(view);
		mSeekBar.setMax(mMax);
		mSeekBar.setProgress(mValue);
	}
	
	@Override
	protected void onSetInitialValue(final boolean restore, final Object defaultValue) {
		super.onSetInitialValue(restore, defaultValue);
		if (restore) {
			mValue = shouldPersist() ? getPersistedInt(mDefault) : 0;
		} else { 
			mValue = (Integer)defaultValue;
		}
	}

	public void onProgressChanged(final SeekBar seek, final int value, final boolean fromTouch) {
		final String str = String.valueOf(value);
		mValueText.setText(mSuffix == null ? str : str.concat(mSuffix));
		if (shouldPersist()) {
			persistInt(value);
		}
		callChangeListener(Integer.valueOf(value));
	}
	
	public void onStartTrackingTouch(final SeekBar seek) {
		// do nothing
	}
	
	public void onStopTrackingTouch(final SeekBar seek) {
		// do nothing
	}

	public void setMax(final int max) {
		mMax = max;
	}
	
	public int getMax() {
		return mMax;
	}

	public void setProgress(final int progress) { 
		mValue = progress;
		if (mSeekBar != null) {
			mSeekBar.setProgress(progress);
		}
	}
	
	public int getProgress() {
		return mValue;
	}
}