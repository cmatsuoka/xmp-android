package org.helllabs.android.xmp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListView;

public abstract class PlaylistActivity extends ActionBarListActivity {
	private static final int SETTINGS_REQUEST = 45;
	private static final int PLAY_MODULE_REQUEST = 669; 
	private ImageButton playAllButton, toggleLoopButton, toggleShuffleButton;
	protected List<PlaylistInfo> modList = new ArrayList<PlaylistInfo>();
	protected boolean shuffleMode = true;
	protected boolean loopMode = false;
	protected boolean modifiedOptions = false;
	protected SharedPreferences prefs;
	protected String deleteName;
	private boolean showToasts;
	private ModInterface modPlayer;
	private String[] addList;
	private Context context;
	private PendingIntent restartIntent;
	private Activity activity;

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		context = this;
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		showToasts = prefs.getBoolean(Settings.PREF_SHOW_TOAST, true);
		
		// for activity restart
		activity = this;
		restartIntent = PendingIntent.getActivity(this.getBaseContext(),
					0, new Intent(getIntent()), getIntent().getFlags());
	}

	void setupButtons() {
		playAllButton = (ImageButton)findViewById(R.id.play_all);
		toggleLoopButton = (ImageButton)findViewById(R.id.toggle_loop);
		toggleShuffleButton = (ImageButton)findViewById(R.id.toggle_shuffle);

		playAllButton.setImageResource(R.drawable.list_play);
		playAllButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				playModule(modList);
			}
		});

		toggleLoopButton.setImageResource(loopMode ?
				R.drawable.list_loop_on : R.drawable.list_loop_off);
		toggleLoopButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				loopMode = !loopMode;
				((ImageButton)v).setImageResource(loopMode ?
						R.drawable.list_loop_on : R.drawable.list_loop_off);
				if (showToasts)
					Message.toast(v.getContext(), loopMode ? "Loop on" : "Loop off");
				modifiedOptions = true;
			}
		});

		toggleShuffleButton.setImageResource(shuffleMode ?
				R.drawable.list_shuffle_on : R.drawable.list_shuffle_off);
		toggleShuffleButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				shuffleMode = !shuffleMode;
				((ImageButton)v).setImageResource(shuffleMode ?
						R.drawable.list_shuffle_on : R.drawable.list_shuffle_off);
				if (showToasts)
					Message.toast(v.getContext(), shuffleMode ? "Shuffle on" : "Shuffle off");
				modifiedOptions = true;
			}
		});
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		final String filename = modList.get(position).filename;
		
		/* Test module again if invalid, in case a new file format is added to the
		 * player library and the file was previously unrecognized and cached as invalid.
		 */
		if (InfoCache.testModuleForceIfInvalid(filename)) {
			playModule(filename);
		} else {
			Message.toast(context, "Unrecognized file format");
		}
	}

	abstract void update();

	void playModule(List<PlaylistInfo> list) {
		int num = 0;
		for (PlaylistInfo p : list) {
			if ((new File(p.filename).isFile())) {
				num++;
			}
		}
		if (num == 0)
			return;

		String[] mods = new String[num];
		int i = 0;
		for (PlaylistInfo p : list) {
			if ((new File(p.filename).isFile())) {
				mods[i++] = p.filename;
			}
		}
		if (i > 0) {
			playModule(mods);
		}
	}

	void playModule(String mod) {
		String[] mods = { mod };
		playModule(mods);
	}

	void playModule(String[] mods) {
		if (showToasts) {
			if (mods.length > 1)
				Message.toast(this, "Play all modules in list");
			else
				Message.toast(this, "Play only this module");
		}
		Intent intent = new Intent(this, Player.class);
		intent.putExtra("files", mods);
		intent.putExtra("shuffle", shuffleMode);
		intent.putExtra("loop", loopMode);
		Log.i("Xmp PlaylistActivity", "Start activity Player");
		startActivityForResult(intent, PLAY_MODULE_REQUEST);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i("Xmp PlaylistActivity", "Activity result " + requestCode + "," + resultCode);
		switch (requestCode) {
		case SETTINGS_REQUEST:
			if (resultCode == RESULT_FIRST_USER) {
				// Restart activity
				// see http://blog.janjonas.net/2010-12-20/android-development-restart-application-programmatically
				AlarmManager alarm = (AlarmManager)activity.getSystemService(Context.ALARM_SERVICE);
				alarm.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, restartIntent);
				System.exit(2);
			} else {
				update();
			}
			showToasts = prefs.getBoolean(Settings.PREF_SHOW_TOAST, true);
			break;
		case PLAY_MODULE_REQUEST:
			if (resultCode != RESULT_OK)
				update();
			break;
		}
	}

	// Connection

	private ServiceConnection connection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			modPlayer = ModInterface.Stub.asInterface(service);
			try {				
				modPlayer.add(addList);
			} catch (RemoteException e) {
				Message.toast(PlaylistActivity.this, "Error adding module");
			}
			unbindService(connection);
		}

		public void onServiceDisconnected(ComponentName className) {
			modPlayer = null;
		}
	};

	protected void addToQueue(int start, int size) {
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
			Intent service = new Intent(this, ModService.class);
			
			final String[] realList = new String[realSize];
			System.arraycopy(list,  0, realList, 0, realSize);
		
			if (ModService.isAlive) {
				addList = realList;		
				bindService(service, connection, 0);
			} else {
				playModule(realList);
			}
		}
	}

	// Menu

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);

		// Calling super after populating the menu is necessary here to ensure that the
		// action bar helpers have a chance to handle this event.
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, PlaylistMenu.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		case R.id.menu_new_playlist:
			(new PlaylistUtils()).newPlaylist(this);
			break;
		case R.id.menu_prefs:		
			startActivityForResult(new Intent(this, Settings.class), SETTINGS_REQUEST);
			break;
		case R.id.menu_refresh:
			update();
			break;
		}
		return super.onOptionsItemSelected(item);
	}	
}
