package org.helllabs.android.xmp.service;

import java.util.List;

import org.helllabs.android.xmp.Xmp;
import org.helllabs.android.xmp.preferences.Preferences;
import org.helllabs.android.xmp.service.notifier.LegacyNotifier;
import org.helllabs.android.xmp.service.notifier.LollipopNotifier;
import org.helllabs.android.xmp.service.notifier.Notifier;
import org.helllabs.android.xmp.service.notifier.OreoNotifier;
import org.helllabs.android.xmp.service.utils.QueueManager;
import org.helllabs.android.xmp.service.utils.RemoteControl;
import org.helllabs.android.xmp.service.utils.Watchdog;
import org.helllabs.android.xmp.util.FileUtils;
import org.helllabs.android.xmp.util.InfoCache;
import org.helllabs.android.xmp.util.Log;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.preference.PreferenceManager;


public final class PlayerService extends Service implements OnAudioFocusChangeListener {
	private static final String TAG = "PlayerService";
	
	public static final int RESULT_OK = 0;
	public static final int RESULT_CANT_OPEN_AUDIO = 1;
	public static final int RESULT_NO_AUDIO_FOCUS = 2;
	
	private static final int CMD_NONE = 0;
	private static final int CMD_NEXT = 1;
	private static final int CMD_PREV = 2;
	private static final int CMD_STOP = 3;

	private static final int MIN_BUFFER_MS = 80;
	private static final int MAX_BUFFER_MS = 1000;
	private static final int DEFAULT_BUFFER_MS = 400;
	
	private static final int DUCK_VOLUME = 0x500;

	private AudioManager audioManager;
	private RemoteControl remoteControl;
	private boolean hasAudioFocus;
	private boolean ducking;
	private boolean audioInitialized;
	
	//private MediaSessionCompat session;
	
	private Thread playThread;
	private SharedPreferences prefs;
	private Watchdog watchdog;
	private int sampleRate;
	private int volume;
	private Notifier notifier;
	private int cmd;
	private boolean restart;
	private boolean canRelease;
	private boolean paused;
	private boolean previousPaused;		// save previous pause state
	private boolean discardBuffer;      // don't play current buffer if changing module while paused
	private boolean looped;
	private boolean allSequences;
	private int startIndex;
	private boolean updateData;
	private String fileName;			// currently playing file
	private QueueManager queue;
	private final RemoteCallbackList<PlayerCallback> callbacks = new RemoteCallbackList<>();
	private int sequenceNumber;
	
	private ReceiverHelper receiverHelper;

	public static boolean isAlive;
	public static boolean isLoaded;
	

	@Override
	public void onCreate() {
		super.onCreate();

		Log.i(TAG, "Create service");

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		remoteControl = new RemoteControl(this, audioManager);

		hasAudioFocus = requestAudioFocus();
		if (!hasAudioFocus) {
			Log.e(TAG, "Can't get audio focus");
		}

		receiverHelper = new ReceiverHelper(this);
		receiverHelper.registerReceivers();
		
		int bufferMs = prefs.getInt(Preferences.BUFFER_MS, DEFAULT_BUFFER_MS);
		if (bufferMs < MIN_BUFFER_MS) {
			bufferMs = MIN_BUFFER_MS;
		} else if (bufferMs > MAX_BUFFER_MS) {
			bufferMs = MAX_BUFFER_MS;
		}

		sampleRate = Integer.parseInt(prefs.getString(Preferences.SAMPLING_RATE, "44100"));

		if (Xmp.init(sampleRate, bufferMs)) {
			audioInitialized = true;
		} else {
			Log.e(TAG, "error initializing audio");
		}

		volume = Xmp.getVolume();

		isAlive = false;
		isLoaded = false;
		paused = false;
		allSequences = prefs.getBoolean(Preferences.ALL_SEQUENCES, false);

		//session = new MediaSessionCompat(this, getPackageName());
		//session.setActive(true);

		if (Build.VERSION.SDK_INT >= 26) {
			notifier = new OreoNotifier(this);
		} else if (Build.VERSION.SDK_INT >= 21) {
			notifier = new LollipopNotifier(this);
		} else {
			notifier = new LegacyNotifier(this);
		}

		watchdog = new Watchdog(10);
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
		receiverHelper.unregisterReceivers();

		watchdog.stop();
		notifier.cancel();
		
		//session.setActive(false);
		
		if (audioInitialized) {
			end(hasAudioFocus ? RESULT_OK : RESULT_NO_AUDIO_FOCUS);
		} else {
			end(RESULT_CANT_OPEN_AUDIO);
		}
		super.onDestroy();
	}

	@Override
	public IBinder onBind(final Intent intent) {
		return binder;
	}
	
	private boolean requestAudioFocus() {
		return audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
				AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
	}

	private void updateNotification() {
		if (queue != null) {	// It seems that queue can be null if we're called from PhoneStateListener
			String name = Xmp.getModName();
			if (name.isEmpty()) {
				name = FileUtils.basename(queue.getFilename());
			}
			notifier.notify(name, Xmp.getModType(), queue.getIndex(), paused ? Notifier.TYPE_PAUSE : 0);
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

	public void actionStop() {
		Xmp.stopModule();
		if (paused) {
			doPauseAndNotify();
		}
		cmd = CMD_STOP;
	}

	public void actionPlayPause() {
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

	public void actionPrev() {
		if (Xmp.time() > 2000) {
			Xmp.seek(0);
		} else {
			Xmp.stopModule();
			cmd = CMD_PREV;
		}
		if (paused) {
			doPauseAndNotify();
			discardBuffer = true;
		}
	}

	public void actionNext() {
		Xmp.stopModule();
		if (paused) {
			doPauseAndNotify();
			discardBuffer = true;
		}
		cmd = CMD_NEXT;
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

			final int[] vars = new int[8];
			remoteControl.setStatePlaying();

			int lastRecognized = 0;
			do {    			
				fileName = queue.getFilename();		// Used in reconnection

				// If this file is unrecognized, and we're going backwards, go to previous
				// If we're at the start of the list, go to the last recognized file
				if (fileName == null || !InfoCache.testModule(fileName)) {
					Log.w(TAG, fileName + ": unrecognized format");
					if (cmd == CMD_PREV) {
						if (queue.getIndex() <= 0) {
							queue.setIndex(lastRecognized - 1);		// -1 because we have queue.next() in the while condition
							continue;
						}
						queue.previous();
					}
					continue;
				}

				// Set default pan before we load the module
				final int defpan = prefs.getInt(Preferences.DEFAULT_PAN, 50);
				Log.i(TAG, "Set default pan to " + defpan);
				Xmp.setPlayer(Xmp.PLAYER_DEFPAN, defpan);

				// Ditto if we can't load the module
				Log.w(TAG, "Load " + fileName);
				if (Xmp.loadModule(fileName) < 0) {
					Log.e(TAG, "Error loading " + fileName);
					if (cmd == CMD_PREV) {
						if (queue.getIndex() <= 0) {
							queue.setIndex(lastRecognized - 1);	
							continue;
						}
						queue.previous();
					}
					continue;
				}

				lastRecognized = queue.getIndex();

				cmd = CMD_NONE;

				String name = Xmp.getModName();
				if (name.isEmpty()) {
					name = FileUtils.basename(fileName);
				}
				notifier.notify(name, Xmp.getModType(), queue.getIndex(), Notifier.TYPE_TICKER);
				isLoaded = true;

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

				Xmp.startPlayer(sampleRate);
				
				synchronized (audioManager) {
					if (ducking) {
						Xmp.setPlayer(Xmp.PLAYER_VOLUME, DUCK_VOLUME);
					}
				}

				// Unmute all channels
				for (int i = 0; i < 64; i++) {
					Xmp.mute(i, 0);
				}

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
				Xmp.setPlayer(Xmp.PLAYER_MIX, prefs.getInt(Preferences.STEREO_MIX, 100));
				Xmp.setPlayer(Xmp.PLAYER_INTERP, interpType);
				Xmp.setPlayer(Xmp.PLAYER_DSP, Xmp.DSP_LOWPASS);

				int flags = Xmp.getPlayer(Xmp.PLAYER_CFLAGS);
				if (prefs.getBoolean(Preferences.AMIGA_MIXER, false)) {
					flags |= Xmp.FLAGS_A500;
				} else {
					flags &= ~Xmp.FLAGS_A500;
				}
				Xmp.setPlayer(Xmp.PLAYER_CFLAGS, flags);

				updateData = true;

				sequenceNumber = 0;
				boolean playNewSequence;
				Xmp.setSequence(sequenceNumber);

				Xmp.playAudio();
				
				Log.i(TAG, "Enter play loop");

				do {
					Xmp.getModVars(vars);
					remoteControl.setMetadata(Xmp.getModName(), Xmp.getModType(), vars[0]);
					
					while (cmd == CMD_NONE) {
						discardBuffer = false;

						// Wait if paused
						while (paused) {
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								break;
							}

							watchdog.refresh();
							receiverHelper.checkReceivers();
						}

						if (discardBuffer) {
							Log.d(TAG, "discard buffer");
							Xmp.dropAudio();
							break;
						}

						// Wait if no buffers available
						while (!Xmp.hasFreeBuffer() && !paused && cmd == CMD_NONE) {
							try {
								Thread.sleep(40);
							} catch (InterruptedException e) {	}
						}

						// Fill a new buffer
						if (Xmp.fillBuffer(looped) < 0) {
							break;
						}

						watchdog.refresh();
						receiverHelper.checkReceivers();
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
				} else if (cmd == CMD_PREV) {
					queue.previous();
					//returnToPrev = false;
				}
			} while (cmd != CMD_STOP && queue.next());

			synchronized (playThread) {
				updateData = false;		// stop getChannelData update
			}
			watchdog.stop();
			notifier.cancel();
			
			remoteControl.setStateStopped();
			audioManager.abandonAudioFocus(PlayerService.this);
			
			//end();
			Log.i(TAG, "Stop service");
			stopSelf();
		}
	}

	private void end(final int result) {    	
		Log.i(TAG, "End service");
		final int numClients = callbacks.beginBroadcast();
		for (int i = 0; i < numClients; i++) {
			try {
				callbacks.getBroadcastItem(i).endPlayCallback(result);
			} catch (RemoteException e) {
				Log.e(TAG, "Error notifying end of play to client");
			}
		}	    
		callbacks.finishBroadcast();

		isAlive = false;
		Xmp.stopModule();
		if (paused) {
			doPauseAndNotify();
		}

		Xmp.deinit();
		//audio.release();
	}

	private final ModInterface.Stub binder = new ModInterface.Stub() {
		public void play(final List<String> fileList, final int start, final boolean shuffle, final boolean loopList, final boolean keepFirst) {
			
			if (!audioInitialized || !hasAudioFocus) {
				stopSelf();
				return;
			}
			
			queue = new QueueManager(fileList, start, shuffle, loopList, keepFirst);
			notifier.setQueue(queue);
			//notifier.clean();
			cmd = CMD_NONE;

			if (paused) {
				doPauseAndNotify();
			}

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
			receiverHelper.setHeadsetPaused(false);
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

		public void getChannelData(final int[] volumes, final int[] finalvols, final int[] pans, final int[] instruments, final int[] keys, final int[] periods) {
			if (updateData) {
				synchronized (playThread) {
					Xmp.getChannelData(volumes, finalvols, pans, instruments, keys, periods);
				}
			}
		}

		public void getSampleData(final boolean trigger, final int ins, final int key, final int period, final int chn, final int width, final byte[] buffer) {
			if (updateData) {
				synchronized (playThread) {
					Xmp.getSampleData(trigger, ins, key, period, chn, width, buffer);
				}
			}
		}

		public void nextSong() {
			Xmp.stopModule();
			cmd = CMD_NEXT;
			if (paused) {
				doPauseAndNotify();
			}
			discardBuffer = true;
		}

		public void prevSong() {
			Xmp.stopModule();
			cmd = CMD_PREV;
			if (paused) {
				doPauseAndNotify();
			}
			discardBuffer = true;
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


	// for audio focus loss

	private boolean autoPause(final boolean pause) {
		Log.i(TAG, "Auto pause changed to " + pause + ", previously " + receiverHelper.isAutoPaused());
		if (pause) {
			previousPaused = paused;
			receiverHelper.setAutoPaused(true);
			paused = false;				// set to complement, flip on doPause()
			doPauseAndNotify();
		} else {
			if (receiverHelper.isAutoPaused() && !receiverHelper.isHeadsetPaused()) {
				receiverHelper.setAutoPaused(false);
				paused = !previousPaused;	// set to complement, flip on doPause()
				doPauseAndNotify();
			}
		}	

		return receiverHelper.isAutoPaused();
	}

	@Override
	public void onAudioFocusChange(final int focusChange) {
		switch (focusChange) {
		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
			Log.w(TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
			// Pause playback
			autoPause(true);
			break;
		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
			Log.w(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
			// Lower volume
			synchronized (audioManager) {
				volume = Xmp.getVolume();
				Xmp.setVolume(DUCK_VOLUME);
				ducking = true;
			}
			break;
		case AudioManager.AUDIOFOCUS_GAIN:
			Log.w(TAG, "AUDIOFOCUS_GAIN");
			// Resume playback/raise volume
			autoPause(false);
			synchronized (audioManager) {
				Xmp.setVolume(volume);
				ducking = false;
			}
			break;
		case AudioManager.AUDIOFOCUS_LOSS:
			Log.w(TAG, "AUDIOFOCUS_LOSS");
			// Stop playback
			actionStop();
			break;
		default:
			break;
		}
	}
	
	public boolean isPaused() {
		return paused;
	}
}
