package org.helllabs.android.xmp;
import android.app.AlertDialog;
import android.content.Context;
import android.text.method.SingleLineTransformationMethod;
import android.widget.EditText;
import android.widget.LinearLayout;


public class InputDialog extends AlertDialog.Builder {
	private float scale;
	public EditText input;

	@SuppressWarnings("deprecation")
	protected InputDialog(Context context) {
		super(context);
		
		scale = context.getResources().getDisplayMetrics().density;
		final LinearLayout layout = new LinearLayout(context);
		final int pad = (int)(scale * 6);
		layout.setPadding(pad, pad, pad, pad);
		
		input = new EditText(context);
		input.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));
		input.setTransformationMethod(new SingleLineTransformationMethod());
		layout.addView(input);		
		setView(layout);
	}
}
