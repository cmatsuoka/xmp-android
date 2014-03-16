package org.helllabs.android.xmp.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

public final class Message {
	
	private Message() {
		
	}
	
	public static void fatalError(final Context context, final String message, final Activity a) {
		final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
		
		alertDialog.setTitle("Error");
		alertDialog.setMessage(message);
		alertDialog.setButton("Exit", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				a.finish();
			}
		});
		alertDialog.show();		
	}
	
	public static void error(final Context context, final String message) {
		final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
		
		alertDialog.setTitle("Error");
		alertDialog.setMessage(message);
		alertDialog.setButton("Dismiss", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				//
			}
		});
		alertDialog.show();		
	}
		
	
	public static void toast(final Context context, final String message) {
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();		
	}
	
	public static void yesNoDialog(final Context context, final String title, final String message, DialogInterface.OnClickListener listener) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(title)
			.setMessage(message)
			.setPositiveButton(android.R.string.yes, listener)
		    .setNegativeButton(android.R.string.no, listener)
		    .show();		
	}
}
