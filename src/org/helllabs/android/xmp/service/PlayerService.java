package org.helllabs.android.xmp.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.helllabs.android.xmp.InfoCache;
import org.helllabs.android.xmp.Log;
import org.helllabs.android.xmp.ModInterface;
import org.helllabs.android.xmp.PlayerCallback;
import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.Preferences;
import org.helllabs.android.xmp.Xmp;
import org.helllabs.android.xmp.player.PlayerActivity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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


public class PlayerService extends Service {
	private static final String TAG = PlayerService.class.getSimpleName();
	private final Xmp xmp = new Xmp();
	private AudioTrack audio;
	private Thread playThread;
	private SharedPreferences prefs;
	private Watchdog watchdog;
	private int bufferSize;
	private int sampleRate, sampleFormat;
	private Notifier notifier;
	private boolean stopPlaying;
	private boolean restartList;
	private boolean returnToPrev;
	private boolean paused;
	private boolean looped;
	private int startIndex;
	private Boolean updateData;
	private String fileName;			// currently playing file
	private QueueManager queue;
	private final RemoteCallbackList<PlayerCallback> callbacks =
		new RemoteCallbackList<PlayerCallback>();
	private boolean autoPaused;			// paused on phone call
    
    // for media buttons
    private AudioManager audioManager;
    private ComponentName remoteControlResponder;
    private static Method registerMediaButtonEventReceiver;
    private static Method unregisterMediaButtonEventReceiver;
    
	public static boolean isAlive;
	public static boolean isLoaded;


	static {
		initializeRemoteControlRegistrationMethods();
	}
    
    @Override
	public void onCreate() {
    	super.onCreate();
    	
    	Log.i(TAG, "Create service");
    	
   		prefs = PreferenceManager.getDefaultSharedPreferences(this);
   		
   		final int bufferMs = prefs.getInt(Preferences.BUFFER_MS, 500);
   		sampleRate = Integer.parseInt(prefs.getString(Preferences.SAMPLING_RATE, "44100"));
   		sampleFormat = 0;
   		
   		final boolean stereo = prefs.getBoolean(Preferences.STEREO, true);
   		if (!stereo) {
   			sampleFormat |= Xmp.XMP_FORMAT_MONO;
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

		xmp.init();

		isAlive = false;
		isLoaded = false;
		paused = false;
		
		notifier = new Notifier();
		
		final XmpPhoneStateListener listener = new XmpPhoneStateListener(this);
		final TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE); // NOPMD
		tm.listen(listener, XmpPhoneStateListener.LISTEN_CALL_STATE);
		
		audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		remoteControlResponder = new ComponentName(getPackageName(), RemoteControlReceiver.class.getName());
		registerRemoteControl();
		
		watchdog = new Watchdog(10);
 		watchdog.setOnTimeoutListener(new Watchdog.onTimeoutListener() {
			public void onTimeout() {
				Log.e(TAG, "Stopped by watchdog");
		    	stopSelf();
			}
		});
 		watchdog.start();
    }

    @Override
	public void onDestroy() {
    	unregisterRemoteControl();
    	watchdog.stop();
    	notifier.cancel();
    	end();
    	super.onDestroy();
    }

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
	private void checkMediaButtons() {
		final int key = RemoteControlReceiver.keyCode;
		
		if (key > 0) {
			switch (key) {
			case KeyEvent.KEYCODE_MEDIA_NEXT:
				Log.i(TAG, "Handle KEYCODE_MEDIA_NEXT");
				xmp.stopModule();
				stopPlaying = false;
				paused = false;
				break;
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
				Log.i(TAG, "Handle KEYCODE_MEDIA_PREVIOUS");
				if (xmp.time() > 2000) {
					xmp.seek(0);
				} else {
					xmp.stopModule();
					returnToPrev = true;
					stopPlaying = false;
				}
				paused = false;
				break;
			case KeyEvent.KEYCODE_MEDIA_STOP:
				Log.i(TAG, "Handle KEYCODE_MEDIA_STOP");
		    	xmp.stopModule();
		    	paused = false;
		    	stopPlaying = true;
		    	break;
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				Log.i(TAG, "Handle KEYCODE_MEDIA_PLAY_PAUSE");
				paused = !paused;
				break;
			}
			
			RemoteControlReceiver.keyCode = -1;
		}
	}
	
	private class PlayRunnable implements Runnable {
    	public void run() {
    		final short buffer[] = new short[bufferSize]; // NOPMD
    		returnToPrev = false;
    		
    		do {    			
    			fileName = queue.getFilename();		// Used in reconnection
    			
    			if (!InfoCache.testModule(fileName)) {
    				Log.w(TAG, fileName + ": unrecognized format");
    				if (returnToPrev) {
    					queue.previous();
    				}
    				continue;
    			}
    			
	    		Log.i(TAG, "Load " + fileName);
	       		if (xmp.loadModule(fileName) < 0) {
	       			Log.e(TAG, "Error loading " + fileName);
	       			if (returnToPrev) {
	       				queue.previous();
	       			}
	       			continue;
	       		}
	       		
	       		returnToPrev = false;

	       		notifier.notification(xmp.getModName(), queue.getIndex());
	       		isLoaded = true;
	       		
	       		// Unmute all channels
	       		for (int i = 0; i < 64; i++) {
	       			xmp.mute(i, 0);
	       		}
		       		    	
	        	int numClients = callbacks.beginBroadcast();
	        	for (int j = 0; j < numClients; j++) {
	        		try {
	    				callbacks.getBroadcastItem(j).newModCallback(fileName, xmp.getInstruments());
	    			} catch (RemoteException e) {
	    				Log.e(TAG, "Error notifying new module to client");
	    			}
	        	}
	        	callbacks.finishBroadcast();

	        	final String volBoost = prefs.getString(Preferences.VOL_BOOST, "1");
	        	
	       		final int[] interpTypes = { Xmp.XMP_INTERP_NEAREST, Xmp.XMP_INTERP_LINEAR, Xmp.XMP_INTERP_SPLINE };
	       		final int temp = Integer.parseInt(prefs.getString(Preferences.INTERP_TYPE, "1"));
	       		int interpType;
	       		if (temp >= 1 && temp <= 2) {
	       			interpType = interpTypes[temp];
	       		} else {
	       			interpType = Xmp.XMP_INTERP_LINEAR;
	       		}
	       		
	        	int dsp = 0;
	        	if (prefs.getBoolean(Preferences.FILTER, true)) {
	        		dsp |= Xmp.XMP_DSP_LOWPASS;
	        	}
	        	
	        	if (!prefs.getBoolean(Preferences.INTERPOLATE, true)) {
	        		interpType = Xmp.XMP_INTERP_NEAREST;
	        	}

	       		audio.play();
	       		xmp.startPlayer(0, sampleRate, sampleFormat);
	        	xmp.setPlayer(Xmp.XMP_PLAYER_AMP, Integer.parseInt(volBoost));
	        	xmp.setPlayer(Xmp.XMP_PLAYER_MIX, prefs.getInt(Preferences.PAN_SEPARATION, 70));
	        	xmp.setPlayer(Xmp.XMP_PLAYER_INTERP, interpType);
	        	xmp.setPlayer(Xmp.XMP_PLAYER_DSP, dsp);
	        		        	
	       		updateData = true;
	    		
	    		int count;
	    		int loopCount = 0;
	    		
	       		while (xmp.playFrame() == 0) {
	       			count = xmp.getLoopCount();
	       			if (!looped && count != loopCount) {
	       				break;
	       			}
	       			loopCount = count;
	       			
	       			final int size = xmp.getBuffer(buffer);
	       			audio.write(buffer, 0, size);
	       			
	       			while (paused) {
	       				audio.pause();
	       				watchdog.refresh();
	       				try {
							Thread.sleep(500);
							checkMediaButtons();
						} catch (InterruptedException e) {
							break;
						}
	       			}
	       			audio.play();
	       			
	       			watchdog.refresh();
	       			checkMediaButtons();
	       		}

	       		xmp.endPlayer();
	       		
	       		isLoaded = false;
	       		
	        	numClients = callbacks.beginBroadcast();
	        	for (int j = 0; j < numClients; j++) {
	        		try {
	    				callbacks.getBroadcastItem(j).endModCallback();
	    			} catch (RemoteException e) {
	    				Log.e(TAG, "Error notifying end of module to client");
	    			}
	        	}
	        	callbacks.finishBroadcast();
	        	
	       		xmp.releaseModule();
       		
	       		audio.stop();
	       		
	       		if (restartList) {
	       			//queue.restart();
	       			queue.setIndex(startIndex - 1);
	       			restartList = false;
	       			continue;
	       		}
	       		
	       		if (returnToPrev) {
	       			queue.previous();
	       			//returnToPrev = false;
	       			continue;
	       		}
    		} while (!stopPlaying && queue.next());

    		synchronized (xmp) {
    			updateData = false;		// stop getChannelData update
    		}
    		watchdog.stop();
    		notifier.cancel();
        	end();
        	stopSelf();
    	}
    }

	protected void end() {    	
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
    	xmp.stopModule();
    	paused = false;

    	if (playThread != null && playThread.isAlive()) {
    		try {
    			playThread.join();
    		} catch (InterruptedException e) { }
    	}
    	
    	xmp.deinit();
    	audio.release();
    }
	
	private class Notifier {
	    private final NotificationManager nm;	// NOPMD
	    private final PendingIntent contentIntent;
	    private static final int NOTIFY_ID = R.layout.player;
		private String title;
		private int index;

		public Notifier() {
			nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
	    	contentIntent = PendingIntent.getActivity(PlayerService.this, 0,
					new Intent(PlayerService.this, PlayerActivity.class), 0);
		}
		
		private String message() {
			return queue.size() > 1 ?
				String.format("%s (%d/%d)", title, index, queue.size()) :
				title;
		}
		
		public void cancel() {
			nm.cancel(NOTIFY_ID);
		}
		
		public void notification() {
			notification(null, null);
		}
		
		public void notification(final String title, final int index) {
			this.title = title;
			this.index = index + 1;			
			notification(message(), message());
		}
		
		public void notification(final String ticker) {
			notification(ticker, message());
		}
		
		public void notification(final String ticker, final String latest) {
	        Notification notification = new Notification(
	        		R.drawable.notification, ticker, System.currentTimeMillis());
	        notification.setLatestEventInfo(PlayerService.this, getText(R.string.app_name),
	        		latest, contentIntent);
	        notification.flags |= Notification.FLAG_ONGOING_EVENT;	        
	        nm.notify(NOTIFY_ID, notification);				
		}
	}

	private final ModInterface.Stub binder = new ModInterface.Stub() {
		public void play(String[] files, int start, boolean shuffle, boolean loopList) {			
			notifier.notification();
			queue = new QueueManager(files, start, shuffle, loopList);
			returnToPrev = false;
			stopPlaying = false;
			paused = false;

			if (isAlive) {
				Log.i(TAG, "Use existing player thread");
				restartList = true;
				startIndex = start;
				nextSong();
			} else {
				Log.i(TAG, "Start player thread");
				restartList = false;
		   		playThread = new Thread(new PlayRunnable());
		   		playThread.start();
			}
			isAlive = true;
		}
		
		public void add(String[] files) {	
			queue.add(files);
			notifier.notification("Added to play queue");			
		}
	    
	    public void stop() {
	    	xmp.stopModule();
	    	paused = false;
	    	stopPlaying = true;
	    }
	    
	    public void pause() {
	    	paused = !paused;
	    }
	    
	    public void getInfo(int[] values) {
	    	xmp.getInfo(values);
	    }
	
		public void seek(int seconds) {
			xmp.seek(seconds);
		}
		
		public int time() {
			return xmp.time();
		}
		
		public void getModVars(int[] vars) {
			xmp.getModVars(vars);
		}
		
		public String getModName() {
			return xmp.getModName();
		}
		
		public String getModType() {
			return xmp.getModType();
		}
		
		public void getChannelData(int[] volumes, int[] finalvols, int[] pans, int[] instruments, int[] keys, int[] periods) {
			synchronized (xmp) {
				if (updateData) {
					xmp.getChannelData(volumes, finalvols, pans, instruments, keys, periods);
				}
			}
		}
		
		public void getSampleData(boolean trigger, int ins, int key, int period, int chn, int width, byte[] buffer) {
			synchronized (xmp) {
				if (updateData) {
					xmp.getSampleData(trigger, ins, key, period, chn, width, buffer);
				}
			}
		}
		
		public void nextSong() {
			xmp.stopModule();
			stopPlaying = false;
			paused = false;
		}
		
		public void prevSong() {
			xmp.stopModule();
			returnToPrev = true;
			stopPlaying = false;
			paused = false;
		}

		public boolean toggleLoop() throws RemoteException {
			looped = !looped;
			return looped;
		}
		
		public boolean isPaused() {
			return paused;
		}
		
		// for Reconnection
		
		public String getFileName() {
			return fileName;
		}
		
		public String[] getInstruments() {
			return xmp.getInstruments();
		}
		
		public void getPatternRow(final int pat, final int row, final byte[] rowNotes, final byte[] rowInstruments) {
			if (isAlive) {
				xmp.getPatternRow(pat, row, rowNotes, rowInstruments);
			}
		}
		
		public int mute(final int chn, final int status) {
			return xmp.mute(chn, status);
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
			paused = autoPaused = true;
		} else {
			if (autoPaused) {
				paused = autoPaused = false;
			}
		}	
		
		return autoPaused;
	}
	
	// for media buttons
	// see http://android-developers.blogspot.com/2010/06/allowing-applications-to-play-nicer.html
	
	private static void initializeRemoteControlRegistrationMethods() {
		try {
			if (registerMediaButtonEventReceiver == null) {
				registerMediaButtonEventReceiver = AudioManager.class
						.getMethod("registerMediaButtonEventReceiver",
								new Class[] { ComponentName.class });
			}
			if (unregisterMediaButtonEventReceiver == null) {
				unregisterMediaButtonEventReceiver = AudioManager.class
						.getMethod("unregisterMediaButtonEventReceiver",
								new Class[] { ComponentName.class });
			}
			/* success, this device will take advantage of better remote */
			/* control event handling */
		} catch (NoSuchMethodException nsme) {
			/* failure, still using the legacy behavior, but this app */
			/* is future-proof! */
		}
	}

	private void registerRemoteControl() {
		try {
			if (registerMediaButtonEventReceiver == null) {
				return;
			}
			registerMediaButtonEventReceiver.invoke(audioManager, remoteControlResponder);
		} catch (InvocationTargetException ite) {
			/* unpack original exception when possible */
			final Throwable cause = ite.getCause();
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			} else if (cause instanceof Error) {
				throw (Error) cause;
			} else {
				/* unexpected checked exception; wrap and re-throw */
				throw new RuntimeException(ite);
			}
		} catch (IllegalAccessException ie) {
			Log.e(TAG, "Unexpected " + ie);
		}
	}

	private void unregisterRemoteControl() {
		try {
			if (unregisterMediaButtonEventReceiver == null) {
				return;
			}
			unregisterMediaButtonEventReceiver.invoke(audioManager,	remoteControlResponder);
		} catch (InvocationTargetException ite) {
			/* unpack original exception when possible */
			final Throwable cause = ite.getCause();
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			} else if (cause instanceof Error) {
				throw (Error) cause;
			} else {
				/* unexpected checked exception; wrap and re-throw */
				throw new RuntimeException(ite);
			}
		} catch (IllegalAccessException ie) {
			Log.e(TAG, "Unexpected " + ie);
		}
	}
}
