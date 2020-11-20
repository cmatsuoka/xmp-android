package org.helllabs.android.xmp.player;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.XmpApplication;
import org.helllabs.android.xmp.browser.PlaylistMenu;
import org.helllabs.android.xmp.player.viewer.ChannelViewer;
import org.helllabs.android.xmp.player.viewer.InstrumentViewer;
import org.helllabs.android.xmp.player.viewer.PatternViewer;
import org.helllabs.android.xmp.player.viewer.Viewer;
import org.helllabs.android.xmp.preferences.Preferences;
import org.helllabs.android.xmp.service.ModInterface;
import org.helllabs.android.xmp.service.PlayerCallback;
import org.helllabs.android.xmp.service.PlayerService;
import org.helllabs.android.xmp.util.FileUtils;
import org.helllabs.android.xmp.util.Log;
import org.helllabs.android.xmp.util.Message;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.net.Uri;
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


public class PlayerActivity extends Activity {
	private static final String TAG = "PlayerActivity";
	
	public static final String PARM_SHUFFLE = "shuffle";
	public static final String PARM_LOOP = "loop";
	public static final String PARM_START = "start";
	public static final String PARM_KEEPFIRST = "keepFirst";
	
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
	private boolean showElapsed;
	private boolean skipToPrevious;
	private final TextView[] infoName = new TextView[2];
	private final TextView[] infoType = new TextView[2];
	private TextView infoStatus;
	private TextView elapsedTime;
	private ViewFlipper titleFlipper;
	private int flipperPage;
	private List<String> fileList;
	private int start;
	private SharedPreferences prefs;
	private FrameLayout viewerLayout;
	private final Handler handler = new Handler();
	private int totalTime;
	private boolean screenOn;
	private Activity activity;
	private BroadcastReceiver screenReceiver;
	private Viewer viewer;
	private Viewer.Info info;
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

			synchronized (playerLock) {
				modPlayer = ModInterface.Stub.asInterface(service);
				flipperPage = 0;

				try {
					modPlayer.registerCallback(playerCallback);
				} catch (RemoteException e) {
					Log.e(TAG, "Can't register player callback");
				}

				if (fileList != null && !fileList.isEmpty()) {
					// Start new queue
					playNewMod(fileList, start);
				} else {
					// Reconnect to existing service
					try {
						showNewMod();

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
			saveAllSeqPreference();
			
			synchronized (playerLock) {
				stopUpdate = true;
				//modPlayer = null;
				Log.i(TAG, "Service disconnected");
				finish();
			}
		}
	};

	private final PlayerCallback playerCallback = new PlayerCallback.Stub() {

		@Override
		public void newModCallback() throws RemoteException {
			synchronized (playerLock) {
				Log.d(TAG, "newModCallback: show module data");
				showNewMod();
				canChangeViewer = true;
			}
		}

		@Override
		public void endModCallback() throws RemoteException {
			synchronized (playerLock) {
				Log.d(TAG, "endModCallback: end of module");
				stopUpdate = true;
				canChangeViewer = false;
			}
		}

		@Override
		public void endPlayCallback(final int result) throws RemoteException {
			synchronized (playerLock) {
				Log.d(TAG, "endPlayCallback: End progress thread");
				stopUpdate = true;
				
				if (result != PlayerService.RESULT_OK) {
				 	runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (result == PlayerService.RESULT_CANT_OPEN_AUDIO) {
								Message.toast(PlayerActivity.this, R.string.error_opensl);
							} else if (result == PlayerService.RESULT_NO_AUDIO_FOCUS) {
								Message.toast(PlayerActivity.this, R.string.error_audiofocus);
							}
						}
					});
				}

				if (progressThread != null && progressThread.isAlive()) {
					try {
						progressThread.join();
					} catch (InterruptedException e) { }
				}
				if (!isFinishing()) {
					finish();
				}
			}
		}

		@Override
		public void pauseCallback() throws RemoteException {
			Log.d(TAG, "pauseCallback");
			handler.post(setPauseStateRunnable);
		}
		
		@Override
		public void newSequenceCallback() throws RemoteException {
			synchronized (playerLock) {
				Log.d(TAG, "newSequenceCallback: show new sequence");
				showNewSequence();
			}
		}
	};

	private final Runnable setPauseStateRunnable = new Runnable() {
		@Override
		public void run() {
			synchronized (playerLock) {
				if (modPlayer != null) {
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
			}
		}
	};
	
	private final Runnable updateInfoRunnable = new Runnable() {
		private int oldSpd = -1;
		private int oldBpm = -1;
		private int oldPos = -1;
		private int oldPat = -1;
		private int oldTime = -1;
		private boolean oldShowElapsed;
		private final char[] c = new char[2];
		private final StringBuilder s = new StringBuilder();

		@Override
		public void run() {
			final boolean p = paused;

			if (!p) {
				// update seekbar
				if (!seeking && playTime >= 0) {
					seekBar.setProgress(playTime);
				}
				
				// get current frame info
				synchronized (playerLock) {
					if (modPlayer != null) {
						try {
							modPlayer.getInfo(info.values);							
							info.time = modPlayer.time() / 1000;

							modPlayer.getChannelData(info.volumes, info.finalvols, info.pans,
													 info.instruments, info.keys, info.periods);
						} catch (RemoteException e) {
							// fail silently
						}
					}
				}

				// display frame info
				if (info.values[5] != oldSpd || info.values[6] != oldBpm
						|| info.values[0] != oldPos || info.values[1] != oldPat)
				{
					// Ugly code to avoid expensive String.format()

					s.delete(0, s.length());

					s.append("Speed:");
					Util.to02X(c, info.values[5]);
					s.append(c);

					s.append(" BPM:");
					Util.to02X(c, info.values[6]);
					s.append(c);

					s.append(" Pos:");
					Util.to02X(c, info.values[0]);
					s.append(c);

					s.append(" Pat:");
					Util.to02X(c, info.values[1]);
					s.append(c);

					infoStatus.setText(s);

					oldSpd = info.values[5];
					oldBpm = info.values[6];
					oldPos = info.values[0];
					oldPat = info.values[1];
				}

				// display playback time
				if (info.time != oldTime || showElapsed != oldShowElapsed) {
					int t = info.time;
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

					oldTime = info.time;
					oldShowElapsed = showElapsed;
				}
			} // !p

			// always call viewer update (for scrolls during pause)
			synchronized (viewerLayout) {
				viewer.update(info, p);
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
				if (stopUpdate) {
					Log.i(TAG, "Stop update");
					break;
				}
				
				synchronized (playerLock) {
					if (modPlayer != null) {
						try {
							playTime = modPlayer.time() / 100;
						} catch (RemoteException e) {
							// fail silently
						}
					}
				}

				if (screenOn) {
					handler.post(updateInfoRunnable);
				}

				try {
					while ((now = System.nanoTime()) - lastTimer < frameTime && !stopUpdate) {
						sleep(10);
					}
					lastTimer = now;
				} catch (InterruptedException e) { }
			} while (playTime >= 0);

			handler.removeCallbacksAndMessages(null);
			handler.post(new Runnable() {
				@Override
				public void run() {
					synchronized (playerLock) {
						if (modPlayer != null) {
							Log.i(TAG, "Flush interface update");
							try {
								modPlayer.allowRelease();		// finished playing, we can release the module
							} catch (RemoteException e) {
								Log.e(TAG, "Can't allow module release");
							}
						}
					}
				}
			});
		}
	}

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
			viewer.setRotation(display.getRotation());
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
			if (intent.getAction().equals(Intent.ACTION_VIEW)) {
				path = handleIntentAction(intent);
			} else {
				path = intent.getData().getPath();
			}
		}

		//fileArray = null;

		if (path != null) {		// from intent filter
			Log.i(TAG, "Player started from intent filter");
			fileList = new ArrayList<>();
			fileList.add(path);
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
				//fileArray = extras.getStringArray("files");
				final XmpApplication app = (XmpApplication)getApplication();
				fileList = app.getFileList();
				shuffleMode = extras.getBoolean(PARM_SHUFFLE);
				loopListMode = extras.getBoolean(PARM_LOOP);
				keepFirst = extras.getBoolean(PARM_KEEPFIRST);
				start = extras.getInt(PARM_START);
				app.clearFileList();
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
		}
	}

	private String handleIntentAction(Intent intent)  {
		Log.d(TAG,"Handing incoming intent");

		Uri uri = intent.getData();
		String uriString;
		if (uri != null) {
			uriString = uri.toString();
		} else {
			return null;
		}

		String fileName = "temp." + uriString.substring(uriString.lastIndexOf('.') + 1);
		File output = new File(this.getExternalCacheDir(), fileName);

		// Lets delete  the temp file to ensure a clean copy.
		if (output.exists()) {
			if(output.delete()) {
				Log.d(TAG, "Temp file deleted.");
			} else {
				Log.e(TAG, "Failed to delete temp file!");
			}
		}

		try{
			InputStream inputStream = getContentResolver().openInputStream(uri);
			OutputStream outputStream = new FileOutputStream(output);
			byte[] buffer = new byte[1024];
			int length;

			while((length=inputStream.read(buffer)) > 0) {
				outputStream.write(buffer,0,length);
			}

			outputStream.close();
			inputStream.close();
		}catch (IOException e) {
			Log.e(TAG, "Error creating temp file --Check Trace--");
			e.printStackTrace();
			return null;
		}

		return output.getPath();
	}

	//private void setFont(final TextView name, final String path, final int res) {
	//    final Typeface typeface = Typeface.createFromAsset(this.getAssets(), path); 
	//    name.setTypeface(typeface); 
	//}

	private void changeViewer() {
		currentViewer++;
		currentViewer %= 3;

		synchronized (viewerLayout) {
			synchronized (playerLock) {
				if (modPlayer != null) {
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
					viewer.setRotation(display.getRotation());
				}
			}
		}
	}
	

	// Sidebar services
	
	public boolean toggleAllSequences() {
		synchronized (playerLock) {
			if (modPlayer != null) {
				try {
					return modPlayer.toggleAllSequences();
				} catch (RemoteException e) {
					Log.e(TAG, "Can't toggle all sequences status");
				}
			}
			return false;
		}
	}

	public boolean getAllSequences() {
		if (modPlayer != null) {
			try {
				return modPlayer.getAllSequences();
			} catch (RemoteException e) {
				Log.e(TAG, "Can't get all sequences status");
			}
		}
		return false;
	}


	// Click listeners
	
	public void loopButtonListener(final View view) {
		synchronized (playerLock) {
			if (modPlayer != null) {
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
		}
	}

	public void playButtonListener(final View view) {
		//Debug.startMethodTracing("xmp");				
		synchronized (this) {
			Log.d(TAG, "Play/pause button pressed (paused=" + paused + ")");
			if (modPlayer != null) {
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
	}

	public void stopButtonListener(final View view) {
		//Debug.stopMethodTracing();

		synchronized (playerLock) {
			Log.d(TAG, "Stop button pressed");
			if (modPlayer != null) {	
				try {
					modPlayer.stop();
				} catch (RemoteException e1) {
					Log.e(TAG, "Can't stop module");
				}
			}
		}

		paused = false;

//		if (progressThread != null && progressThread.isAlive()) {
//			try {
//				progressThread.join();
//			} catch (InterruptedException e) { }
//		}
	}

	public void backButtonListener(final View view) {
		synchronized (playerLock) {
			Log.d(TAG, "Back button pressed");
			if (modPlayer != null) {
				try {
					if (modPlayer.time() > 3000) {
						modPlayer.seek(0);
						if (paused) {
							modPlayer.pause();
						}
					} else {
						modPlayer.prevSong();
						skipToPrevious = true;
					}
					unpause();
				} catch (RemoteException e) {
					Log.e(TAG, "Can't go to previous module");
				}
			}
		}
	}

	public void forwardButtonListener(final View view) {
		synchronized (playerLock) {
			Log.d(TAG, "Next button pressed");
			if (modPlayer != null) {
				try {
					modPlayer.nextSong();
					unpause();
				} catch (RemoteException e) {
					Log.e(TAG, "Can't go to next module");
				}
			}
		}
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


	private void saveAllSeqPreference() {
		synchronized (playerLock) {
			if (modPlayer != null) {
				try {
					// Write our all sequences button status to shared prefs
					final boolean allSeq = modPlayer.getAllSequences();
					if (allSeq != prefs.getBoolean(Preferences.ALL_SEQUENCES, false)) {
						Log.w(TAG, "Write all sequences preference");
						final SharedPreferences.Editor editor = prefs.edit();
						editor.putBoolean(Preferences.ALL_SEQUENCES, allSeq);
						editor.apply();
					}
				} catch (RemoteException e) {
					Log.e(TAG, "Can't save all sequences preference");
				}
			}
		}
	}
	
	@Override
	public void onDestroy() {		
		//if (deleteDialog != null) {
		//	deleteDialog.cancel();
		//}

		saveAllSeqPreference();
		
		synchronized (playerLock) {
			if (modPlayer != null) {
				try {
					modPlayer.unregisterCallback(playerCallback);
				} catch (RemoteException e) {
					Log.e(TAG, "Can't unregister player callback");
				}
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
	
	public void playNewSequence(final int num) {
		synchronized (playerLock) {
			if (modPlayer != null) {
				try {
					Log.i(TAG, "Set sequence " + num);
					modPlayer.setSequence(num);
				} catch (RemoteException e) {
					Log.e(TAG, "Can't set sequence " + num);
				}
			}
		}
	}
	
	private void showNewSequence() {
		synchronized (playerLock) {
			if (modPlayer != null) {
				try {
					modPlayer.getModVars(modVars);
				} catch (RemoteException e) {
					Log.e(TAG, "Can't get new sequence data");
				}

				handler.post(showNewSequenceRunnable);
			}
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

	private void showNewMod() {
		//if (deleteDialog != null) {
		//	deleteDialog.cancel();
		//}
		handler.post(showNewModRunnable);
	}
	
	private final Runnable showNewModRunnable = new Runnable() {
		public void run() {
			Log.i(TAG, "Show new module");

			synchronized (playerLock) {
				if (modPlayer != null) {
					try {
						modPlayer.getModVars(modVars);
						modPlayer.getSeqVars(seqVars);
						playTime = modPlayer.time() / 100;
					} catch (RemoteException e) {
						Log.e(TAG, "Can't get module data");
						return;
					}

					String name;
					String type;
					boolean allSeq;
					boolean loop;
					try {
						name = modPlayer.getModName();
						type = modPlayer.getModType();
						allSeq = modPlayer.getAllSequences();
						loop = modPlayer.getLoop();

						if (name.trim().isEmpty()) {
							name = FileUtils.basename(modPlayer.getFileName());
						}
					} catch (RemoteException e) {
						name = "";
						type = "";
						allSeq = false;
						loop = false;
						Log.e(TAG, "Can't get module name and type");
					}
					final int time = modVars[0];
					/*int len = vars[1]; */
					final int pat = modVars[2];
					final int chn = modVars[3];
					final int ins = modVars[4];
					final int smp = modVars[5];
					final int numSeq = modVars[6];


					sidebar.setDetails(pat, ins, smp, chn, allSeq);
					sidebar.clearSequences();
					for (int i = 0; i < numSeq; i++) {
						sidebar.addSequence(i, seqVars[i]);
					}
					sidebar.selectSequence(0);

					loopButton.setImageResource(loop ? R.drawable.loop_on : R.drawable.loop_off);

					totalTime = time / 1000;
					seekBar.setMax(time / 100);
					seekBar.setProgress(playTime);

					flipperPage = (flipperPage + 1) % 2;

					infoName[flipperPage].setText(name);
					infoType[flipperPage].setText(type);
					
					if (skipToPrevious) {
						titleFlipper.setInAnimation(PlayerActivity.this, R.anim.slide_in_left_slow);
						titleFlipper.setOutAnimation(PlayerActivity.this, R.anim.slide_out_right_slow);
					} else {
						titleFlipper.setInAnimation(PlayerActivity.this, R.anim.slide_in_right_slow);
						titleFlipper.setOutAnimation(PlayerActivity.this, R.anim.slide_out_left_slow);
					}
					skipToPrevious = false;

					titleFlipper.showNext();

					viewer.setup(modPlayer, modVars);
					viewer.setRotation(display.getRotation());

					/*infoMod.setText(String.format("Channels: %d\n" +
		       			"Length: %d, Patterns: %d\n" +
		       			"Instruments: %d, Samples: %d\n" +
		       			"Estimated play time: %dmin%02ds",
		       			chn, len, pat, ins, smp,
		       			((time + 500) / 60000), ((time + 500) / 1000) % 60));*/

					info = new Viewer.Info();

					stopUpdate = false;
					if (progressThread == null || !progressThread.isAlive()) {
						progressThread = new ProgressThread();
						progressThread.start();
					}
				}
			}
		}
	};

	private void playNewMod(final List<String> fileList, final int start) {
		synchronized (playerLock) {
			if (modPlayer != null) {
				try {
					modPlayer.play(fileList, start, shuffleMode, loopListMode, keepFirst);
				} catch (RemoteException e) {
					Log.e(TAG, "Can't play module");
				}
			}
		}
	}


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
			Message.yesNoDialog(activity, "Delete", "Are you sure to delete this file?", new Runnable() {
				@Override
				public void run() {
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
			});
		}
		return true;
	}
}
