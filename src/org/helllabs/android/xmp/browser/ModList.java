package org.helllabs.android.xmp.browser;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.helllabs.android.xmp.InfoCache;
import org.helllabs.android.xmp.ModInfo;
import org.helllabs.android.xmp.Preferences;
import org.helllabs.android.xmp.R;

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
	//private boolean isBadDir = false;
	private boolean isPathMenu;
	private ProgressDialog progressDialog;
	private final Handler handler = new Handler();
	private TextView curPath;
	private ImageButton upButton;
	private String currentDir;
	private int directoryNum;
	private int parentNum;
	private int playlistSelection;
	private int fileSelection;
	private int fileNum;
	private Context context;
	private int textColor;
	private ListView listView;

	/*
	 * Add directory to playlist
	 */
	private final DialogInterface.OnClickListener addDirToPlaylistDialogClickListener = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			PlaylistUtils utils = new PlaylistUtils();

			if (which == DialogInterface.BUTTON_POSITIVE) {
				if (playlistSelection >= 0) {
					utils.filesToPlaylist(context, modList.get(fileSelection).filename,
							PlaylistUtils.listNoSuffix()[playlistSelection], false);
				}
			}
		}
	};

	/*
	 * Recursively add current directory to playlist
	 */	
	private final DialogInterface.OnClickListener addCurRecursiveToPlaylistDialogClickListener = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			PlaylistUtils utils = new PlaylistUtils();

			if (which == DialogInterface.BUTTON_POSITIVE) {
				if (playlistSelection >= 0) {
					utils.filesToPlaylist(context, currentDir,
							PlaylistUtils.listNoSuffix()[playlistSelection], true);
				}
			}
		}
	};

	/*
	 * Recursively add directory to playlist
	 */	
	private final DialogInterface.OnClickListener addRecursiveToPlaylistDialogClickListener = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			PlaylistUtils utils = new PlaylistUtils();

			if (which == DialogInterface.BUTTON_POSITIVE) {
				if (playlistSelection >= 0) {
					utils.filesToPlaylist(context, modList.get(fileSelection).filename,
							PlaylistUtils.listNoSuffix()[playlistSelection], true);
				}
			}
		}
	};

	/*
	 * Add Files to playlist
	 */	
	private final DialogInterface.OnClickListener addFileToPlaylistDialogClickListener = new DialogInterface.OnClickListener() {
		public void onClick(final DialogInterface dialog, int which) {
			if (which == DialogInterface.BUTTON_POSITIVE) {
				if (playlistSelection >= 0) {
					boolean invalid = false;
					for (int i = fileSelection; i < fileSelection + fileNum; i++) {
						final PlaylistInfo info = modList.get(i);
						final ModInfo modInfo = new ModInfo();
						if (InfoCache.testModule(info.filename, modInfo)) {
							final String line = info.filename + ":" + modInfo.type + ":" + modInfo.name;
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
	private final DialogInterface.OnClickListener deleteDialogClickListener = new DialogInterface.OnClickListener() {
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

	class DirFilter implements FileFilter {
		public boolean accept(final File dir) {
			return dir.isDirectory();
		}
	}

	class ModFilter implements FilenameFilter {
		public boolean accept(final File dir, final String name) {
			final File file = new File(dir,name);
			return !file.isDirectory();
		}
	}

	@Override
	public void onCreate(final Bundle icicle) {
		super.onCreate(icicle);	
		setContentView(R.layout.modlist);

		listView = (ListView)findViewById(R.id.modlist_listview);
		super.setOnItemClickListener(listView);

		registerForContextMenu(listView);
		final String media_path = prefs.getString(Preferences.MEDIA_PATH, Preferences.DEFAULT_MEDIA_PATH);

		context = this;

		// Set status area background color
		/*LinearLayout statusArea = (LinearLayout)findViewById(R.id.status_area);		
		if (prefs.getBoolean(Preferences.DARK_THEME, false)) {
			statusArea.setBackgroundColor(R.color.dark_theme_status_color);
		}*/

		setTitle("File Browser");

		curPath = (TextView)findViewById(R.id.current_path);
		registerForContextMenu(curPath);


		textColor = curPath.getCurrentTextColor();
		curPath.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction() == MotionEvent.ACTION_UP){
					curPath.setTextColor(textColor);
				}
				else{
					curPath.setTextColor(getResources().getColor(R.color.actionbar_title_color));
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
				if(event.getAction() == MotionEvent.ACTION_UP){
					upButton.setImageResource(R.drawable.parent);
				} else {
					upButton.setImageResource(R.drawable.parent_touch);
				}
				return false;
			}
		});

		// Check if directory exists
		final File modDir = new File(media_path);

		if (!modDir.isDirectory()) {
			final Examples examples = new Examples(this);

			//isBadDir = true;
			final AlertDialog alertDialog = new AlertDialog.Builder(this).create();

			alertDialog.setTitle("Path not found");
			alertDialog.setMessage(media_path + " not found. " +
					"Create this directory or change the module path.");
			alertDialog.setButton("Create", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					examples.install(media_path,
							prefs.getBoolean(Preferences.EXAMPLES, true));
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
			final SharedPreferences.Editor editor = prefs.edit();
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

		//isBadDir = false;
		progressDialog = ProgressDialog.show(this,      
				"Please wait", "Scanning module files...", true);

		final boolean titlesInBrowser = prefs.getBoolean(Preferences.TITLES_IN_BROWSER, false);

		parentNum = directoryNum = 0;
		final File modDir = new File(path);
		new Thread() { 
			public void run() {
				/* if (!path.equals("/")) {
					modList.add(new PlaylistInfo("..", "Parent directory", path + "/..", R.drawable.parent));
					parentNum++;
					directoryNum++;
				} */

				final List<PlaylistInfo> list = new ArrayList<PlaylistInfo>();
				for (final File file : modDir.listFiles(new DirFilter())) {
					directoryNum++;
					list.add(new PlaylistInfo(file.getName(), "Directory",
							file.getAbsolutePath(), R.drawable.folder));
				}
				Collections.sort(list);
				modList.addAll(list);

				final ModInfo info = new ModInfo();

				list.clear();
				for (final File file : modDir.listFiles(new ModFilter())) {
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
						listView.setAdapter(playlist);
					}
				});

				progressDialog.dismiss();
			}
		}.start();
	}

	// Playlist context menu

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (v.equals(curPath)) {
			isPathMenu = true;
			menu.setHeaderTitle("All files");
			menu.add(Menu.NONE, 0, 0, "Add to playlist");
			menu.add(Menu.NONE, 1, 1, "Recursive add to playlist");
			menu.add(Menu.NONE, 2, 2, "Add to play queue");
			menu.add(Menu.NONE, 3, 3, "Set as default path");
			menu.add(Menu.NONE, 4, 4, "Clear cache");

			return;
		}

		isPathMenu = false;

		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;

		if (info.position < parentNum) {
			// Do nothing
		} else if (info.position < directoryNum) {			// For directory
			menu.setHeaderTitle("This directory");
			menu.add(Menu.NONE, 0, 0, "Add to playlist");
			menu.add(Menu.NONE, 1, 1, "Recursive add to playlist");
		} else {											// For files			
			final int mode = Integer.parseInt(prefs.getString(Preferences.PLAYLIST_MODE, "1"));

			menu.setHeaderTitle("This file");
			menu.add(Menu.NONE, 0, 0, "Add to playlist");
			if (mode != 3) {
				menu.add(Menu.NONE, 1, 1, "Add to play queue");
			}
			if (mode != 2) {
				menu.add(Menu.NONE, 2, 2, "Play this file");
			}
			if (mode != 1) {
				menu.add(Menu.NONE, 3, 3, "Play all starting here");
			}
			menu.add(Menu.NONE, 4, 4, "Delete file");
		}

		//menu.setGroupEnabled(1, PlaylistUtils.list().length > 0);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final int id = item.getItemId();

		if (isPathMenu) {
			switch (id) {
			case 0:						// Add all to playlist
				addToPlaylist(directoryNum, modList.size() - directoryNum, addFileToPlaylistDialogClickListener);
				break;
			case 1:						// Recursive add to playlist
				addToPlaylist(2, modList.size() - 2, addCurRecursiveToPlaylistDialogClickListener);
				break;
			case 2:						// Add all to queue
				addToQueue(directoryNum, modList.size() - directoryNum);
				break;
			case 3:						// Set as default path
				SharedPreferences.Editor editor = prefs.edit();
				editor.putString(Preferences.MEDIA_PATH, currentDir);
				editor.commit();
				Message.toast(context, "Set as default module path");
				break;
			case 4:						// Clear cache
				clearCachedEntries(directoryNum, modList.size() - directoryNum);
				break;
			}

			return true;
		}

		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();

		if (info.position < parentNum) {				// Parent dir
			// Do nothing
		} else if (info.position < directoryNum) {		// Directories
			switch (id) {
			case 0:										//    Add to playlist
				addToPlaylist(info.position, 1, addDirToPlaylistDialogClickListener);
				break;
			case 1:										//    Recursive add to playlist
				addToPlaylist(info.position, 1, addRecursiveToPlaylistDialogClickListener);
				break;
			}
		} else {										// Files
			switch (id) {
			case 0:										// Add to playlist
				addToPlaylist(info.position, 1, addFileToPlaylistDialogClickListener);
				break;
			case 1:										// Add to queue
				addToQueue(info.position, 1);
				break;
			case 2:										// Play this module
				playModule(modList.get(info.position).filename);
				break;
			case 3:										// Play all starting here
				playModule(modList, info.position);
				break;
			case 4:										// Delete file
				deleteName = modList.get(info.position).filename;
				Message.yesNoDialog(this, "Delete", "Are you sure to delete " + deleteName + "?", deleteDialogClickListener);
				break;
			}
		}

		return true;
	}

	protected void addToPlaylist(final int start, final int num, final DialogInterface.OnClickListener listener) {
		fileSelection = start;
		fileNum = num;
		playlistSelection = 0;

		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.msg_select_playlist)
		.setPositiveButton(android.R.string.ok, listener)
		.setNegativeButton(android.R.string.cancel, listener)
		.setSingleChoiceItems(PlaylistUtils.listNoSuffix(), 0, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				playlistSelection = which;
			}
		})
		.show();
	}	

	protected void clearCachedEntries(final int start, final int num) {
		for (int i = 0; i < num; i++) {
			final String filename = modList.get(start + i).filename;
			InfoCache.clearCache(filename);
		}
	}

}
