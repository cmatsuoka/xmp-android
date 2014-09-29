package org.helllabs.android.xmp.browser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.Xmp;
import org.helllabs.android.xmp.browser.model.PlaylistItem;
import org.helllabs.android.xmp.preferences.Preferences;
import org.helllabs.android.xmp.util.Message;
import org.helllabs.android.xmp.util.ModInfo;

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
	
	public static void newPlaylistDialog(final Context context, final Runnable runnable) {
		final AlertDialog.Builder alert = new AlertDialog.Builder(context);		  
		alert.setTitle("New playlist");  	
	    final LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    final View layout = inflater.inflate(R.layout.newlist, null);

	    alert.setView(layout);
		  
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {  
			public void onClick(final DialogInterface dialog, final int whichButton) {
			    final EditText e1 = (EditText)layout.findViewById(R.id.new_playlist_name);
				final EditText e2 = (EditText)layout.findViewById(R.id.new_playlist_comment);
				final String name = e1.getText().toString();
				final String comment = e2.getText().toString();
				
				try {
					final Playlist playlist = new Playlist(context, name);
					playlist.setComment(comment);
					playlist.commit();
					
					if (runnable != null) {
						runnable.run();
					}
				} catch (IOException e) {
					Message.error(context, context.getString(R.string.error_create_playlist));
				}
			}  
		});  
		  
		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {  
			public void onClick(final DialogInterface dialog, final int whichButton) {  
				// Canceled.  
			}  
		});  
		  
		alert.show();
	}
	
	/*
	 * Send files in directory to the specified playlist
	 */
	
	private static void addFiles(final Context context, final String path, final String name, final boolean recursive)
	{
		final List<PlaylistItem> list = new ArrayList<PlaylistItem>();
		final ModInfo modInfo = new ModInfo();
		final File modDir = new File(path);
		
		int num = 0;
    	for (final File file : modDir.listFiles()) {
    		if (file.isDirectory()) {
    			if (recursive) {
    				addFiles(context, file.getPath(), name, true);
    			} else {
    				continue;
    			}
    		}
    		final String filename = path + "/" + file.getName();
    		if (!Xmp.testModule(filename, modInfo)) {
    			continue;
    		}
    		list.add(new PlaylistItem(modInfo.name, modInfo.type, filename));
    		num++;
    	}
    	
    	if (num > 0) {
    		Playlist.addToList(context, name, list);
    	}
	}
	
	public static void filesToPlaylist(final Context context, final String path, final String name, final boolean recursive) {
		final File modDir = new File(path);
		
		if (!modDir.isDirectory()) {
			Message.error(context, context.getString(R.string.error_exist_dir));
			return;
		}
		
		final ProgressDialog progressDialog = ProgressDialog.show(context,      
				"Please wait", "Scanning module files...", true);
		
		new Thread() { 
			public void run() { 	
				addFiles(context, path, name, recursive);
                progressDialog.dismiss();
			}
		}.start();
	}
	
	public static String[] list() {		
		return Preferences.DATA_DIR.list(new PlayListFilter());
	}
	
	public static String[] listNoSuffix() {
		String[] pList = list();
		for (int i = 0; i < pList.length; i++) {
			pList[i] = pList[i].substring(0, pList[i].lastIndexOf(Playlist.PLAYLIST_SUFFIX));
		}
		return pList;
	}
}
