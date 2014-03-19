package org.helllabs.android.xmp.browser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.helllabs.android.xmp.InfoCache;
import org.helllabs.android.xmp.ModInterface;
import org.helllabs.android.xmp.Preferences;
import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.player.PlayerActivity;
import org.helllabs.android.xmp.service.PlayerService;

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
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;

@SuppressWarnings("PMD.GodClass")
public abstract class PlaylistActivity extends ActionBarActivity {	// NOPMD
	private static final int SETTINGS_REQUEST = 45;
	private static final int PLAY_MOD_REQUEST = 669; 
	protected List<PlaylistInfo> modList = new ArrayList<PlaylistInfo>();
	protected boolean shuffleMode = true;
	protected boolean loopMode;
	protected boolean modifiedOptions;
	protected SharedPreferences prefs;
	protected String deleteName;
	private boolean showToasts;
	private ModInterface modPlayer;
	private String[] addList;
	private Context context;

	@Override
	public void onCreate(final Bundle icicle) {
		super.onCreate(icicle);

		context = this;
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		showToasts = prefs.getBoolean(Preferences.SHOW_TOAST, true);
	}

	protected void setupButtons() {
		final ImageButton playAllButton = (ImageButton)findViewById(R.id.play_all);
		final ImageButton toggleLoopButton = (ImageButton)findViewById(R.id.toggle_loop);
		final ImageButton toggleShuffleButton = (ImageButton)findViewById(R.id.toggle_shuffle);

		playAllButton.setImageResource(R.drawable.list_play);
		playAllButton.setOnClickListener(new OnClickListener() {
			public void onClick(final View view) {
				playModule(modList);
			}
		});

		toggleLoopButton.setImageResource(loopMode ? R.drawable.list_loop_on : R.drawable.list_loop_off);
		toggleLoopButton.setOnClickListener(new OnClickListener() {
			public void onClick(final View view) {
				loopMode = !loopMode;
				((ImageButton)view).setImageResource(loopMode ?
						R.drawable.list_loop_on : R.drawable.list_loop_off);
				if (showToasts) {
					Message.toast(view.getContext(), loopMode ? "Loop on" : "Loop off");
				}
				modifiedOptions = true;
			}
		});

		toggleShuffleButton.setImageResource(shuffleMode ? R.drawable.list_shuffle_on : R.drawable.list_shuffle_off);
		toggleShuffleButton.setOnClickListener(new OnClickListener() {
			public void onClick(final View view) {
				shuffleMode = !shuffleMode;
				((ImageButton)view).setImageResource(shuffleMode ?	R.drawable.list_shuffle_on : R.drawable.list_shuffle_off);
				if (showToasts) {
					Message.toast(view.getContext(), shuffleMode ? "Shuffle on" : "Shuffle off");
				}
				modifiedOptions = true;
			}
		});
	}

	protected void onListItemClick(final AdapterView<?> list, final View view, final int position, final long id) {
		final String filename = modList.get(position).filename;
		final int mode = Integer.parseInt(prefs.getString(Preferences.PLAYLIST_MODE, "1"));

		/* Test module again if invalid, in case a new file format is added to the
		 * player library and the file was previously unrecognized and cached as invalid.
		 */
		if (InfoCache.testModuleForceIfInvalid(filename)) {
			switch (mode) {
			case 1:								// play all starting at this one
			default:
				playModule(modList, position);
				break;
			case 2:								// play this one
				playModule(filename);
				break;
			case 3:								// add to queue
				addToQueue(position, 1);
				break;
			}
		} else {
			Message.toast(context, "Unrecognized file format");
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
	public void playModule(final List<PlaylistInfo> list) {
		playModule(list, 0, shuffleMode);
	}

	// Play all modules in list with start position, no shuffle
	public void playModule(final List<PlaylistInfo> list, final int position) {
		playModule(list, position, false);
	}

	// Play modules in list starting at the specified one
	public void playModule(final List<PlaylistInfo> list, int start, final boolean shuffle) {
		int num = 0;
		int dir = 0;

		for (final PlaylistInfo info : list) {
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

		int i = 0; // NOPMD
		for (final PlaylistInfo info : list) {
			if (new File(info.filename).isFile()) {
				mods[i++] = info.filename;
			}
		}
		if (i > 0) {
			playModule(mods, start - dir, shuffle);
		}
	}

	// Play this module
	public void playModule(final String mod) {
		final String[] mods = { mod };
		playModule(mods, 0, shuffleMode);
	}

	// Play all modules in list and honor default shuffle mode
	public void playModule(final String[] mods) {
		playModule(mods, 0, shuffleMode);
	}

	public void playModule(final String[] mods, final int start, final boolean shuffle) {
		if (showToasts) {
			if (mods.length > 1) {
				Message.toast(this, "Play all modules in list");
			} else {
				Message.toast(this, "Play only this module");
			}
		}

		final Intent intent = new Intent(this, PlayerActivity.class);
		intent.putExtra("files", mods);
		intent.putExtra("shuffle", shuffle);
		intent.putExtra("loop", loopMode);
		intent.putExtra("start", start);
		Log.i("Xmp PlaylistActivity", "Start activity Player");
		startActivityForResult(intent, PLAY_MOD_REQUEST);
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		Log.i("Xmp PlaylistActivity", "Activity result " + requestCode + "," + resultCode);
		switch (requestCode) {
		case SETTINGS_REQUEST:
			update();			
			showToasts = prefs.getBoolean(Preferences.SHOW_TOAST, true);
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
			modPlayer = ModInterface.Stub.asInterface(service);
			try {				
				modPlayer.add(addList);
			} catch (RemoteException e) {
				Message.toast(PlaylistActivity.this, "Error adding module");
			}
			unbindService(connection);
		}

		public void onServiceDisconnected(final ComponentName className) {
			modPlayer = null;		// NOPMD
		}
	};

	protected void addToQueue(final int start, final int size) {
		final String[] list = new String[size];
		int realSize = 0;
		boolean invalid = false;

		for (int i = 0; i < size; i++) {
			final String filename = modList.get(start + i).filename;
			if (InfoCache.testModule(filename)) {
				list[realSize++] = filename;
			} else {
				invalid = true;
			}
		}

		if (invalid) {
			Message.toast(context, "Only valid files were sent to player");
		}

		if (realSize > 0) {
			final Intent service = new Intent(this, PlayerService.class);

			final String[] realList = new String[realSize];
			System.arraycopy(list,  0, realList, 0, realSize);

			if (PlayerService.isAlive) {
				addList = realList;		
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
			(new PlaylistUtils()).newPlaylist(this);
			break;
		case R.id.menu_prefs:		
			startActivityForResult(new Intent(this, Preferences.class), SETTINGS_REQUEST);
			break;
		case R.id.menu_refresh:
			update();
			break;
		}
		return super.onOptionsItemSelected(item);
	}	
}
