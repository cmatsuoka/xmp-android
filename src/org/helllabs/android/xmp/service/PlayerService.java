package org.helllabs.android.xmp.service;

import org.helllabs.android.xmp.Xmp;
import org.helllabs.android.xmp.preferences.Preferences;
import org.helllabs.android.xmp.service.receiver.HeadsetPlugReceiver;
import org.helllabs.android.xmp.service.receiver.NotificationActionReceiver;
import org.helllabs.android.xmp.service.receiver.RemoteControlReceiver;
import org.helllabs.android.xmp.util.InfoCache;
import org.helllabs.android.xmp.util.Log;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;


public final class PlayerService extends Service {
	private static final String TAG = "PlayerService";
	private static final int CMD_NONE = 0;
	private static final int CMD_NEXT = 1;
	private static final int CMD_PREV = 2;
	private static final int CMD_STOP = 3;
	private AudioTrack audio;
	private Thread playThread;
	private SharedPreferences prefs;
	private Watchdog watchdog;
	private int bufferSize;
	private int sampleRate, sampleFormat;
	private Notifier notifier;
	private int cmd;
	private boolean restart;
	private boolean canRelease;
	private boolean paused;
	private boolean looped;
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

	// remote control
	private MediaButtons mediaButtons;

	public static boolean isAlive;
	public static boolean isLoaded;


	@Override
	public void onCreate() {
		super.onCreate();

		Log.i(TAG, "Create service");

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		if (prefs.getBoolean(Preferences.HEADSET_PAUSE, true)) {
			// For listening to headset changes, the broadcast receiver cannot be
			// declared in the manifest, it must be dynamically registered. 
			headsetPlugReceiver = new HeadsetPlugReceiver();
			registerReceiver(headsetPlugReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
		}

		final int bufferMs = prefs.getInt(Preferences.BUFFER_MS, 500);
		sampleRate = Integer.parseInt(prefs.getString(Preferences.SAMPLING_RATE, "44100"));
		sampleFormat = 0;

		final boolean stereo = prefs.getBoolean(Preferences.STEREO, true);
		if (!stereo) {
			sampleFormat |= Xmp.FORMAT_MONO;
		}

		bufferSize = (sampleRate * (stereo ? 2 : 1) * 2 * bufferMs / 1000) & ~0x3;

		final int channelConfig = stereo ?
				AudioFormat.CHANNEL_OUT_STEREO :
					AudioFormat.CHANNEL_OUT_MONO;

		final int minSize = AudioTrack.getMinBufferSize(
				sampleRate,
				channelConfig,
				AudioFormat.ENCODING_PCM_16BIT);

		if (bufferSize < minSize) {
			bufferSize = minSize;
		}

		audio = new AudioTrack(
				AudioManager.STREAM_MUSIC, sampleRate,
				channelConfig,
				AudioFormat.ENCODING_PCM_16BIT,
				bufferSize,
				AudioTrack.MODE_STREAM);

		Xmp.init();

		isAlive = false;
		isLoaded = false;
		paused = false;

		notifier = new Notifier(this);

		final XmpPhoneStateListener listener = new XmpPhoneStateListener(this);
		final TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		tm.listen(listener, XmpPhoneStateListener.LISTEN_CALL_STATE);

		mediaButtons = new MediaButtons(this);
		mediaButtons.register();

		watchdog = new Watchdog(5);
		watchdog.setOnTimeoutListener(new Watchdog.OnTimeoutListener() {
			public void onTimeout() {
				Log.e(TAG, "Stopped by watchdog");
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
		mediaButtons.unregister();
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
		if (paused) {
			notifier.pauseNotification(Xmp.getModName(), queue.getIndex());
		} else {
			notifier.unpauseNotification(Xmp.getModName(), queue.getIndex());
		}		
	}
	
	private void doPauseAndNotify() {
		paused ^= true;
		updateNotification();
	}

	private void actionStop() {
		Xmp.stopModule();
		paused = false;
		cmd = CMD_STOP;
	}

	private void actionPause() {
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
		final int key = RemoteControlReceiver.getKeyCode();

		if (key != RemoteControlReceiver.NO_KEY) {
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
				actionPause();
				headsetPause = false;
				break;
			}

			RemoteControlReceiver.setKeyCode(RemoteControlReceiver.NO_KEY);
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
				actionPause();
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
					actionPause();
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
						actionPause();
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

	private int playFrame() {
		// Synchronize frame play with data gathering so we don't change playing variables
		// in the middle of e.g. sample data reading, which results in a segfault in C code

		synchronized (playThread) {
			return Xmp.playFrame();
		}
	}

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
			final short buffer[] = new short[bufferSize];
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

				int numClients = callbacks.beginBroadcast();
				for (int j = 0; j < numClients; j++) {
					try {
						callbacks.getBroadcastItem(j).newModCallback(fileName, Xmp.getInstruments());
					} catch (RemoteException e) {
						Log.e(TAG, "Error notifying new module to client");
					}
				}
				callbacks.finishBroadcast();

				final String volBoost = prefs.getString(Preferences.VOL_BOOST, "1");

				final int[] interpTypes = { Xmp.INTERP_NEAREST, Xmp.INTERP_LINEAR, Xmp.INTERP_SPLINE };
				final int temp = Integer.parseInt(prefs.getString(Preferences.INTERP_TYPE, "1"));
				int interpType;
				if (temp >= 1 && temp <= 2) {
					interpType = interpTypes[temp];
				} else {
					interpType = Xmp.INTERP_LINEAR;
				}

				int dsp = 0;
				if (prefs.getBoolean(Preferences.FILTER, true)) {
					dsp |= Xmp.DSP_LOWPASS;
				}

				if (!prefs.getBoolean(Preferences.INTERPOLATE, true)) {
					interpType = Xmp.INTERP_NEAREST;
				}

				audio.play();
				Xmp.startPlayer(0, sampleRate, sampleFormat);
				Xmp.setPlayer(Xmp.PLAYER_AMP, Integer.parseInt(volBoost));
				Xmp.setPlayer(Xmp.PLAYER_MIX, prefs.getInt(Preferences.PAN_SEPARATION, 70));
				Xmp.setPlayer(Xmp.PLAYER_INTERP, interpType);
				Xmp.setPlayer(Xmp.PLAYER_DSP, dsp);

				updateData = true;

				int count;
				int loopCount = 0;

				sequenceNumber = 0;
				boolean playNewSequence;
				final boolean allSequences = prefs.getBoolean(Preferences.ALL_SEQUENCES, false);
				Xmp.setSequence(sequenceNumber);

				do {
					while (playFrame() == 0) {
						count = Xmp.getLoopCount();
						if (!looped && count != loopCount) {
							break;
						}
						loopCount = count;

						final int size = Xmp.getBuffer(buffer);
						audio.write(buffer, 0, size);

						while (paused) {
							audio.flush();
							audio.pause();
							watchdog.refresh();
							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
								break;
							}

							checkMediaButtons();
							checkHeadsetState();
							checkNotificationButtons();
						}
						audio.play();

						watchdog.refresh();
						checkMediaButtons();
						checkHeadsetState();
						checkNotificationButtons();
					}

					// Subsong explorer
					// Do all this if we've exited normally and explorer is active
					playNewSequence = false;
					if (allSequences && cmd == CMD_NONE) {
						sequenceNumber++;
						loopCount = Xmp.getLoopCount();

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

				audio.stop();

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
		audio.release();
	}

	private final ModInterface.Stub binder = new ModInterface.Stub() {
		public void play(final String[] files, final int start, final boolean shuffle, final boolean loopList, final boolean keepFirst) {			
			queue = new QueueManager(files, start, shuffle, loopList, keepFirst);
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

		public void add(final String[] files) {	
			queue.add(files);
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

		public boolean isPaused() {
			return paused;
		}

		public boolean setSequence(int seq) {
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
}
