package org.helllabs.android.xmp.browser;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.preferences.Preferences;
import org.helllabs.android.xmp.util.Log;
import org.helllabs.android.xmp.util.Message;

import android.os.Bundle;
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
		return name.endsWith(Playlist.PLAYLIST_SUFFIX);
	}
}

public class PlaylistActivity extends BasePlaylistActivity {
	private static final String TAG = "PlaylistActivity";
	private Playlist playlist;
	private PlaylistItemAdapter playlistAdapter;

	@Override
	public void onCreate(final Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.playlist);

		final Bundle extras = getIntent().getExtras();
		if (extras == null) {
			return;
		}

		setTitle("Playlist");

		final TouchListView listView = (TouchListView)findViewById(R.id.plist_list);
		super.setOnItemClickListener(listView);

		listView.setDropListener(onDrop);
		listView.setRemoveListener(onRemove);

		final String name = extras.getString("name");

		try {
			playlist = new Playlist(this, name);
			playlistAdapter = new PlaylistItemAdapter(this, R.layout.song_item, R.id.info, playlist.getList(), false);
			listView.setAdapter(playlistAdapter);
		} catch (IOException e) {
			Log.e(TAG, "Can't read playlist " + name);
		}

		final TextView curListName = (TextView)findViewById(R.id.current_list_name);
		final TextView curListDesc = (TextView)findViewById(R.id.current_list_description);

		curListName.setText(name);
		curListDesc.setText(playlist.getComment());
		registerForContextMenu(listView);

		setupButtons();
	}

	@Override
	public void onPause() {
		super.onPause();

		try {
			playlist.commit();
		} catch (IOException e) {
			Message.toast(this, getString(R.string.error_write_to_playlist));
		}

	}

	@Override
	protected List<PlaylistItem> getModList() {
		return playlist.getList();
	}

	@Override
	protected void setShuffleMode(final boolean shuffleMode) {
		playlist.setShuffleMode(shuffleMode);
	}

	@Override
	protected void setLoopMode(final boolean loopMode) {
		playlist.setLoopMode(loopMode);
	}

	@Override
	protected boolean isShuffleMode() {
		return playlist.isShuffleMode();
	}

	@Override
	protected boolean isLoopMode() {
		return playlist.isLoopMode();
	}

	public void update() {
		playlistAdapter.notifyDataSetChanged();
	}

	// Playlist context menu

	@Override
	public void onCreateContextMenu(final ContextMenu menu, final View view, final ContextMenuInfo menuInfo) {

		final int mode = Integer.parseInt(mPrefs.getString(Preferences.PLAYLIST_MODE, "1"));

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
			playlist.remove(info.position);
			try {
				playlist.commit();
			} catch (IOException e) {
				Message.toast(this, getString(R.string.error_write_to_playlist));
			}
			update();
			break;
		case 1:										// Add to play queue
			addToQueue(info.position, 1);
			break;
		case 2:										// Add all to play queue
			addToQueue(0, getModList().size());
			break;
		case 3:										// Play only this module
			playModule(getModList().get(info.position).filename);
			break;
		case 4:										// Play all starting here
			playModule(getModList(), info.position);
			break;
		}

		return true;
	}


	// List reorder

	private final TouchListView.DropListener onDrop = new TouchListView.DropListener() {
		@Override
		public void drop(final int from, final int to) {
			final PlaylistItem item = playlistAdapter.getItem(from);
			playlistAdapter.remove(item);
			playlistAdapter.insert(item, to);
			playlist.setListChanged(true);
		}
	};

	private final TouchListView.RemoveListener onRemove = new TouchListView.RemoveListener() {
		@Override
		public void remove(final int which) {
			playlistAdapter.remove(playlistAdapter.getItem(which));
		}
	};		
}
