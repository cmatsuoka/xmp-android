package org.helllabs.android.xmp.service;

import java.util.List;

import org.helllabs.android.xmp.Xmp;
import org.helllabs.android.xmp.preferences.Preferences;
import org.helllabs.android.xmp.service.receiver.BluetoothConnectionReceiver;
import org.helllabs.android.xmp.service.receiver.HeadsetPlugReceiver;
import org.helllabs.android.xmp.service.receiver.MediaButtonsReceiver;
import org.helllabs.android.xmp.service.receiver.NotificationActionReceiver;
import org.helllabs.android.xmp.service.utils.Notifier;
import org.helllabs.android.xmp.service.utils.QueueManager;
import org.helllabs.android.xmp.service.utils.RemoteControl;
import org.helllabs.android.xmp.service.utils.Watchdog;
import org.helllabs.android.xmp.util.InfoCache;
import org.helllabs.android.xmp.util.Log;

import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;


public final class PlayerService extends Service implements OnAudioFocusChangeListener {
	private static final String TAG = "PlayerService";
	private static final int CMD_NONE = 0;
	private static final int CMD_NEXT = 1;
	private static final int CMD_PREV = 2;
	private static final int CMD_STOP = 3;

	private static final int MIN_BUFFER_MS = 80;
	private static final int MAX_BUFFER_MS = 1000;
	private static final int DEFAULT_BUFFER_MS = 400;

	private AudioManager audioManager;
	private RemoteControl remoteControl;
	
	private int bufferMs;
	private Thread playThread;
	private SharedPreferences prefs;
	private Watchdog watchdog;
	private int sampleRate;
	private Notifier notifier;
	private int cmd;
	private boolean restart;
	private boolean canRelease;
	private boolean paused;
	private boolean looped;
	private boolean allSequences;
	private int startIndex;
	private boolean updateData;
	private String fileName;			// currently playing file
	private QueueManager queue;
	private final RemoteCallbackList<PlayerCallback> callbacks =
			new RemoteCallbackList<PlayerCallback>();
	private int sequenceNumber;

	// Telephony autopause
	private boolean autoPaused;			// paused on phone call
	private boolean previousPaused;		// save previous pause state

	// Headset autopause
	private HeadsetPlugReceiver headsetPlugReceiver;
	private boolean headsetPause;

	// Bluetooth autopause
	private BluetoothConnectionReceiver bluetoothConnectionReceiver;

	// Media buttons
	//private MediaButtons mediaButtons;

	public static boolean isAlive;
	public static boolean isLoaded;


	@Override
	public void onCreate() {
		super.onCreate();

		Log.i(TAG, "Create service");

		audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

		//Request audio focus
		final int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
		if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
			stopSelf();
			return;
		}
		
		remoteControl = new RemoteControl(this, audioManager);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		if (prefs.getBoolean(Preferences.HEADSET_PAUSE, true)) {
			Log.i(TAG, "Register headset receiver");
			// For listening to headset changes, the broadcast receiver cannot be
			// declared in the manifest, it must be dynamically registered. 
			headsetPlugReceiver = new HeadsetPlugReceiver();
			registerReceiver(headsetPlugReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
		}

		if (prefs.getBoolean(Preferences.BLUETOOTH_PAUSE, true)) {
			Log.i(TAG, "Register bluetooth receiver");
			bluetoothConnectionReceiver = new BluetoothConnectionReceiver();
			final IntentFilter filter = new IntentFilter();
			filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
			filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
			filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
			if (Build.VERSION.SDK_INT >= 11) {
				filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
			}
			registerReceiver(bluetoothConnectionReceiver, filter);
		}

		bufferMs = prefs.getInt(Preferences.BUFFER_MS, DEFAULT_BUFFER_MS);
		if (bufferMs < MIN_BUFFER_MS) {
			bufferMs = MIN_BUFFER_MS;
		} else if (bufferMs > MAX_BUFFER_MS) {
			bufferMs = MAX_BUFFER_MS;
		}

		sampleRate = Integer.parseInt(prefs.getString(Preferences.SAMPLING_RATE, "44100"));

		Xmp.init();

		isAlive = false;
		isLoaded = false;
		paused = false;
		allSequences = prefs.getBoolean(Preferences.ALL_SEQUENCES, false);

		notifier = new Notifier(this);

		final XmpPhoneStateListener listener = new XmpPhoneStateListener(this);
		final TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		tm.listen(listener, XmpPhoneStateListener.LISTEN_CALL_STATE);

		//mediaButtons = new MediaButtons(this);
		//mediaButtons.register();

		watchdog = new Watchdog(5);
		watchdog.setOnTimeoutListener(new Watchdog.OnTimeoutListener() {
			public void onTimeout() {
				Log.e(TAG, "Stopped by watchdog");
				audioManager.abandonAudioFocus(PlayerService.this);
				stopSelf();
			}
		});
		watchdog.start();
	}

	@Override
	public int onStartCommand(final Intent intent, final int flags, final int startId) {
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		if (headsetPlugReceiver != null) {
			unregisterReceiver(headsetPlugReceiver);
		}
		if (bluetoothConnectionReceiver != null) {		// Z933 (glaucus) needs this test
			unregisterReceiver(bluetoothConnectionReceiver);
		}
		//mediaButtons.unregister();
		watchdog.stop();
		notifier.cancel();
		end();
		super.onDestroy();
	}

	@Override
	public IBinder onBind(final Intent intent) {
		return binder;
	}

	private void updateNotification() {
		if (queue != null) {	// It seems that queue can be null if we're called from PhoneStateListener
			if (paused) {
				notifier.pauseNotification(Xmp.getModName(), queue.getIndex());
			} else {
				notifier.unpauseNotification(Xmp.getModName(), queue.getIndex());
			}
		}
	}

	private void doPauseAndNotify() {
		paused ^= true;
		updateNotification();
		if (paused) {
			Xmp.stopAudio();
			remoteControl.setStatePaused();
		} else {
			remoteControl.setStatePlaying();
			Xmp.restartAudio();
		}
	}

	private void actionStop() {
		Xmp.stopModule();
		paused = false;
		cmd = CMD_STOP;
	}

	private void actionPlayPause() {
		doPauseAndNotify();

		// Notify clients that we paused
		final int numClients = callbacks.beginBroadcast();
		for (int i = 0; i < numClients; i++) {
			try {
				callbacks.getBroadcastItem(i).pauseCallback();
			} catch (RemoteException e) {
				Log.e(TAG, "Error notifying pause to client");
			}
		}	    
		callbacks.finishBroadcast();
	}

	private void actionPrev() {
		if (Xmp.time() > 2000) {
			Xmp.seek(0);
		} else {
			Xmp.stopModule();
			cmd = CMD_PREV;
		}
		paused = false;
	}

	private void actionNext() {
		Xmp.stopModule();
		paused = false;
		cmd = CMD_NEXT;
	}

	private void checkMediaButtons() {
		final int key = MediaButtonsReceiver.getKeyCode();

		if (key != MediaButtonsReceiver.NO_KEY) {
			switch (key) {
			case KeyEvent.KEYCODE_MEDIA_NEXT:
				Log.i(TAG, "Handle KEYCODE_MEDIA_NEXT");
				actionNext();
				break;
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
				Log.i(TAG, "Handle KEYCODE_MEDIA_PREVIOUS");
				actionPrev();
				break;
			case KeyEvent.KEYCODE_MEDIA_STOP:
				Log.i(TAG, "Handle KEYCODE_MEDIA_STOP");
				actionStop();
				break;
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				Log.i(TAG, "Handle KEYCODE_MEDIA_PLAY_PAUSE");
				actionPlayPause();
				headsetPause = false;
				break;
			case KeyEvent.KEYCODE_MEDIA_PLAY:
				Log.i(TAG, "Handle KEYCODE_MEDIA_PLAY");
				if (paused) {
					actionPlayPause();
				}
				break;
			case KeyEvent.KEYCODE_MEDIA_PAUSE:
				Log.i(TAG, "Handle KEYCODE_MEDIA_PAUSE");
				if (!paused) {
					actionPlayPause();
					headsetPause = false;
				}
				break;
			}

			MediaButtonsReceiver.setKeyCode(MediaButtonsReceiver.NO_KEY);
		}
	}

	private void checkNotificationButtons() {
		final int key = NotificationActionReceiver.getKeyCode();

		if (key != NotificationActionReceiver.NO_KEY) {
			switch (key) {
			case NotificationActionReceiver.STOP:
				Log.i(TAG, "Handle notification stop");
				actionStop();
				break;
			case NotificationActionReceiver.PAUSE:
				Log.i(TAG, "Handle notification pause");
				actionPlayPause();
				headsetPause = false;
				break;
			case NotificationActionReceiver.NEXT:
				Log.i(TAG, "Handle notification next");
				actionNext();
				break;
			}

			NotificationActionReceiver.setKeyCode(NotificationActionReceiver.NO_KEY);
		}
	}

	private void checkHeadsetState() {
		final int state = HeadsetPlugReceiver.getState();

		if (state != HeadsetPlugReceiver.NO_STATE) {
			switch (state) {
			case HeadsetPlugReceiver.HEADSET_UNPLUGGED:
				Log.i(TAG, "Handle headset unplugged");

				// If not already paused
				if (!paused && !autoPaused) {
					headsetPause = true;
					actionPlayPause();
				} else {
					Log.i(TAG, "Already paused");
				}
				break;
			case HeadsetPlugReceiver.HEADSET_PLUGGED:
				Log.i(TAG, "Handle headset plugged");

				// If paused by headset unplug
				if (headsetPause) {
					// Don't unpause if we're paused due to phone call
					if (!autoPaused) {
						actionPlayPause();
					} else {
						Log.i(TAG, "Paused by phone state, don't unpause");
					}
					headsetPause = false;
				} else {
					Log.i(TAG, "Manual pause, don't unpause");
				}
				break;
			}

			HeadsetPlugReceiver.setState(HeadsetPlugReceiver.NO_STATE);
		}
	}

	private void checkBluetoothState() {
		final int state = BluetoothConnectionReceiver.getState();

		if (state != BluetoothConnectionReceiver.NO_STATE) {
			switch (state) {
			case BluetoothConnectionReceiver.DISCONNECTED:
				Log.i(TAG, "Handle bluetooth disconnection");

				// If not already paused
				if (!paused && !autoPaused) {
					headsetPause = true;
					actionPlayPause();
				} else {
					Log.i(TAG, "Already paused");
				}
				break;
			case BluetoothConnectionReceiver.CONNECTED:
				Log.i(TAG, "Handle bluetooth connection");

				// If paused by headset unplug
				if (headsetPause) {
					// Don't unpause if we're paused due to phone call
					if (!autoPaused) {
						actionPlayPause();
					} else {
						Log.i(TAG, "Paused by phone state, don't unpause");
					}
					headsetPause = false;
				} else {
					Log.i(TAG, "Manual pause, don't unpause");
				}
				break;
			}

			BluetoothConnectionReceiver.setState(BluetoothConnectionReceiver.NO_STATE);
		}
	}

	//	private int playFrame() {
	//		// Synchronize frame play with data gathering so we don't change playing variables
	//		// in the middle of e.g. sample data reading, which results in a segfault in C code
	//
	//		synchronized (playThread) {
	//			return Xmp.playBuffer();
	//		}
	//	}

	private void notifyNewSequence() {
		final int numClients = callbacks.beginBroadcast();
		for (int j = 0; j < numClients; j++) {
			try {
				callbacks.getBroadcastItem(j).newSequenceCallback();
			} catch (RemoteException e) {
				Log.e(TAG, "Error notifying end of module to client");
			}
		}
		callbacks.finishBroadcast();
	}

	private class PlayRunnable implements Runnable {
		public void run() {
			cmd = CMD_NONE;

			do {    			
				fileName = queue.getFilename();		// Used in reconnection

				// If this file is unrecognized, and we're going backwards, go to previous
				if (fileName == null || !InfoCache.testModule(fileName)) {
					Log.w(TAG, fileName + ": unrecognized format");
					if (cmd == CMD_PREV) {
						if (queue.getIndex() < 0) {
							break;
						}
						queue.previous();
					}
					continue;
				}

				final int defpan = prefs.getInt(Preferences.DEFAULT_PAN, 50);
				Log.i(TAG, "Set default pan to " + defpan);
				Xmp.setPlayer(Xmp.PLAYER_DEFPAN, defpan);

				// Ditto if we can't load the module
				Log.w(TAG, "Load " + fileName);
				if (Xmp.loadModule(fileName) < 0) {
					Log.e(TAG, "Error loading " + fileName);
					if (cmd == CMD_PREV) {
						if (queue.getIndex() < 0) {
							break;
						}
						queue.previous();
					}
					continue;
				}

				cmd = CMD_NONE;

				notifier.tickerNotification(Xmp.getModName(), queue.getIndex());
				isLoaded = true;

				// Unmute all channels
				for (int i = 0; i < 64; i++) {
					Xmp.mute(i, 0);
				}

				final String volBoost = prefs.getString(Preferences.VOL_BOOST, "1");

				final int[] interpTypes = { Xmp.INTERP_NEAREST, Xmp.INTERP_LINEAR, Xmp.INTERP_SPLINE };
				final int temp = Integer.parseInt(prefs.getString(Preferences.INTERP_TYPE, "1"));
				int interpType;
				if (temp >= 1 && temp <= 2) {
					interpType = interpTypes[temp];
				} else {
					interpType = Xmp.INTERP_LINEAR;
				}

				if (!prefs.getBoolean(Preferences.INTERPOLATE, true)) {
					interpType = Xmp.INTERP_NEAREST;
				}

				remoteControl.setStatePlaying();
				Xmp.startPlayer(sampleRate, bufferMs);

				int numClients = callbacks.beginBroadcast();
				for (int j = 0; j < numClients; j++) {
					try {
						callbacks.getBroadcastItem(j).newModCallback();
					} catch (RemoteException e) {
						Log.e(TAG, "Error notifying new module to client");
					}
				}
				callbacks.finishBroadcast();

				Xmp.setPlayer(Xmp.PLAYER_AMP, Integer.parseInt(volBoost));
				Xmp.setPlayer(Xmp.PLAYER_MIX, prefs.getInt(Preferences.PAN_SEPARATION, 70));				
				Xmp.setPlayer(Xmp.PLAYER_INTERP, interpType);
				Xmp.setPlayer(Xmp.PLAYER_DSP, Xmp.DSP_LOWPASS);

				updateData = true;

				sequenceNumber = 0;
				boolean playNewSequence;
				Xmp.setSequence(sequenceNumber);

				Xmp.playAudio();

				do {
					while (cmd == CMD_NONE) {
						while (paused) {					
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								break;
							}

							watchdog.refresh();
							checkMediaButtons();
							checkHeadsetState();
							checkBluetoothState();
							checkNotificationButtons();
						}

						while (!Xmp.hasFreeBuffer() && !paused && cmd == CMD_NONE) {
							try {
								Thread.sleep(40);
							} catch (InterruptedException e) {	}
						}

						if (Xmp.fillBuffer(looped) < 0) {
							break;
						}

						watchdog.refresh();
						checkMediaButtons();
						checkHeadsetState();
						checkBluetoothState();
						checkNotificationButtons();
					}

					// Subsong explorer
					// Do all this if we've exited normally and explorer is active
					playNewSequence = false;
					if (allSequences && cmd == CMD_NONE) {
						sequenceNumber++;

						Log.w(TAG, "Play sequence " + sequenceNumber);
						if (Xmp.setSequence(sequenceNumber)) {
							playNewSequence = true;
							notifyNewSequence();
						}
					}
				} while (playNewSequence);

				Xmp.endPlayer();
				remoteControl.setStateStopped();

				isLoaded = false;

				// notify end of module to our clients
				numClients = callbacks.beginBroadcast();
				if (numClients > 0) {
					canRelease = false;

					for (int j = 0; j < numClients; j++) {
						try {
							Log.w(TAG, "Call end of module callback");
							callbacks.getBroadcastItem(j).endModCallback();
						} catch (RemoteException e) {
							Log.e(TAG, "Error notifying end of module to client");
						}
					}
					callbacks.finishBroadcast();

					// if we have clients, make sure we can release module
					int timeout = 0;
					try {
						while (!canRelease && timeout < 20) {
							Thread.sleep(100);
							timeout++;
						}
					} catch (InterruptedException e) {
						Log.e(TAG, "Sleep interrupted: " + e);
					}
				} else {
					callbacks.finishBroadcast();
				}

				Log.w(TAG, "Release module");
				Xmp.releaseModule();

				//audio.stop();

				// Used when current files are replaced by a new set
				if (restart) {
					Log.i(TAG, "Restart");
					queue.setIndex(startIndex - 1);
					cmd = CMD_NONE;
					restart = false;
					continue;
				} else if (cmd == CMD_PREV) {
					queue.previous();
					//returnToPrev = false;
					continue;
				}
			} while (cmd != CMD_STOP && queue.next());

			synchronized (playThread) {
				updateData = false;		// stop getChannelData update
			}
			watchdog.stop();
			notifier.cancel();
			
			audioManager.abandonAudioFocus(PlayerService.this);
			
			//end();
			Log.i(TAG, "Stop service");
			stopSelf();
		}
	}

	private void end() {    	
		Log.i(TAG, "End service");
		final int numClients = callbacks.beginBroadcast();
		for (int i = 0; i < numClients; i++) {
			try {
				callbacks.getBroadcastItem(i).endPlayCallback();
			} catch (RemoteException e) {
				Log.e(TAG, "Error notifying end of play to client");
			}
		}	    
		callbacks.finishBroadcast();

		isAlive = false;
		Xmp.stopModule();
		paused = false;

		Xmp.deinit();
		//audio.release();
	}

	private final ModInterface.Stub binder = new ModInterface.Stub() {
		public void play(final List<String> fileList, final int start, final boolean shuffle, final boolean loopList, final boolean keepFirst) {			
			queue = new QueueManager(fileList, start, shuffle, loopList, keepFirst);
			notifier.setQueue(queue);
			//notifier.clean();
			cmd = CMD_NONE;
			paused = false;

			if (isAlive) {
				Log.i(TAG, "Use existing player thread");
				restart = true;
				startIndex = keepFirst ? 0 : start;
				nextSong();
			} else {
				Log.i(TAG, "Start player thread");
				playThread = new Thread(new PlayRunnable());
				playThread.start();
			}
			isAlive = true;
		}

		public void add(final List<String> fileList) {	
			queue.add(fileList);
			updateNotification();
			//notifier.notification("Added to play queue");			
		}

		public void stop() {
			actionStop();
		}

		public void pause() {
			doPauseAndNotify();
			headsetPause = false;
		}

		public void getInfo(final int[] values) {
			Xmp.getInfo(values);
		}

		public void seek(final int seconds) {
			Xmp.seek(seconds);
		}

		public int time() {
			return Xmp.time();
		}

		public void getModVars(final int[] vars) {
			Xmp.getModVars(vars);
		}

		public String getModName() {
			return Xmp.getModName();
		}

		public String getModType() {
			return Xmp.getModType();
		}

		public void getChannelData(int[] volumes, int[] finalvols, int[] pans, int[] instruments, int[] keys, int[] periods) {
			if (updateData) {
				synchronized (playThread) {
					Xmp.getChannelData(volumes, finalvols, pans, instruments, keys, periods);
				}
			}
		}

		public void getSampleData(boolean trigger, int ins, int key, int period, int chn, int width, byte[] buffer) {
			if (updateData) {
				synchronized (playThread) {
					Xmp.getSampleData(trigger, ins, key, period, chn, width, buffer);
				}
			}
		}

		public void nextSong() {
			Xmp.stopModule();
			cmd = CMD_NEXT;
			paused = false;
		}

		public void prevSong() {
			Xmp.stopModule();
			cmd = CMD_PREV;
			paused = false;
		}

		public boolean toggleLoop() throws RemoteException {
			looped ^= true;
			return looped;
		}

		public boolean toggleAllSequences() throws RemoteException {
			allSequences ^= true;
			return allSequences;
		}

		public boolean getLoop() throws RemoteException {
			return looped;
		}

		public boolean getAllSequences() throws RemoteException {
			return allSequences;
		}

		public boolean isPaused() {
			return paused;
		}

		public boolean setSequence(final int seq) {
			final boolean ret = Xmp.setSequence(seq);
			if (ret) {
				sequenceNumber = seq;
				notifyNewSequence();
			}
			return ret;
		}

		public void allowRelease() {
			canRelease = true;
		}

		public void getSeqVars(final int[] vars) {
			Xmp.getSeqVars(vars);
		}

		// for Reconnection

		public String getFileName() {
			return fileName;
		}

		public String[] getInstruments() {
			return Xmp.getInstruments();
		}

		public void getPatternRow(final int pat, final int row, final byte[] rowNotes, final byte[] rowInstruments) {
			if (isAlive) {
				Xmp.getPatternRow(pat, row, rowNotes, rowInstruments);
			}
		}

		public int mute(final int chn, final int status) {
			return Xmp.mute(chn, status);
		}

		public boolean hasComment() {
			return Xmp.getComment() != null;
		}

		// File management

		public boolean deleteFile() {
			Log.i(TAG, "Delete file " + fileName);
			return InfoCache.delete(fileName);
		}


		// Callback

		public void registerCallback(final PlayerCallback callback) {
			if (callback != null) {
				callbacks.register(callback);
			}
		}

		public void unregisterCallback(final PlayerCallback callback) {
			if (callback != null) {
				callbacks.unregister(callback);
			}
		}
	};


	// for Telephony

	public boolean autoPause(final boolean pause) {
		Log.i(TAG, "Auto pause changed to " + pause + ", previously " + autoPaused);
		if (pause) {
			previousPaused = paused;
			autoPaused = true;
			paused = false;				// set to complement, flip on doPause()
			doPauseAndNotify();
		} else {
			if (autoPaused && !headsetPause) {
				autoPaused = false;
				paused = !previousPaused;	// set to complement, flip on doPause()
				doPauseAndNotify();
			}
		}	

		return autoPaused;
	}

	@Override
	public void onAudioFocusChange(final int focusChange) {
		switch (focusChange) {
		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
			Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
			// Pause playback
			break;
		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
			Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
			// Lower volume
			break;
		case AudioManager.AUDIOFOCUS_GAIN:
			Log.d(TAG, "AUDIOFOCUS_GAIN");
			// Resume playback/raise volume 
			break;
		case AudioManager.AUDIOFOCUS_LOSS:
			Log.w(TAG, "AUDIOFOCUS_LOSS");
			// Stop playback
			actionStop();
			remoteControl.unregisterReceiver();
			audioManager.abandonAudioFocus(this);
			break;
		default:
			break;
		}
	}
}
