package org.helllabs.android.xmp.player;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.browser.Message;
import org.helllabs.android.xmp.browser.PlaylistMenu;
import org.helllabs.android.xmp.preferences.Preferences;
import org.helllabs.android.xmp.service.ModInterface;
import org.helllabs.android.xmp.service.PlayerCallback;
import org.helllabs.android.xmp.service.PlayerService;
import org.helllabs.android.xmp.util.Log;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ViewFlipper;


@SuppressWarnings("PMD.ShortVariable")
public class PlayerActivity extends Activity {
	private static final String TAG = "PlayerActivity";
	private ModInterface modPlayer;	/* actual mod player */
	private ImageButton playButton;
	private ImageButton loopButton;
	private SeekBar seekBar;
	private Thread progressThread;
	private boolean seeking;
	private boolean shuffleMode;
	private boolean loopListMode;
	private boolean keepFirst;
	private boolean paused;
	private boolean finishing;
	private boolean showElapsed;
	private final TextView[] infoName = new TextView[2];
	private final TextView[] infoType = new TextView[2];
	private TextView infoStatus;
	private TextView elapsedTime;
	private ViewFlipper titleFlipper;
	private int flipperPage;
	private String[] fileArray;
	private int start;
	private SharedPreferences prefs;
	private FrameLayout viewerLayout;
	private final Handler handler = new Handler();
	private int latency;
	private int totalTime;
	private boolean screenOn;
	private Activity activity;
	//private AlertDialog deleteDialog;
	private BroadcastReceiver screenReceiver;
	private Viewer viewer;
	private Viewer.Info[] info;
	private final int[] modVars = new int[10];
	private final int[] seqVars = new int[16];		// this is MAX_SEQUENCES defined in common.h
	private static final int FRAME_RATE = 25;
	private static boolean stopUpdate;				// this MUST be static (volatile doesn't work!)
	private static boolean canChangeViewer;
	private int currentViewer;
	private Display display;
	private Viewer instrumentViewer;
	private Viewer channelViewer;
	private Viewer patternViewer;
	private int playTime;
	private final Object playerLock = new Object();		// for sync
	private Sidebar sidebar;

	private final ServiceConnection connection = new ServiceConnection() {

		public void onServiceConnected(final ComponentName className, final IBinder service) {
			Log.i(TAG, "Service connected");

			modPlayer = ModInterface.Stub.asInterface(service);
			flipperPage = 0;

			synchronized (playerLock) {
				try {
					modPlayer.registerCallback(playerCallback);
				} catch (RemoteException e) {
					Log.e(TAG, "Can't register player callback");
				}

				if (fileArray != null && fileArray.length > 0) {
					// Start new queue
					playNewMod(fileArray, start);
				} else {
					// Reconnect to existing service
					try {
						showNewMod(modPlayer.getFileName());

						if (modPlayer.isPaused()) {
							pause();
						} else {
							unpause();
						}
					} catch (RemoteException e) {
						Log.e(TAG, "Can't get module file name");
					}
				}
			}
		}

		public void onServiceDisconnected(final ComponentName className) {
			stopUpdate = true;
			modPlayer = null;		// NOPMD
			Log.i(TAG, "Service disconnected");
		}
	};

	private final PlayerCallback playerCallback = new PlayerCallback.Stub() {

		@Override
		public void newModCallback(final String name, final String[] instruments) throws RemoteException {
			synchronized (playerLock) {
				Log.i(TAG, "Show module data");
				showNewMod(name);
				canChangeViewer = true;
			}
		}

		@Override
		public void endModCallback() throws RemoteException {
			synchronized (playerLock) {
				Log.i(TAG, "End of module");
				stopUpdate = true;
				canChangeViewer = false;
			}
		}

		@Override
		public void endPlayCallback() throws RemoteException {
			Log.i(TAG, "End progress thread");
			stopUpdate = true;

			if (progressThread != null && progressThread.isAlive()) {
				try {
					progressThread.join();
				} catch (InterruptedException e) { }
			}
			finish();
		}

		@Override
		public void pauseCallback() throws RemoteException {
			handler.post(setPauseStateRunnable);
		}
		
		@Override
		public void newSequenceCallback() throws RemoteException {
			synchronized (playerLock) {
				Log.i(TAG, "Show new sequence");
				showNewSequence();
			}
		}
	};

	private final Runnable setPauseStateRunnable = new Runnable() {

		@Override
		public void run() {
			try {
				// Set pause status according to external state
				if (modPlayer.isPaused()) {
					pause();
				} else {
					unpause();
				}
			} catch (RemoteException e) {
				Log.e(TAG, "Can't get pause status");
			}
		}
	};

	private final Runnable updateInfoRunnable = new Runnable() {
		private int oldSpd = -1;
		private int oldBpm = -1;
		private int oldPos = -1;
		private int oldPat = -1;
		private int oldTime = -1;
		private int before, now;
		private boolean oldShowElapsed;
		private final char[] c = new char[2];
		private StringBuilder s = new StringBuilder();

		@Override
		public void run() {
			final boolean p = paused;
			
			now = (before + FRAME_RATE * latency / 1000 + 1) % FRAME_RATE;

			if (!p) {
				
				// update seekbar
				if (!seeking && playTime >= 0) {
					seekBar.setProgress(playTime);
				}
				
				// get current frame info
				synchronized (playerLock) {
					try {
						modPlayer.getInfo(info[now].values);							
						info[now].time = modPlayer.time() / 1000;

						modPlayer.getChannelData(info[now].volumes, info[now].finalvols, info[now].pans,
								info[now].instruments, info[now].keys, info[now].periods);
					} catch (Exception e) {
						// fail silently
					}
				}

				// display frame info
				if (info[before].values[5] != oldSpd || info[before].values[6] != oldBpm
						|| info[before].values[0] != oldPos || info[before].values[1] != oldPat)
				{
					// Ugly code to avoid expensive String.format()

					s.delete(0, s.length());

					s.append("Speed:");
					Util.to02X(c, info[before].values[5]);
					s.append(c);

					s.append(" BPM:");
					Util.to02X(c, info[before].values[6]);
					s.append(c);

					s.append(" Pos:");
					Util.to02X(c, info[before].values[0]);
					s.append(c);

					s.append(" Pat:");
					Util.to02X(c, info[before].values[1]);
					s.append(c);

					infoStatus.setText(s);

					oldSpd = info[before].values[5];
					oldBpm = info[before].values[6];
					oldPos = info[before].values[0];
					oldPat = info[before].values[1];
				}

				// display playback time
				if (info[before].time != oldTime || showElapsed != oldShowElapsed) {
					int t = info[before].time;
					if (t < 0) {
						t = 0;
					}

					s.delete(0, s.length());

					if (showElapsed) {	
						Util.to2d(c, t / 60);
						s.append(c);
						s.append(':');
						Util.to02d(c, t % 60);
						s.append(c);

						elapsedTime.setText(s);
					} else {
						t = totalTime - t;

						s.append('-');
						Util.to2d(c, t / 60);
						s.append(c);
						s.append(':');
						Util.to02d(c, t % 60);
						s.append(c);

						elapsedTime.setText(s);
					}

					oldTime = info[before].time;
					oldShowElapsed = showElapsed;
				}
			} // !p

			// always call viewer update (for scrolls during pause)
			synchronized (viewerLayout) {
				viewer.update(info[before], p);
			}

			// update latency compensation
			if (!p) {
				before++;
				if (before >= FRAME_RATE) {
					before = 0;
				}
			}
		}
	};

	private class ProgressThread extends Thread {

		@Override
		public void run() {
			Log.i(TAG, "Start progress thread");

			final long frameTime = 1000000000 / FRAME_RATE;
			long lastTimer = System.nanoTime();
			long now;

			playTime = 0;

			do {
				synchronized (playerLock) {
					if (stopUpdate) {
						Log.i(TAG, "Stop update");
						break;
					}

					try {
						playTime = modPlayer.time() / 100;
					} catch (RemoteException e) {
						// fail silently
					}
				}

				if (/* !paused && */ screenOn) {
					// also show information when paused so scrolls work
					handler.post(updateInfoRunnable);
				}

				try {
					while ((now = System.nanoTime()) - lastTimer < frameTime && !stopUpdate) {
						sleep(10);
					}
					lastTimer = now;
				} catch (InterruptedException e) { }
			} while (playTime >= 0);

			seekBar.setProgress(0);
			try {
				modPlayer.allowRelease();		// finished playing, we can release the module
			} catch (RemoteException e) {
				Log.e(TAG, "Can't allow module release");
			}
		}
	};

	private void pause() {
		paused = true;
		playButton.setImageResource(R.drawable.play);
	}

	private void unpause() {
		paused = false;
		playButton.setImageResource(R.drawable.pause);
	}

	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (viewer != null) {
			viewer.setRotation(display.getOrientation());
		}
	}

	@Override
	protected void onNewIntent(final Intent intent) {
		boolean reconnect = false;
		boolean fromHistory = false;

		Log.i(TAG, "New intent");

		if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
			Log.i(TAG, "Player started from history");
			fromHistory = true;
		}

		String path = null;
		if (intent.getData() != null) {
			path = intent.getData().getPath();
		}

		fileArray = null;

		if (path != null) {		// from intent filter
			Log.i(TAG, "Player started from intent filter");
			fileArray = new String[1];
			fileArray[0] = path;
			shuffleMode = false;
			loopListMode = false;
			keepFirst = false;
			start = 0;
		} else if (fromHistory) {
			// Oops. We don't want to start service if launched from history and service is not running
			// so run the browser instead.
			Log.i(TAG, "Start file browser");
			final Intent browserIntent = new Intent(this, PlaylistMenu.class);
			browserIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(browserIntent);
			finish();
			return;
		} else {	
			final Bundle extras = intent.getExtras();
			if (extras != null) {
				fileArray = extras.getStringArray("files");	
				shuffleMode = extras.getBoolean("shuffle");
				loopListMode = extras.getBoolean("loop");
				keepFirst = extras.getBoolean("keepFirst");
				start = extras.getInt("start");
			} else {
				reconnect = true;
			}
		}
		
		final Intent service = new Intent(this, PlayerService.class);
		if (!reconnect) {
			Log.i(TAG, "Start service");
			startService(service);
		}

		if (!bindService(service, connection, 0)) {
			Log.e(TAG, "Can't bind to service");
			finish();
			return;
		}
	}

	//private void setFont(final TextView name, final String path, final int res) {
	//    final Typeface typeface = Typeface.createFromAsset(this.getAssets(), path); 
	//    name.setTypeface(typeface); 
	//}

	private void changeViewer() {
		currentViewer++;
		currentViewer %= 3;

		synchronized (viewerLayout) {
			viewerLayout.removeAllViews();
			switch (currentViewer) {
			case 0:
				viewer = instrumentViewer;
				break;
			case 1:
				viewer = channelViewer;
				break;
			case 2:
				viewer = patternViewer;
				break;
			}

			viewerLayout.addView(viewer);   		
			viewer.setup(modPlayer, modVars);
			viewer.setRotation(display.getOrientation());
		}
	}


	// Click listeners

	public void loopButtonListener(final View view) {
		try {
			if (modPlayer.toggleLoop()) {
				loopButton.setImageResource(R.drawable.loop_on);
			} else {
				loopButton.setImageResource(R.drawable.loop_off);
			}
		} catch (RemoteException e) {
			Log.e(TAG, "Can't get loop status");
		}
	}

	public void playButtonListener(final View view) {
		//Debug.startMethodTracing("xmp");				
		if (modPlayer == null) {
			return;
		}

		synchronized (this) {
			try {
				modPlayer.pause();

				if (paused) {
					unpause();
				} else {
					pause();
				}
			} catch (RemoteException e) {
				Log.e(TAG, "Can't pause/unpause module");
			}
		}
	}

	public void stopButtonListener(final View view) {
		//Debug.stopMethodTracing();
		if (modPlayer == null) {
			return;
		}

		stopPlayingMod();
	}

	public void backButtonListener(final View view) {
		if (modPlayer == null) {
			return;
		}

		try {
			if (modPlayer.time() > 3000) {
				modPlayer.seek(0);
			} else {
				synchronized (playerLock) {
					modPlayer.prevSong();
				}
			}
			unpause();
		} catch (RemoteException e) {
			Log.e(TAG, "Can't go to previous module");
		}
	}

	public void forwardButtonListener(final View view) {				
		if (modPlayer == null) {
			return;
		}

		try {
			synchronized (playerLock) {
				modPlayer.nextSong();
			}
		} catch (RemoteException e) {
			Log.e(TAG, "Can't go to next module");
		}

		unpause();
	}

	// Life cycle

	@Override
	public void onCreate(final Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.player_main);
		
		sidebar = new Sidebar(this);

		activity = this;
		display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

		Log.i(TAG, "Create player interface");

		// INITIALIZE RECEIVER by jwei512
		final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		screenReceiver = new ScreenReceiver();
		registerReceiver(screenReceiver, filter);

		screenOn = true;

		if (PlayerService.isLoaded) {
			canChangeViewer = true;
		}

		setResult(RESULT_OK);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		final boolean showInfoLine = prefs.getBoolean(Preferences.SHOW_INFO_LINE, true);
		showElapsed = true;

		latency = prefs.getInt(Preferences.BUFFER_MS, 500);
		if (latency > 1000) {
			latency = 1000;
		}

		onNewIntent(getIntent());

		infoName[0] = (TextView)findViewById(R.id.info_name_0);
		infoType[0] = (TextView)findViewById(R.id.info_type_0);
		infoName[1] = (TextView)findViewById(R.id.info_name_1);
		infoType[1] = (TextView)findViewById(R.id.info_type_1);
		infoStatus = (TextView)findViewById(R.id.info_status);
		elapsedTime = (TextView)findViewById(R.id.elapsed_time);
		titleFlipper = (ViewFlipper)findViewById(R.id.title_flipper);
		viewerLayout = (FrameLayout)findViewById(R.id.viewer_layout);

		viewer = new InstrumentViewer(this);
		viewerLayout.addView(viewer);
		viewerLayout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View view) {
				synchronized (playerLock) {
					if (canChangeViewer) {
						changeViewer();
					}
				}
			}
		});		

		if (prefs.getBoolean(Preferences.KEEP_SCREEN_ON, false)) {
			titleFlipper.setKeepScreenOn(true);
		}

		titleFlipper.setInAnimation(this, R.anim.slide_in_right);
		titleFlipper.setOutAnimation(this, R.anim.slide_out_left);

		final Typeface font = Typeface.createFromAsset(this.getAssets(), "fonts/Michroma.ttf");

		for (int i = 0; i < 2; i++) {
			infoName[i].setTypeface(font);
			infoName[i].setIncludeFontPadding(false);
			infoType[i].setTypeface(font);
			infoType[i].setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
		}

		if (!showInfoLine) {
			infoStatus.setVisibility(LinearLayout.GONE);
			elapsedTime.setVisibility(LinearLayout.GONE);
		}

		playButton = (ImageButton)findViewById(R.id.play);
		loopButton = (ImageButton)findViewById(R.id.loop);

		loopButton.setImageResource(R.drawable.loop_off);

		elapsedTime.setOnClickListener(new OnClickListener() {
			public void onClick(final View view) {
				showElapsed ^= true;
			}
		});

		seekBar = (SeekBar)findViewById(R.id.seek);
		seekBar.setProgress(0);

		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			public void onProgressChanged(final SeekBar s, final int p, final boolean b) {
				// do nothing
			}

			public void onStartTrackingTouch(final SeekBar s) {
				seeking = true;
			}

			public void onStopTrackingTouch(final SeekBar s) {
				if (modPlayer != null) {
					try {
						modPlayer.seek(s.getProgress() * 100);
						playTime = modPlayer.time() / 100;
					} catch (RemoteException e) {
						Log.e(TAG, "Can't seek to time");
					}
				}
				seeking = false;
			}
		});

		instrumentViewer = new InstrumentViewer(this);
		channelViewer = new ChannelViewer(this);
		patternViewer = new PatternViewer(this);
	}


	@Override
	public void onDestroy() {		
		//if (deleteDialog != null) {
		//	deleteDialog.cancel();
		//}

		if (modPlayer != null) {
			try {
				modPlayer.unregisterCallback(playerCallback);
			} catch (RemoteException e) {
				Log.e(TAG, "Can't unregister player callback");
			}
		}

		unregisterReceiver(screenReceiver);

		try {
			unbindService(connection);
			Log.i(TAG, "Unbind service");
		} catch (IllegalArgumentException e) {
			Log.i(TAG, "Can't unbind unregistered service");
		}

		super.onDestroy();
	}

	/*
	 * Stop screen updates when screen is off
	 */
	@Override
	protected void onPause() {
		// Screen is about to turn off
		if (ScreenReceiver.wasScreenOn) {
			screenOn = false;
		} //else {
		// Screen state not changed
		//}
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		screenOn = true;
	}
	
	public void playNewSequence(int num) {
		synchronized (playerLock) {
			try {
				Log.i(TAG, "Set sequence " + num);
				modPlayer.setSequence(num);
			} catch (RemoteException e) {
				Log.e(TAG, "Can't set sequence " + num);
			}
		}
	}
	
	private void showNewSequence() {
		synchronized (playerLock) {
			try {
				modPlayer.getModVars(modVars);
			} catch (RemoteException e) {
				Log.e(TAG, "Can't get new sequence data");
			}
			
			handler.post(showNewSequenceRunnable);
		}
	}
	
	private final Runnable showNewSequenceRunnable = new Runnable() {
		public void run() {
			final int time = modVars[0];
			totalTime = time / 1000;
			seekBar.setProgress(0);
			seekBar.setMax(time / 100);
			Message.toast(activity, "New sequence duration: " +
						String.format("%d:%02d", time / 60000, (time / 1000) % 60));
			
			final int sequence = modVars[7];
			sidebar.selectSequence(sequence);
		}
	};

	private void showNewMod(final String fileName) {
		//if (deleteDialog != null) {
		//	deleteDialog.cancel();
		//}
		handler.post(showNewModRunnable);
	}
	
	private final Runnable showNewModRunnable = new Runnable() {
		public void run() {
			Log.i(TAG, "Show new module");

			synchronized (playerLock) {
				try {
					modPlayer.getModVars(modVars);
					modPlayer.getSeqVars(seqVars);
				} catch (RemoteException e) {
					Log.e(TAG, "Can't get module data");
					return;
				}

				String name;
				String type;
				try {
					name = modPlayer.getModName();
					type = modPlayer.getModType();

					if (name.trim().length() == 0) {
						name = "<untitled>";
					}
				} catch (RemoteException e) {
					name = "";
					type = "";
					Log.e(TAG, "Can't get module name and type");
				}
				final int time = modVars[0];
				/*int len = vars[1]; */
				final int pat = modVars[2];
				final int chn = modVars[3];
				final int ins = modVars[4];
				final int smp = modVars[5];
				final int numSeq = modVars[6];
				
				sidebar.setDetails(pat, ins, smp, chn);
				sidebar.clearSequences();
				for (int i = 0; i < numSeq; i++) {
					sidebar.addSequence(i, seqVars[i]);
				}
				sidebar.selectSequence(0);

				totalTime = time / 1000;
				seekBar.setProgress(0);
				seekBar.setMax(time / 100);

				flipperPage = (flipperPage + 1) % 2;

				infoName[flipperPage].setText(name);
				infoType[flipperPage].setText(type);

				titleFlipper.showNext();

				viewer.setup(modPlayer, modVars);
				viewer.setRotation(display.getOrientation());

				/*infoMod.setText(String.format("Channels: %d\n" +
		       			"Length: %d, Patterns: %d\n" +
		       			"Instruments: %d, Samples: %d\n" +
		       			"Estimated play time: %dmin%02ds",
		       			chn, len, pat, ins, smp,
		       			((time + 500) / 60000), ((time + 500) / 1000) % 60));*/

				info = new Viewer.Info[FRAME_RATE];
				for (int i = 0; i < FRAME_RATE; i++) {
					info[i] = new Viewer.Info();
				}

				stopUpdate = false;
				if (progressThread == null || !progressThread.isAlive()) {
					progressThread = new ProgressThread();
					progressThread.start();
				}
			}
		}
	};

	private void playNewMod(final String[] files, final int start) {      	 
		try {
			modPlayer.play(files, start, shuffleMode, loopListMode, keepFirst);
		} catch (RemoteException e) {
			Log.e(TAG, "Can't play module");
		}
	}

	private void stopPlayingMod() {
		if (finishing) {
			return;
		}
		finishing = true;

		synchronized (playerLock) {
			try {
				modPlayer.stop();
			} catch (RemoteException e1) {
				Log.e(TAG, "Can't stop module");
			}
		}

		paused = false;

		if (progressThread != null && progressThread.isAlive()) {
			try {
				progressThread.join();
			} catch (InterruptedException e) { }
		}
	}

	private final DialogInterface.OnClickListener deleteDialogClickListener = new DialogInterface.OnClickListener() {
		public void onClick(final DialogInterface dialog, final int which) {
			if (which == DialogInterface.BUTTON_POSITIVE) {
				try {
					if (modPlayer.deleteFile()) {
						Message.toast(activity, "File deleted");
						setResult(RESULT_FIRST_USER);
						modPlayer.nextSong();
					} else {
						Message.toast(activity, "Can\'t delete file");
					}
				} catch (RemoteException e) {
					Message.toast(activity, "Can\'t connect service");
				}
			}
		}
	};

	// Menu

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		if (prefs.getBoolean(Preferences.ENABLE_DELETE, false)) {
			final MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.player_menu, menu);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if (item.getItemId() == R.id.menu_delete) {
			Message.yesNoDialog(activity, "Delete", "Are you sure to delete this file?", deleteDialogClickListener);
		}
		return true;
	}	
}
