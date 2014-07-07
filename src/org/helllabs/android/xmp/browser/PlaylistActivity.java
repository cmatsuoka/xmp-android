package org.helllabs.android.xmp.browser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.preferences.Preferences;
import org.helllabs.android.xmp.util.InfoCache;
import org.helllabs.android.xmp.util.Log;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

import com.commonsware.cwac.tlv.TouchListView;


class PlayListFilter implements FilenameFilter {
	public boolean accept(final File dir, final String name) {
		return name.endsWith(PlaylistUtils.PLAYLIST_SUFFIX);
	}
}

public class PlaylistActivity extends BasePlaylistActivity {
	private static final String TAG = "PlayList";
	private String name;
	private PlaylistInfoAdapter plist;
	private Boolean modified;
	private TouchListView listView;
	
	@Override
	public void onCreate(final Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.playlist);
		
		final Bundle extras = getIntent().getExtras();
		if (extras == null) {
			return;
		}

		setTitle("Playlist");
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		listView = (TouchListView)findViewById(R.id.plist_list);
		super.setOnItemClickListener(listView);
		
		listView.setDropListener(onDrop);
		listView.setRemoveListener(onRemove);
		
		//final View curList = (View)findViewById(R.id.current_list);
		final TextView curListName = (TextView)findViewById(R.id.current_list_name);
		final TextView curListDesc = (TextView)findViewById(R.id.current_list_description);
		
		name = extras.getString("name");
		curListName.setText(name);
		curListDesc.setText(PlaylistUtils.readComment(this, name));
		registerForContextMenu(listView);
		
		// Set status area background color		
		//if (prefs.getBoolean(Preferences.DARK_THEME, false)) {
		//	curList.setBackgroundColor(R.color.dark_theme_status_color);
		//}
		
		shuffleMode = prefs.getBoolean(PlaylistUtils.OPTIONS_PREFIX + name + "_shuffleMode", true);
		loopMode = prefs.getBoolean(PlaylistUtils.OPTIONS_PREFIX + name + "_loopMode", false);
		setupButtons();
				
		modified = false;
		modifiedOptions = false;

		updateList();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		if (modified) {			
			writeList();
		}
		
		if (modifiedOptions) {
			final SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean(PlaylistUtils.OPTIONS_PREFIX + name + "_shuffleMode", shuffleMode);
			editor.putBoolean(PlaylistUtils.OPTIONS_PREFIX + name + "_loopMode", loopMode);
			editor.commit();
		}
	}
	
	public void update() {
		updateList();
	}
	
	private void updateList() {
		modList.clear();
		
		final File file = new File(Preferences.DATA_DIR, name + PlaylistUtils.PLAYLIST_SUFFIX);
		String line;
		int lineNum;
		
		final List<Integer> invalidList = new ArrayList<Integer>();
		
	    try {
	    	final BufferedReader reader = new BufferedReader(new FileReader(file), 512);
	    	lineNum = 0;
	    	while ((line = reader.readLine()) != null) {
	    		final String[] fields = line.split(":", 3);
	    		if (InfoCache.fileExists(fields[0])) {
	    			modList.add(new PlaylistInfo(fields[2], fields[1], fields[0], R.drawable.grabber));
	    		} else {
	    			invalidList.add(lineNum);
	    		}
	    		lineNum++;
	    	}
	    	reader.close();
	    } catch (IOException e) {
	    	Log.e(TAG, "Error reading playlist " + file.getPath());
	    }		
		
	    if (!invalidList.isEmpty()) {
	    	final int[] array = new int[invalidList.size()];
	    	final Iterator<Integer> iterator = invalidList.iterator();
	    	for (int i = 0; i < array.length; i++) {
	    		array[i] = iterator.next().intValue();
	    	}
	    	
			try {
				FileUtils.removeLineFromFile(file, array);
			} catch (FileNotFoundException e) {
				Log.e(TAG, "Playlist file " + file.getPath() + " not found");
			} catch (IOException e) {
				Log.e(TAG, "I/O error removing invalid lines from " + file.getPath());
			}
		}
	    
	    plist = new PlaylistInfoAdapter(PlaylistActivity.this,
    				R.layout.playlist_item, R.id.plist_info, modList,
    				prefs.getBoolean(Preferences.USE_FILENAME, false));
        
	    listView.setAdapter(plist);
	}
	
	// Playlist context menu
	
	@Override
	public void onCreateContextMenu(final ContextMenu menu, final View view, final ContextMenuInfo menuInfo) {
		
		final int mode = Integer.parseInt(prefs.getString(Preferences.PLAYLIST_MODE, "1"));

		menu.setHeaderTitle("Edit playlist");
		menu.add(Menu.NONE, 0, 0, "Remove from playlist");
		menu.add(Menu.NONE, 1, 1, "Add to play queue");
		menu.add(Menu.NONE, 2, 2, "Add all to play queue");
		if (mode != 2) {
			menu.add(Menu.NONE, 3, 3, "Play this module");
		}
		if (mode != 1) {
			menu.add(Menu.NONE, 4, 4, "Play all starting here");
		}
	}
	
	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		final int itemId = item.getItemId();
		
		switch (itemId) {
		case 0:										// Remove from playlist
			removeFromPlaylist(name, info.position);
			updateList();
			break;
		case 1:										// Add to play queue
			addToQueue(info.position, 1);
			break;
		case 2:										// Add all to play queue
			addToQueue(0, modList.size());
	    	break;
		case 3:										// Play only this module
			playModule(modList.get(info.position).filename);
			break;
		case 4:										// Play all starting here
			playModule(modList, info.position);
			break;
		}

		return true;
	}
	
	public void removeFromPlaylist(final String playlist, final int position) {
		final File file = new File(Preferences.DATA_DIR, name + PlaylistUtils.PLAYLIST_SUFFIX);
		if (modified) {
			writeList();
		}
		try {
			FileUtils.removeLineFromFile(file, position);
		} catch (FileNotFoundException e) {
			Log.e(TAG, "Playlist file " + file.getPath() + " not found");
		} catch (IOException e) {
			Log.e(TAG, "I/O error removing line from " + file.getPath());
		}
	}
	
	// List reorder
	
	private final TouchListView.DropListener onDrop = new TouchListView.DropListener() {
		@Override
		public void drop(final int from, final int to) {
			final PlaylistInfo item = plist.getItem(from);
			plist.remove(item);
			plist.insert(item, to);
			modified = true;
		}
	};

	private final TouchListView.RemoveListener onRemove = new TouchListView.RemoveListener() {
		@Override
		public void remove(final int which) {
			plist.remove(plist.getItem(which));
		}
	};		

	private final void writeList() {		
		final File file = new File(Preferences.DATA_DIR, name + PlaylistUtils.PLAYLIST_SUFFIX + ".new");
		Log.i(TAG, "Write playlist " + name);
		
		file.delete();
		
		try {
			final BufferedWriter out = new BufferedWriter(new FileWriter(file), 512);
			for (final PlaylistInfo info : modList) {
				out.write(String.format("%s:%s:%s\n", info.filename, info.comment, info.name));
			}
			out.close();
			
			final File oldFile = new File(Preferences.DATA_DIR, name + PlaylistUtils.PLAYLIST_SUFFIX);
			oldFile.delete();
			file.renameTo(oldFile);
			
			modified = false;
		} catch (IOException e) {
			Log.e(TAG, "Error writing playlist " + file.getPath());
		}
	}
}
