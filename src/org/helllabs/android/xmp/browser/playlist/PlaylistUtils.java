package org.helllabs.android.xmp.browser.playlist;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.Xmp;
import org.helllabs.android.xmp.preferences.Preferences;
import org.helllabs.android.xmp.util.Message;
import org.helllabs.android.xmp.util.ModInfo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;


public final class PlaylistUtils {

	private PlaylistUtils() {

	}

	public static void newPlaylistDialog(final Context context) {
		newPlaylistDialog(context, null);
	}

	@SuppressLint("InflateParams")
	public static void newPlaylistDialog(final Context context, final Runnable runnable) {
		final AlertDialog.Builder alert = new AlertDialog.Builder(context);		  
		alert.setTitle("New playlist");  	
		final LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View layout = inflater.inflate(R.layout.newlist, null);

		alert.setView(layout);

		alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {  
			public void onClick(final DialogInterface dialog, final int whichButton) {
				final EditText e1 = (EditText)layout.findViewById(R.id.new_playlist_name);
				final EditText e2 = (EditText)layout.findViewById(R.id.new_playlist_comment);
				final String name = e1.getText().toString();
				final String comment = e2.getText().toString();

				if (createEmptyPlaylist(context, name, comment) && runnable != null) {
					runnable.run();
				}
			}  
		});  

		alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {  
			public void onClick(final DialogInterface dialog, final int whichButton) {  
				// Canceled.  
			}  
		});  

		alert.show();
	}

	/*
	 * Send files to the specified playlist
	 */
	private static void addFiles(final Activity activity, final List<String> fileList, final String playlistName) {
		final List<PlaylistItem> list = new ArrayList<PlaylistItem>();
		final ModInfo modInfo = new ModInfo();
		boolean hasInvalid = false;

		for (final String filename : fileList) {
			if (Xmp.testModule(filename, modInfo)) {
				list.add(new PlaylistItem(modInfo.name, modInfo.type, filename));  // NOPMD
			} else {
				hasInvalid = true;
			}
		}

		if (!list.isEmpty()) {
			Playlist.addToList(activity, playlistName, list);

			if (hasInvalid) {
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (list.size() > 1) {
							Message.toast(activity, R.string.msg_only_valid_files_added);
						} else {
							Message.error(activity, R.string.unrecognized_format);
						}
					}
				});
			}
		}
	}

	public static void filesToPlaylist(final Activity activity, final List<String> fileList, final String playlistName) {

		final ProgressDialog progressDialog = ProgressDialog.show(activity, "Please wait", "Scanning module files...", true);

		new Thread() { 
			public void run() { 	
				addFiles(activity, fileList, playlistName);
				progressDialog.dismiss();
			}
		}.start();
	}

	public static void filesToPlaylist(final Activity activity, final String filename, final String playlistName) {
		final List<String> fileList = new ArrayList<String>();
		fileList.add(filename);
		addFiles(activity, fileList, playlistName);
	}

	public static String[] list() {		
		return Preferences.DATA_DIR.list(new PlaylistFilter());
	}

	public static String[] listNoSuffix() {
		String[] pList = list();
		for (int i = 0; i < pList.length; i++) {
			pList[i] = pList[i].substring(0, pList[i].lastIndexOf(Playlist.PLAYLIST_SUFFIX));	//NOPMD
		}
		return pList;
	}
	
	public static String getPlaylistName(final int index) {
		final String[] pList = list();
		return pList[index].substring(0, pList[index].lastIndexOf(Playlist.PLAYLIST_SUFFIX));
	}
	
	public static boolean createEmptyPlaylist(final Context context, final String name, final String comment) {
		try {
			final Playlist playlist = new Playlist(context, name);
			playlist.setComment(comment);
			playlist.commit();
			return true;
		} catch (IOException e) {
			Message.error(context, context.getString(R.string.error_create_playlist));
			return false;
		}	
	}
}
