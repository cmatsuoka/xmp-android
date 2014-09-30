package org.helllabs.android.xmp.browser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.XmpApplication;
import org.helllabs.android.xmp.browser.adapter.PlaylistItemAdapter;
import org.helllabs.android.xmp.browser.model.PlaylistItem;
import org.helllabs.android.xmp.browser.playlist.PlaylistUtils;
import org.helllabs.android.xmp.modarchive.Search;
import org.helllabs.android.xmp.player.PlayerActivity;
import org.helllabs.android.xmp.preferences.Preferences;
import org.helllabs.android.xmp.service.ModInterface;
import org.helllabs.android.xmp.service.PlayerService;
import org.helllabs.android.xmp.util.InfoCache;
import org.helllabs.android.xmp.util.Log;
import org.helllabs.android.xmp.util.Message;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;

public abstract class BasePlaylistActivity extends ActionBarActivity {
	private static final String TAG = "PlaylistActivity";
	private static final int SETTINGS_REQUEST = 45;
	private static final int PLAY_MOD_REQUEST = 669; 
	
	private Context mContext;
	private boolean mShowToasts;
	private ModInterface mModPlayer;
	private String[] mAddList;
	protected SharedPreferences mPrefs;
	protected PlaylistItemAdapter playlistAdapter;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mShowToasts = mPrefs.getBoolean(Preferences.SHOW_TOAST, true);
		
		// Action bar icon navigation
	    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	protected abstract void setShuffleMode(boolean shuffleMode);
	protected abstract void setLoopMode(boolean loopMode);
	protected abstract boolean isShuffleMode();
	protected abstract boolean isLoopMode();
	
	private final OnClickListener playAllButtonListener = new OnClickListener() {
		@Override
		public void onClick(final View view) {
			playModule(playlistAdapter.getItems());
		}
	};
	
	private final OnClickListener toggleLoopButtonListener = new OnClickListener() {
		@Override
		public void onClick(final View view) {
			boolean loopMode = isLoopMode();
			loopMode ^= true;
			((ImageButton)view).setImageResource(loopMode ?
					R.drawable.list_loop_on : R.drawable.list_loop_off);
			if (mShowToasts) {
				Message.toast(view.getContext(), loopMode ? "Loop on" : "Loop off");
			}
			setLoopMode(loopMode);
		}
	};
	
	private final OnClickListener toggleShuffleButtonListener = new OnClickListener() {
		@Override
		public void onClick(final View view) {
			boolean shuffleMode = isShuffleMode();
			shuffleMode ^= true;
			((ImageButton)view).setImageResource(shuffleMode ?	R.drawable.list_shuffle_on : R.drawable.list_shuffle_off);
			if (mShowToasts) {
				Message.toast(view.getContext(), shuffleMode ? "Shuffle on" : "Shuffle off");
			}
			setShuffleMode(shuffleMode);
		}
	};
	
	protected void setupButtons() {
		final ImageButton playAllButton = (ImageButton)findViewById(R.id.play_all);
		final ImageButton toggleLoopButton = (ImageButton)findViewById(R.id.toggle_loop);
		final ImageButton toggleShuffleButton = (ImageButton)findViewById(R.id.toggle_shuffle);

		playAllButton.setImageResource(R.drawable.list_play);
		playAllButton.setOnClickListener(playAllButtonListener);

		toggleLoopButton.setImageResource(isLoopMode() ? R.drawable.list_loop_on : R.drawable.list_loop_off);
		toggleLoopButton.setOnClickListener(toggleLoopButtonListener);

		toggleShuffleButton.setImageResource(isShuffleMode() ? R.drawable.list_shuffle_on : R.drawable.list_shuffle_off);
		toggleShuffleButton.setOnClickListener(toggleShuffleButtonListener);
	}

	protected void onListItemClick(final AdapterView<?> list, final View view, final int position, final long id) {
		final PlaylistItemAdapter adapter = (PlaylistItemAdapter)list.getAdapter();
		final String filename = adapter.getItem(position).filename;
		
		final int mode = Integer.parseInt(mPrefs.getString(Preferences.PLAYLIST_MODE, "1"));

		/* Test module again if invalid, in case a new file format is added to the
		 * player library and the file was previously unrecognized and cached as invalid.
		 */
		if (InfoCache.testModuleForceIfInvalid(filename)) {
			switch (mode) {
			case 1:								// play all starting at this one
			default:
				playModule(adapter.getItems(), position, isShuffleMode(), isShuffleMode());
				break;
			case 2:								// play this one
				playModule(filename);
				break;
			case 3:								// add to queue
				addToQueue(position, 1);
				break;
			}
		} else {
			Message.toast(mContext, "Unrecognized file format");
		}
	}
	
	// Item click	
	protected void setOnItemClickListener(final ListView list) {
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(final AdapterView<?> list, final View view, final int position, final long id) {
				onListItemClick(list, view, position, id);
			}
		});
	}

	abstract public void update();

	// Play all modules in list and honor default shuffle mode
	protected void playModule(final List<PlaylistItem> list) {
		playModule(list, 0, isShuffleMode());
	}

	// Play all modules in list with start position, no shuffle
	protected void playModule(final List<PlaylistItem> list, final int position) {
		playModule(list, position, false);
	}
	
	protected void playModule(final List<PlaylistItem> list, final int start, final boolean shuffle) {
		playModule(list, start, shuffle, false);
	}

	// Play modules in list starting at the specified one
	protected void playModule(final List<PlaylistItem> list, int start, final boolean shuffle, final boolean keepFirst) {
		int num = 0;
		int dir = 0;

		for (final PlaylistItem info : list) {
			if (new File(info.filename).isDirectory()) {
				dir++;
			} else {
				num++;
			}
		}
		if (num == 0) {
			return;
		}

		if (start < dir) {
			start = dir;
		}

		if (start >= (dir + num)) {
			return;
		}

		final String[] mods = new String[num];

		int i = 0;
		for (final PlaylistItem info : list) {
			if (new File(info.filename).isFile()) {
				mods[i++] = info.filename;
			}
		}
		if (i > 0) {
			playModule(mods, start - dir, shuffle, keepFirst);
		}
	}

	// Play this module
	protected void playModule(final String mod) {
		final String[] mods = { mod };
		playModule(mods, 0, isShuffleMode(), false);
	}

	// Play all modules in list and honor default shuffle mode
	protected void playModule(final String[] mods) {
		playModule(mods, 0, isShuffleMode(), false);
	}

	protected void playModule(final String[] mods, final int start, final boolean shuffle, final boolean keepFirst) {
		if (mShowToasts) {
			if (mods.length > 1) {
				Message.toast(this, "Play all modules in list");
			} else {
				Message.toast(this, "Play only this module");
			}
		}

		final Intent intent = new Intent(this, PlayerActivity.class);
		((XmpApplication)getApplication()).setFileArray(mods);
		intent.putExtra("shuffle", shuffle);
		intent.putExtra("loop", isLoopMode());
		intent.putExtra("start", start);
		intent.putExtra("keepFirst", keepFirst);
		//intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);	// prevent screen flicker when starting player activity 
		Log.i(TAG, "Start Player activity");
		startActivityForResult(intent, PLAY_MOD_REQUEST);
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		Log.i(TAG, "Activity result " + requestCode + "," + resultCode);
		switch (requestCode) {
		case SETTINGS_REQUEST:
			update();			
			mShowToasts = mPrefs.getBoolean(Preferences.SHOW_TOAST, true);
			break;
		case PLAY_MOD_REQUEST:
			if (resultCode != RESULT_OK) {
				update();
			}
			break;
		}
	}

	// Connection

	private final ServiceConnection connection = new ServiceConnection() {
		public void onServiceConnected(final ComponentName className, final IBinder service) {
			mModPlayer = ModInterface.Stub.asInterface(service);
			try {				
				mModPlayer.add(mAddList);
			} catch (RemoteException e) {
				Message.toast(BasePlaylistActivity.this, "Error adding module");
			}
			unbindService(connection);
		}

		public void onServiceDisconnected(final ComponentName className) {
			mModPlayer = null;
		}
	};
	
	protected static List<String> recursiveList(final String filename) {
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
	
	protected void addToQueue(final List<String> list) {
		final List<String> realList = new ArrayList<String>();
		int realSize = 0;
		boolean invalid = false;

		for (final String filename : list) {
			if (InfoCache.testModule(filename)) {
				realList.add(filename);
				realSize++;
			} else {
				invalid = true;
			}
		}

		if (invalid) {
			Message.toast(this, "Only valid files were sent to player");
		}

		if (realSize > 0) {
			final Intent service = new Intent(this, PlayerService.class);

			if (PlayerService.isAlive) {
				mAddList = (String[])realList.toArray();		
				bindService(service, connection, 0);
			} else {
				playModule((String[])realList.toArray());
			}
		}
	}

	protected void addToQueue(final int start, final int size) {
		final String[] list = new String[size];
		int realSize = 0;
		boolean invalid = false;

		for (int i = 0; i < size; i++) {
			final String filename = playlistAdapter.getItem(start + i).filename;
			if (InfoCache.testModule(filename)) {
				list[realSize++] = filename;
			} else {
				invalid = true;
			}
		}

		if (invalid) {
			Message.toast(mContext, "Only valid files were sent to player");
		}

		if (realSize > 0) {
			final Intent service = new Intent(this, PlayerService.class);

			final String[] realList = new String[realSize];
			System.arraycopy(list,  0, realList, 0, realSize);

			if (PlayerService.isAlive) {
				mAddList = realList;		
				bindService(service, connection, 0);
			} else {
				playModule(realList);
			}
		}
	}
	

	// Menu

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);

		// Calling super after populating the menu is necessary here to ensure that the
		// action bar helpers have a chance to handle this event.
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch(item.getItemId()) {
		case android.R.id.home:
			final Intent intent = new Intent(this, PlaylistMenu.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		case R.id.menu_new_playlist:
			PlaylistUtils.newPlaylistDialog(this);
			break;
		case R.id.menu_prefs:		
			startActivityForResult(new Intent(this, Preferences.class), SETTINGS_REQUEST);
			break;
		case R.id.menu_refresh:
			update();
			break;
		case R.id.menu_download:
			startActivity(new Intent(this, Search.class));
			break;
		}
		return super.onOptionsItemSelected(item);
	}	
}
