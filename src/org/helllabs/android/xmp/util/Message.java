package org.helllabs.android.xmp.util;

import org.helllabs.android.xmp.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

public final class Message {
	
	private Message() {
		
	}
	
	public static void fatalError(final Context context, final String message, final Activity activity) {
		final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
		
		alertDialog.setTitle(R.string.error);
		alertDialog.setMessage(message);
		alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, context.getString(R.string.exit), new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, final int which) {
				dialog.dismiss();
				activity.finish();
			}
		});
		alertDialog.show();		
	}
	
	public static void error(final Context context, final String message) {
		final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
		
		alertDialog.setTitle(R.string.error);
		alertDialog.setMessage(message);
		alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, context.getString(R.string.dismiss), new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, final int which) {
				//
			}
		});
		alertDialog.show();		
	}
	
	public static void error(final Context context, final int resId) {
		error(context, context.getString(resId));
	}
		
	
	public static void toast(final Context context, final String message) {
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();		
	}
	
	public static void toast(final Context context, final int resId) {
		toast(context, context.getString(resId));
	}
	
	public static void yesNoDialog(final Context context, final String title, final String message, final Runnable runnable) {
		
		final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					runnable.run();
				}
			}
		};
		
		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(title)
			.setMessage(message)
			.setPositiveButton(android.R.string.yes, listener)
		    .setNegativeButton(android.R.string.no, listener)
		    .show();		
	}
}
