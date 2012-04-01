/* Xmp for Android
 * Copyright (C) 2010 Claudio Matsuoka
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.helllabs.android.xmp;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

public class ModList extends PlaylistActivity {
	boolean isBadDir = false;
	boolean isPathMenu;
	ProgressDialog progressDialog;
	final Handler handler = new Handler();
	TextView curPath;
	ImageButton upButton;
	String currentDir;
	int directoryNum;
	int parentNum;
	int playlistSelection;
	int fileSelection;
	int fileNum;
	Context context;
	int textColor;
	
	class DirFilter implements FileFilter {
	    public boolean accept(File dir) {
	        return dir.isDirectory();
	    }
	}
	
	class ModFilter implements FilenameFilter {
	    public boolean accept(File dir, String name) {
	    	File f = new File(dir,name);
	        return !f.isDirectory();
	    }
	}
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);	
		setContentView(R.layout.modlist);
		
		registerForContextMenu(getListView());
		final String media_path = prefs.getString(Settings.PREF_MEDIA_PATH, Settings.DEFAULT_MEDIA_PATH);
		
		context = this;

		setTitle("File Browser");
		
		curPath = (TextView)findViewById(R.id.current_path);
		registerForContextMenu(curPath);
		
		textColor = curPath.getCurrentTextColor();
		curPath.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction() == (MotionEvent.ACTION_UP)){
					curPath.setTextColor(textColor);
				}
				else{
					curPath.setTextColor(R.color.actionbar_title_color);
				}
				return false;
			}
		});
		
		upButton = (ImageButton)findViewById(R.id.up_button);
		upButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final File file = new File(currentDir + "/.");
				String name;
				if ((name = file.getParentFile().getParent()) == null) {
						name = "/";
				}
				updateModlist(name);
			}
		});
		
		upButton.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction() == (MotionEvent.ACTION_UP)){
					upButton.setImageResource(R.drawable.parent);
				}
				else{
					upButton.setImageResource(R.drawable.parent_touch);
				}
				return false;
			}
		});
		
		// Check if directory exists
		final File modDir = new File(media_path);
		
		if (!modDir.isDirectory()) {
			final Examples examples = new Examples(this);
			
			isBadDir = true;
			AlertDialog alertDialog = new AlertDialog.Builder(this).create();
			
			alertDialog.setTitle("Path not found");
			alertDialog.setMessage(media_path + " not found. " +
					"Create this directory or change the module path.");
			alertDialog.setButton("Create", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					examples.install(media_path,
							prefs.getBoolean(Settings.PREF_EXAMPLES, true));
					updateModlist(media_path);
				}
			});
			alertDialog.setButton2("Back", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			});
			alertDialog.show();
			return;
		}
		
		shuffleMode = prefs.getBoolean("options_shuffleMode", true);
		loopMode = prefs.getBoolean("options_loopMode", false);
		setupButtons();
		
		updateModlist(media_path);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (modifiedOptions) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean("options_shuffleMode", shuffleMode);
			editor.putBoolean("options_loopMode", loopMode);
			editor.commit();
		}
	}

	public void update() {
		updateModlist(currentDir);
	}
	
	public void updateModlist(final String path) {
		modList.clear();
		
		currentDir = path;
		curPath.setText(path);
		
		isBadDir = false;
		progressDialog = ProgressDialog.show(this,      
				"Please wait", "Scanning module files...", true);
		
		final boolean titlesInBrowser = prefs.getBoolean(Settings.PREF_TITLES_IN_BROWSER, false);

		parentNum = directoryNum = 0;
		final File modDir = new File(path);
		new Thread() { 
			public void run() {
				/* if (!path.equals("/")) {
					modList.add(new PlaylistInfo("..", "Parent directory", path + "/..", R.drawable.parent));
					parentNum++;
					directoryNum++;
				} */
				
				List<PlaylistInfo> list = new ArrayList<PlaylistInfo>();
            	for (File file : modDir.listFiles(new DirFilter())) {
            		directoryNum++;
            		list.add(new PlaylistInfo(file.getName(), "Directory",
            						file.getAbsolutePath(), R.drawable.folder));
            	}
            	Collections.sort(list);
            	modList.addAll(list);
            	
            	ModInfo info = new ModInfo();
            	
            	list.clear();
            	for (File file : modDir.listFiles(new ModFilter())) {
            		final String filename = path + "/" + file.getName();
            		final String date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
            				DateFormat.MEDIUM).format(file.lastModified());
            		
            		if (titlesInBrowser && !file.isDirectory()) {
            			if (InfoCache.testModule(filename, info)) {
            				list.add(new PlaylistInfo(info.name, info.type, filename));
            			}
            		} else {
            			final String name = file.getName();
            			final String comment = date + String.format(" (%d kB)", file.length() / 1024);
            			list.add(new PlaylistInfo(name, comment, filename));
            		}
            	}
            	Collections.sort(list);
            	modList.addAll(list);
            	
                final PlaylistInfoAdapter playlist = new PlaylistInfoAdapter(ModList.this,
                			R.layout.song_item, R.id.info, modList, false);
                
                /* This one must run in the UI thread */
                handler.post(new Runnable() {
                	public void run() {
                		 setListAdapter(playlist);
                	 }
                });
            	
                progressDialog.dismiss();
			}
		}.start();
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		String name = modList.get(position).filename;
		File file = new File(name);
		
		if (file.isDirectory()) {	
			if (file.getName().equals("..")) {
				if ((name = file.getParentFile().getParent()) == null)
					name = "/";
			}
			updateModlist(name);
		} else {
			super.onListItemClick(l, v, position, id);
		}
	}
	
	// Playlist context menu
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (v.equals(curPath)) {
			isPathMenu = true;
			menu.setHeaderTitle("All files");
			menu.add(Menu.NONE, 0, 0, "Add to playlist");
			menu.add(Menu.NONE, 1, 1, "Add to play queue");
			menu.add(Menu.NONE, 2, 2, "Set as default path");
			menu.add(Menu.NONE, 3, 3, "Clear cache");

			return;
		}
		
		isPathMenu = false;
		
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
		
		if (info.position < parentNum) {
			// Do nothing
		} else if (info.position < directoryNum) {			// For directory
			menu.setHeaderTitle("This directory");
			menu.add(Menu.NONE, 0, 0, "Add to playlist");		
		} else {											// For files
			menu.setHeaderTitle("This file");
			menu.add(Menu.NONE, 0, 0, "Add to playlist");
			menu.add(Menu.NONE, 1, 1, "Add to play queue");
			menu.add(Menu.NONE, 2, 2, "Delete file");
		}
		
		//menu.setGroupEnabled(1, PlaylistUtils.list().length > 0);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		final int id = item.getItemId();
		
		if (isPathMenu) {
			switch (id) {
			case 0:						// Add all to playlist
				addToPlaylist(directoryNum, modList.size() - directoryNum, addFileToPlaylistDialogClickListener);
				break;
			case 1:						// Add all to queue
				addToQueue(directoryNum, modList.size() - directoryNum);
				break;
			case 2:						// Set as default path
				SharedPreferences.Editor editor = prefs.edit();
				editor.putString(Settings.PREF_MEDIA_PATH, currentDir);
				editor.commit();
				Message.toast(context, "Set as default module path");
				break;
			case 3:						// Clear cache
				clearCachedEntries(directoryNum, modList.size() - directoryNum);
				break;
			}
			
			return true;
		}

		if (info.position < parentNum) {				// Parent dir
			// Do nothing
		} else if (info.position < directoryNum) {		// Directories
			if (id == 0) {
				addToPlaylist(info.position, 1, addDirToPlaylistDialogClickListener);
			}
		} else {										// Files
			switch (id) {
			case 0:										// Add to playlist
				addToPlaylist(info.position, 1, addFileToPlaylistDialogClickListener);
				break;
			case 1:										// Add to queue
				addToQueue(info.position, 1);
				break;
			case 2:										// Delete file
				deleteName = modList.get(info.position).filename;
				Message.yesNoDialog(this, "Delete", "Are you sure to delete " + deleteName + "?", deleteDialogClickListener);
				break;
			}
		}

		return true;
	}
	
	protected void addToPlaylist(int start, int num, DialogInterface.OnClickListener listener) {
		fileSelection = start;
		fileNum = num;
		playlistSelection = 0;
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.msg_select_playlist)
		.setPositiveButton(R.string.msg_ok, listener)
	    .setNegativeButton(R.string.msg_cancel, listener)
	    .setSingleChoiceItems(PlaylistUtils.listNoSuffix(), 0, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int which) {
		        playlistSelection = which;
		    }
		})
	    .show();
	}	
	
	protected void clearCachedEntries(int start, int num) {
		for (int i = 0; i < num; i++) {
			final String filename = modList.get(start + i).filename;
			InfoCache.clearCache(filename);
		}
	}
	
	/*
	 * Add directory to playlist
	 */
	private DialogInterface.OnClickListener addDirToPlaylistDialogClickListener = new DialogInterface.OnClickListener() {
	    public void onClick(DialogInterface dialog, int which) {
	    	PlaylistUtils p = new PlaylistUtils();
	    	
	        if (which == DialogInterface.BUTTON_POSITIVE) {
	        	if (playlistSelection >= 0) {
	        		p.filesToPlaylist(context, modList.get(fileSelection).filename,
	        					PlaylistUtils.listNoSuffix()[playlistSelection]);
	        	}
	        }
	    }
	};
	
	/*
	 * Add Files to playlist
	 */	
	private DialogInterface.OnClickListener addFileToPlaylistDialogClickListener = new DialogInterface.OnClickListener() {
	    public void onClick(DialogInterface dialog, int which) {
	        if (which == DialogInterface.BUTTON_POSITIVE) {
	        	if (playlistSelection >= 0) {
	        		boolean invalid = false;
	        		for (int i = fileSelection; i < fileSelection + fileNum; i++) {
	        			final PlaylistInfo pi = modList.get(i);
	        			ModInfo modInfo = new ModInfo();
	        			if (InfoCache.testModule(pi.filename, modInfo)) {
	        				String line = pi.filename + ":" + modInfo.type + ":" + modInfo.name;
	        				PlaylistUtils.addToList(context, PlaylistUtils.listNoSuffix()[playlistSelection], line);
	        			} else {
	        				invalid = true;
	        			}
	        		}
	        		if (invalid) {
	        			if (fileNum > 1) {
	        				Message.toast(context, "Only valid files were added to playlist");
	        			} else {
	        				Message.error(context, "Unrecognized file format");
	        			}
	        		}
	        	}
	        }
	    }
	};
	
	/*
	 * Delete file
	 */
	private DialogInterface.OnClickListener deleteDialogClickListener = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			if (which == DialogInterface.BUTTON_POSITIVE) {
				if (InfoCache.delete(deleteName)) {
					updateModlist(currentDir);
					Message.toast(context, getString(R.string.msg_file_deleted));
				} else {
					Message.toast(context, getString(R.string.msg_cant_delete));
				}
			}
		}
	};
}
