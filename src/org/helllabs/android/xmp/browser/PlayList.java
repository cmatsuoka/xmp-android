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

import org.helllabs.android.xmp.InfoCache;
import org.helllabs.android.xmp.Preferences;
import org.helllabs.android.xmp.R;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

import com.commonsware.cwac.tlv.TouchListView;


class PlayListFilter implements FilenameFilter {
	public boolean accept(File dir, String name) {
		return name.endsWith(".playlist");
	}
}

public class PlayList extends PlaylistActivity {
	private String name;
	private PlaylistInfoAdapter plist;
	private Boolean modified;
	private TouchListView listView;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.playlist);
		
		Bundle extras = getIntent().getExtras();
		if (extras == null)
			return;

		
		setTitle("Playlist");
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		listView = (TouchListView)findViewById(R.id.plist_list);
		
		listView.setDropListener(onDrop);
		listView.setRemoveListener(onRemove);
		
		final View curList = (View)findViewById(R.id.current_list);
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
		
		shuffleMode = prefs.getBoolean("options_" + name + "_shuffleMode", true);
		loopMode = prefs.getBoolean("options_" + name + "_loopMode", false);
		setupButtons();
				
		modified = false;
		modifiedOptions = false;

		updateList();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (modified) {			
			writeList();
		}
		
		if (modifiedOptions) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean("options_" + name + "_shuffleMode", shuffleMode);
			editor.putBoolean("options_" + name + "_loopMode", loopMode);
			editor.commit();
		}
	}
	
	void update() {
		updateList();
	}
	
	private void updateList() {
		modList.clear();
		
		File file = new File(Preferences.DATA_DIR, name + ".playlist");
		String line;
		int lineNum;
		
		List<Integer> invalidList = new ArrayList<Integer>();
		
	    try {
	    	BufferedReader in = new BufferedReader(new FileReader(file), 512);
	    	lineNum = 0;
	    	while ((line = in.readLine()) != null) {
	    		String[] fields = line.split(":", 3);
	    		if (!InfoCache.fileExists(fields[0])) {
	    			invalidList.add(lineNum);
	    		} else {
	    			modList.add(new PlaylistInfo(fields[2], fields[1], fields[0], R.drawable.grabber));
	    		}
	    		lineNum++;
	    	}
	    	in.close();
	    } catch (IOException e) {
	    	Log.e("Xmp PlayList", "Error reading playlist " + file.getPath());
	    }		
		
	    if (!invalidList.isEmpty()) {
	    	final int[] x = new int[invalidList.size()];
	    	Iterator<Integer> iterator = invalidList.iterator();
	    	for (int i = 0; i < x.length; i++)
	    		x[i] = iterator.next().intValue();
	    	
			try {
				FileUtils.removeLineFromFile(file, x);
			} catch (FileNotFoundException e) {

			} catch (IOException e) {

			}
		}
	    
	    plist = new PlaylistInfoAdapter(PlayList.this,
    				R.layout.playlist_item, R.id.plist_info, modList,
    				prefs.getBoolean(Preferences.USE_FILENAME, false));
        
	    listView.setAdapter(plist);
	}
	
	// Playlist context menu
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		
		final int mode = Integer.parseInt(prefs.getString(Preferences.PLAYLIST_MODE, "1"));

		menu.setHeaderTitle("Edit playlist");
		menu.add(Menu.NONE, 0, 0, "Remove from playlist");
		menu.add(Menu.NONE, 1, 1, "Add to play queue");
		menu.add(Menu.NONE, 2, 2, "Add all to play queue");
		if (mode != 2)
			menu.add(Menu.NONE, 3, 3, "Play this module");
		if (mode != 1)
			menu.add(Menu.NONE, 4, 4, "Play all starting here");
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		int id = item.getItemId();
		
		switch (id) {
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
	
	public void removeFromPlaylist(String playlist, int position) {
		File file = new File(Preferences.DATA_DIR, name + ".playlist");
		if (modified) {
			writeList();
		}
		try {
			FileUtils.removeLineFromFile(file, position);
		} catch (FileNotFoundException e) {

		} catch (IOException e) {

		}
	}
	
	// List reorder
	
	private TouchListView.DropListener onDrop = new TouchListView.DropListener() {
		@Override
		public void drop(int from, int to) {
			PlaylistInfo item = plist.getItem(from);
			plist.remove(item);
			plist.insert(item, to);
			modified = true;
		}
	};

	private TouchListView.RemoveListener onRemove = new TouchListView.RemoveListener() {
		@Override
		public void remove(int which) {
			plist.remove(plist.getItem(which));
		}
	};		

	private void writeList() {		
		File file = new File(Preferences.DATA_DIR, name + ".playlist.new");
		Log.i("Xmp PlayList", "Write playlist " + name);
		
		file.delete();
		
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(file), 512);
			for (PlaylistInfo info : modList) {
				out.write(String.format("%s:%s:%s\n", info.filename, info.comment, info.name));
			}
			out.close();
			
			File oldFile = new File(Preferences.DATA_DIR, name + ".playlist");
			oldFile.delete();
			file.renameTo(oldFile);
			
			modified = false;
		} catch (IOException e) {
			Log.e("Xmp PlayList", "Error writing playlist " + file.getPath());
		}
	}
}
