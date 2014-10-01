package org.helllabs.android.xmp.browser;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.browser.adapter.PlaylistAdapter;
import org.helllabs.android.xmp.browser.model.PlaylistItem;
import org.helllabs.android.xmp.browser.playlist.PlaylistUtils;
import org.helllabs.android.xmp.preferences.Preferences;
import org.helllabs.android.xmp.util.Crossfader;
import org.helllabs.android.xmp.util.FileUtils;
import org.helllabs.android.xmp.util.InfoCache;
import org.helllabs.android.xmp.util.Log;
import org.helllabs.android.xmp.util.Message;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

public class FilelistActivity extends BasePlaylistActivity {
	private static final String TAG = "BasePlaylistActivity";
	private static final String OPTIONS_SHUFFLE_MODE = "options_shuffleMode";
	private static final String OPTIONS_LOOP_MODE = "options_loopMode";
	private static final boolean DEFAULT_SHUFFLE_MODE = true;
	private static final boolean DEFAULT_LOOP_MODE = false;

	private Stack<String> mPathStack;

	private boolean isPathMenu;
	private TextView curPath;
	private String currentDir;
	private String deleteName;
	private int directoryNum;
	private int parentNum;
	private int playlistSelection;
	private int fileSelection;
	private Context context;
	private boolean mLoopMode;
	private boolean mShuffleMode;
	private boolean mBackButtonParentdir;
	private Crossfader crossfade;

	@Override
	protected void setShuffleMode(final boolean shuffleMode) {
		mShuffleMode = shuffleMode;
	}

	@Override
	protected void setLoopMode(final boolean loopMode) {
		mLoopMode = loopMode;
	}


	@Override
	protected boolean isShuffleMode() {
		return mShuffleMode;
	}

	@Override
	protected boolean isLoopMode() {
		return mLoopMode;
	}

//	/*
//	 * Add directory to playlist
//	 */
//	private final DialogInterface.OnClickListener addDirToPlaylistDialogClickListener = new DialogInterface.OnClickListener() {
//		public void onClick(final DialogInterface dialog, final int which) {
//			if (which == DialogInterface.BUTTON_POSITIVE) {
//				if (playlistSelection >= 0) {
//					PlaylistUtils.filesToPlaylist(context, playlistAdapter.getFilename(fileSelection),
//							PlaylistUtils.getPlaylistName(playlistSelection));
//				}
//			}
//		}
//	};

	/*
	 * Recursively add current directory to playlist
	 */	
	private final DialogInterface.OnClickListener addCurRecursiveToPlaylistDialogClickListener = new DialogInterface.OnClickListener() {
		public void onClick(final DialogInterface dialog, final int which) {
			if (which == DialogInterface.BUTTON_POSITIVE) {
				if (playlistSelection >= 0) {
					PlaylistUtils.filesToPlaylist(FilelistActivity.this, recursiveList(currentDir),
							PlaylistUtils.getPlaylistName(playlistSelection));
				}
			}
		}
	};

	/*
	 * Recursively add directory to playlist
	 */	
	private final DialogInterface.OnClickListener addRecursiveToPlaylistDialogClickListener = new DialogInterface.OnClickListener() {
		public void onClick(final DialogInterface dialog, final int which) {
			if (which == DialogInterface.BUTTON_POSITIVE) {
				if (playlistSelection >= 0) {
					PlaylistUtils.filesToPlaylist(FilelistActivity.this, recursiveList(playlistAdapter.getFilename(fileSelection)),
							PlaylistUtils.getPlaylistName(playlistSelection));
				}
			}
		}
	};

	/*
	 * Add one file to playlist
	 */	
	private final DialogInterface.OnClickListener addFileToPlaylistDialogClickListener = new DialogInterface.OnClickListener() {
		public void onClick(final DialogInterface dialog, final int which) {
			if (which == DialogInterface.BUTTON_POSITIVE) {
				if (playlistSelection >= 0) {
					PlaylistUtils.filesToPlaylist(FilelistActivity.this, playlistAdapter.getFilename(fileSelection),
							PlaylistUtils.getPlaylistName(playlistSelection));
				}
			}
		}
	};

	/*
	 * Add file list to playlist
	 */	
	private final DialogInterface.OnClickListener addFileListToPlaylistDialogClickListener = new DialogInterface.OnClickListener() {
		public void onClick(final DialogInterface dialog, final int which) {
			if (which == DialogInterface.BUTTON_POSITIVE) {
				if (playlistSelection >= 0) {
					PlaylistUtils.filesToPlaylist(FilelistActivity.this, playlistAdapter.getFilenameList(fileSelection),
							PlaylistUtils.getPlaylistName(playlistSelection));
				}
			}
		}
	};
	
	/*
	 * Delete file
	 */
	private final DialogInterface.OnClickListener deleteDialogClickListener = new DialogInterface.OnClickListener() {
		public void onClick(final DialogInterface dialog, final int which) {
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

	/*
	 * Delete file
	 */
	private final DialogInterface.OnClickListener deleteDirectoryDialogClickListener = new DialogInterface.OnClickListener() {
		public void onClick(final DialogInterface dialog, final int which) {
			if (which == DialogInterface.BUTTON_POSITIVE) {
				if (InfoCache.deleteRecursive(deleteName)) {
					updateModlist(currentDir);
					Message.toast(context, getString(R.string.msg_dir_deleted));
				} else {
					Message.toast(context, getString(R.string.msg_cant_delete_dir));
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
	protected void onListItemClick(final AdapterView<?> list, final View view, final int position, final long id) {
		String name = playlistAdapter.getFilename(position);
		final File file = new File(name);

		if (file.isDirectory()) {
			if (file.getName().equals("..")) {
				name = file.getParentFile().getParent();
				if (name == null) {
					name = "/";
				}
			}
			mPathStack.push(name);
			updateModlist(name);
		} else {
			super.onListItemClick(list, view, position, id);
		}
	}

	private void pathNotFound(final String media_path) {

		final AlertDialog alertDialog = new AlertDialog.Builder(this).create();

		alertDialog.setTitle("Path not found");
		alertDialog.setMessage(media_path + " not found. " +
				"Create this directory or change the module path.");
		alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Create", new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, final int which) {
				Examples.install(context, media_path, mPrefs.getBoolean(Preferences.EXAMPLES, true));
				updateModlist(media_path);
			}
		});
		alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Back", new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, final int which) {
				finish();
			}
		});
		alertDialog.show();
	}

	private boolean readShuffleModePref() {
		return mPrefs.getBoolean(OPTIONS_SHUFFLE_MODE, DEFAULT_SHUFFLE_MODE);
	}

	private boolean readLoopModePref() {
		return mPrefs.getBoolean(OPTIONS_LOOP_MODE, DEFAULT_LOOP_MODE);
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		setContentView(R.layout.modlist);

		final ListView listView = (ListView)findViewById(R.id.modlist_listview);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(final AdapterView<?> list, final View view, final int position, final long id) {
				onListItemClick(list, view, position, id);
			}		
		});

		playlistAdapter = new PlaylistAdapter(this, R.layout.song_item, R.id.info, new ArrayList<PlaylistItem>(), false);
		listView.setAdapter(playlistAdapter);

		registerForContextMenu(listView);
		final String mediaPath = mPrefs.getString(Preferences.MEDIA_PATH, Preferences.DEFAULT_MEDIA_PATH);

		context = this;

		setTitle("File Browser");

		crossfade = new Crossfader(this);
		crossfade.setup(R.id.modlist_content, R.id.modlist_spinner);

		curPath = (TextView)findViewById(R.id.current_path);
		registerForContextMenu(curPath);

		mBackButtonParentdir = mPrefs.getBoolean(Preferences.BACK_BUTTON_NAVIGATION, true);

		final int textColor = curPath.getCurrentTextColor();
		curPath.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(final View view, final MotionEvent event) {
				if(event.getAction() == MotionEvent.ACTION_UP){
					curPath.setTextColor(textColor);
				} else {
					curPath.setTextColor(getResources().getColor(R.color.pressed_color));
				}
				view.performClick();
				return false;
			}
		});

		// Check if directory exists
		final File modDir = new File(mediaPath);

		if (modDir.isDirectory()) {
			updateModlist(mediaPath);	
		} else {
			pathNotFound(mediaPath);
		}

		mShuffleMode = readShuffleModePref();
		mLoopMode = readLoopModePref();

		mPathStack = new Stack<String>();

		setupButtons();
	}
	
	public void upButtonClick(final View view) {
		parentDir();
	}

	private void parentDir() {
		final File file = new File(currentDir + "/.");
		String name = file.getParentFile().getParent();
		if (name == null) {
			name = "/";
		}

		if (!mPathStack.isEmpty()) {
			mPathStack.pop();
		}

		updateModlist(name);
	}

	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mBackButtonParentdir) {
				// Return to parent dir up to the starting level, then act as regular back
				if (!mPathStack.isEmpty()) {
					parentDir();
					return true;
				}
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		boolean saveModes = false;
		if (mShuffleMode != readShuffleModePref()) {
			saveModes = true;
		}
		if (mLoopMode != readLoopModePref()) {
			saveModes = true;
		}

		if (saveModes) {
			Log.i(TAG, "Save new file list preferences");
			final SharedPreferences.Editor editor = mPrefs.edit();
			editor.putBoolean(OPTIONS_SHUFFLE_MODE, mShuffleMode);
			editor.putBoolean(OPTIONS_LOOP_MODE, mLoopMode);
			editor.commit();
		}
	}

	public void update() {
		final String dir = currentDir;
		if (dir != null) {
			updateModlist(dir);
		}
	}

	public void updateModlist(final String path) {
		playlistAdapter.clear();

		currentDir = path;
		curPath.setText(path);

		parentNum = directoryNum = 0;
		final File modDir = new File(path);

		final List<PlaylistItem> list = new ArrayList<PlaylistItem>();
		final File[] dirFiles = modDir.listFiles(new DirFilter());
		if (dirFiles != null) {
			for (final File file : dirFiles) {
				directoryNum++;
				list.add(new PlaylistItem(file.getName(), "Directory", file.getAbsolutePath(), R.drawable.folder));	// NOPMD
			}
		}
		Collections.sort(list);
		playlistAdapter.addAll(list);

		list.clear();
		final File[] modFiles = modDir.listFiles(new ModFilter());

		if (modFiles != null) {
			for (final File file : modFiles) {
				final String filename = path + "/" + file.getName();
				final String date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
						DateFormat.MEDIUM).format(file.lastModified());

				final String name = file.getName();
				final String comment = date + String.format(" (%d kB)", file.length() / 1024);
				list.add(new PlaylistItem(name, comment, filename));	// NOPMD
			}
			Collections.sort(list);
			playlistAdapter.addAll(list);
		}

		playlistAdapter.notifyDataSetChanged();

		crossfade.crossfade();
	}

	private void deleteDirectory(final int position) {
		deleteName = playlistAdapter.getFilename(position);
		final String mediaPath = mPrefs.getString(Preferences.MEDIA_PATH, Preferences.DEFAULT_MEDIA_PATH);
		if (deleteName.startsWith(mediaPath)) {
			Message.yesNoDialog(this, "Delete directory", "Are you sure you want to delete directory \"" +
					FileUtils.basename(deleteName) + "\" and all its contents?", deleteDirectoryDialogClickListener);
		} else {
			Message.toast(this, "Directory not under mod dir");
		}
	}
	
	private static List<String> recursiveList(final String filename) {
		final List<String> list = new ArrayList<String>();
		final File file = new File(filename);
		
		if (file.isDirectory()) {
			for (final File f : file.listFiles()) {
				if (f.isDirectory()) {
					list.addAll(recursiveList(f.getPath()));
				} else {
					list.add(f.getPath());
				}
			}
		} else {
			list.add(filename);
		}
		
		return list;
	}
	
	private void choosePlaylist(final int start, final DialogInterface.OnClickListener listener) {

		// Return if no playlists exist
		if (PlaylistUtils.list().length <= 0) {
			Message.toast(this, getString(R.string.msg_no_playlists));
			return;
		}

		fileSelection = start;
		playlistSelection = 0;

		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.msg_select_playlist)
				.setPositiveButton(android.R.string.ok, listener)
				.setNegativeButton(android.R.string.cancel, listener)
				.setSingleChoiceItems(PlaylistUtils.listNoSuffix(), 0, new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, final int which) {
				playlistSelection = which;
			}
		}).show();
	}	

	private void clearCachedEntries(final List<String> fileList) {
		for (final String filename : fileList) {
			InfoCache.clearCache(filename);
		}
	}



	// Playlist context menu

	@Override
	public void onCreateContextMenu(final ContextMenu menu, final View view, final ContextMenu.ContextMenuInfo menuInfo) {
		if (view.equals(curPath)) {
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
			menu.add(Menu.NONE, 1, 1, "Add to play queue");
			menu.add(Menu.NONE, 2, 2, "Play contents");
			menu.add(Menu.NONE, 3, 3, "Delete directory");
		} else {											// For files			
			final int mode = Integer.parseInt(mPrefs.getString(Preferences.PLAYLIST_MODE, "1"));

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
	public boolean onContextItemSelected(final MenuItem item) {
		final int id = item.getItemId();

		if (isPathMenu) {
			switch (id) {
			case 0:						// Add all to playlist
				choosePlaylist(directoryNum, addFileListToPlaylistDialogClickListener);
				break;
			case 1:						// Recursive add to playlist
				choosePlaylist(2, addCurRecursiveToPlaylistDialogClickListener);
				break;
			case 2:						// Add all to queue
				addToQueue(playlistAdapter.getFilenameList(directoryNum));
				break;
			case 3:						// Set as default path
				final SharedPreferences.Editor editor = mPrefs.edit();
				editor.putString(Preferences.MEDIA_PATH, currentDir);
				editor.commit();
				Message.toast(context, "Set as default module path");
				break;
			case 4:						// Clear cache
				clearCachedEntries(playlistAdapter.getFilenameList(directoryNum));
				break;
			}

			return true;
		}

		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();

		if (info.position < parentNum) {				// Parent dir
			// Do nothing
		} else if (info.position < directoryNum) {		// Directories
			switch (id) {
			case 0:										//    Add to playlist (recursive)
				choosePlaylist(info.position, addRecursiveToPlaylistDialogClickListener);
				//addToPlaylist(info.position, 1, addDirToPlaylistDialogClickListener);
				break;
			case 1:										//    Add to play queue (recursive)
				addToQueue(recursiveList(playlistAdapter.getFilename(info.position)));
				break;
			case 2:										//    Play now (recursive)
				playModule(recursiveList(playlistAdapter.getFilename(info.position)));
				break;
			case 3:										//    delete directory
				deleteDirectory(info.position);
				break;
			}
		} else {										// Files
			switch (id) {
			case 0:										//   Add to playlist
				choosePlaylist(info.position, addFileToPlaylistDialogClickListener);
				break;
			case 1:										//   Add to queue
				addToQueue(playlistAdapter.getFilename(info.position));
				break;
			case 2:										//   Play this module
				playModule(playlistAdapter.getFilename(info.position));
				break;
			case 3:										//   Play all starting here
				playModule(playlistAdapter.getFilenameList(), info.position);
				break;
			case 4:										//   Delete file
				deleteName = playlistAdapter.getFilename(info.position);
				Message.yesNoDialog(this, "Delete", "Are you sure you want to delete " +
						FileUtils.basename(deleteName) + "?", deleteDialogClickListener);
				break;
			}
		}

		return true;
	}


}
