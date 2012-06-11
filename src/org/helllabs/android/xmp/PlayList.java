package org.helllabs.android.xmp;

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
	String name;
	View curList;
	TextView curListName;
	TextView curListDesc;
	PlaylistInfoAdapter plist;
	Boolean modified;
	SharedPreferences prefs;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.playlist);
		
		Bundle extras = getIntent().getExtras();
		if (extras == null)
			return;
		
		setTitle("Playlist");
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		TouchListView tlv = (TouchListView)getListView();
		tlv.setDropListener(onDrop);
		tlv.setRemoveListener(onRemove);
		
		curList = (View)findViewById(R.id.current_list);
		curListName = (TextView)findViewById(R.id.current_list_name);
		curListDesc = (TextView)findViewById(R.id.current_list_description);
		
		name = extras.getString("name");
		curListName.setText(name);
		curListDesc.setText(PlaylistUtils.readComment(this, name));
		registerForContextMenu(getListView());
		
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
	
	void updateList() {
		modList.clear();
		
		File file = new File(Settings.dataDir, name + ".playlist");
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
	    	int[] x = new int[invalidList.size()];
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
    				prefs.getBoolean(Settings.PREF_USE_FILENAME, false));
        
	    setListAdapter(plist);
	}
	
	// Playlist context menu
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		//int i = 0;
		menu.setHeaderTitle("Edit playlist");
		menu.add(Menu.NONE, 0, 0, "Remove from playlist");
		menu.add(Menu.NONE, 1, 1, "Add to play queue");
		menu.add(Menu.NONE, 2, 2, "Add all to play queue");
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		int id = item.getItemId();
		
		switch (id) {
		case 0:
			removeFromPlaylist(name, info.position);
			updateList();
			break;
		case 1:
			addToQueue(info.position, 1);
			break;
		case 2:
			addToQueue(0, modList.size());
	    	break;
		}

		return true;
	}
	
	public void removeFromPlaylist(String playlist, int position) {
		File file = new File(Settings.dataDir, name + ".playlist");
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
		File file = new File(Settings.dataDir, name + ".playlist.new");
		Log.i("Xmp PlayList", "Write playlist " + name);
		
		file.delete();
		
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(file), 512);
			for (PlaylistInfo info : modList) {
				out.write(String.format("%s:%s:%s\n", info.filename, info.comment, info.name));
			}
			out.close();
			
			File oldFile = new File(Settings.dataDir, name + ".playlist");
			oldFile.delete();
			file.renameTo(oldFile);
			
			modified = false;
		} catch (IOException e) {
			Log.e("Xmp PlayList", "Error writing playlist " + file.getPath());
		}
	}
}
